package com.retro.app.core

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.os.Process
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.FutureTask
import kotlin.math.max

/**
 * Chạy lõi giả lập trên một luồng riêng. Nhịp khung hình được điều khiển bởi âm thanh:
 * AudioTrack.write chặn khi bộ đệm đầy, nên game luôn chạy đúng tốc độ thật.
 */
object EmulatorEngine {
    private val tasks = ConcurrentLinkedQueue<FutureTask<*>>()

    @Volatile private var alive = false
    private var thread: Thread? = null

    @Volatile var paused = true
        private set
    @Volatile private var loaded = false
    private var coreReady = false

    private var track: AudioTrack? = null
    private var av = DoubleArray(5)
    private var currentId: String? = null
    private var romPath: String? = null
    /** Đang có cheat nạp trong lõi (cần nạp lại game sạch để gỡ hẳn bản vá ROM của cheat). */
    private var cheatsActive = false
    private var lastSramHash = 0
    private var lastSramSaveNs = 0L

    /** Gọi (trên luồng giả lập) khi có dữ liệu SRAM mới cần ghi ra file. */
    @Volatile var sramSink: ((String, ByteArray) -> Unit)? = null
    /** Tần số phát ra (Hz); khớp retro::kOutputRate bên C++. */
    private const val OUTPUT_RATE = 48000

    @Volatile var onFps: ((Float) -> Unit)? = null
    @Volatile var onCoreMs: ((Float) -> Unit)? = null

    @Volatile var soundOn = true
        set(v) {
            field = v
            track?.setVolume(if (v) 1f else 0f)
        }

    @Volatile var speed = 1f
        set(v) {
            field = v
            applySpeedToTrack()
        }

    @Volatile private var touchMask = 0
    @Volatile private var hwMask = 0

    // ---------- Đầu vào ----------
    fun setTouchMask(mask: Int) {
        touchMask = mask
        pushInput()
    }

    fun setHardwareMask(mask: Int) {
        hwMask = mask
        pushInput()
    }

    private fun pushInput() {
        if (coreReady) NativeCore.nativeSetInput(touchMask or hwMask)
    }

    // ---------- Chạy việc trên luồng giả lập ----------
    private fun ensureThread() {
        if (thread?.isAlive == true) return
        alive = true
        thread = Thread({ loop() }, "retro-emu").also { it.start() }
    }

    fun <T> call(block: () -> T): T {
        ensureThread()
        if (Thread.currentThread() === thread) return block()
        val task = FutureTask(Callable { block() })
        tasks.add(task)
        return task.get()
    }

    private fun drainTasks() {
        while (true) {
            val t = tasks.poll() ?: break
            t.run()
        }
    }

    // ---------- Nạp / gỡ game ----------
    /** Trả về null nếu thành công, ngược lại là thông báo lỗi. */
    fun load(ctx: Context, rom: File, romId: String, sram: ByteArray?): String? = call {
        try {
            if (loaded) unloadInternal()
            if (!coreReady) {
                val corePath = ctx.applicationInfo.nativeLibraryDir + "/libmgba_libretro.so"
                val sys = File(ctx.filesDir, "system").apply { mkdirs() }.absolutePath
                val save = File(ctx.filesDir, "core-saves").apply { mkdirs() }.absolutePath
                val err = NativeCore.nativeInit(corePath, sys, save)
                if (err != null) return@call err
                coreReady = true
            }
            val err = NativeCore.nativeLoadGame(rom.absolutePath)
            if (err != null) return@call err
            if (sram != null && sram.isNotEmpty()) NativeCore.nativeSetSram(sram)
            av = NativeCore.nativeAvInfo()
            lastSramHash = NativeCore.nativeGetSram()?.contentHashCode() ?: 0
            lastSramSaveNs = System.nanoTime()
            currentId = romId
            romPath = rom.absolutePath
            cheatsActive = false
            pushInput()
            startAudio(OUTPUT_RATE)
            loaded = true
            paused = false
            null
        } catch (e: Throwable) {
            "Lỗi nạp game: ${e.message}"
        }
    }

    fun unload() = call { if (loaded) unloadInternal() }

    private fun unloadInternal() {
        saveSramNow()
        loaded = false
        paused = true
        stopAudio()
        NativeCore.nativeUnloadGame()
        currentId = null
    }

    /** Đưa việc vào hàng đợi luồng giả lập, không chờ kết quả (an toàn gọi từ luồng giao diện). */
    private fun post(block: () -> Unit) {
        ensureThread()
        tasks.add(FutureTask(Callable { block() }))
    }

    fun pause() {
        paused = true
        post {
            if (loaded) saveSramNow()
            track?.let { runCatching { it.pause() } }
        }
    }

    fun resume() {
        post {
            if (loaded) {
                track?.let { runCatching { it.play() } }
                paused = false
            }
        }
    }

    /** Đặt tuỳ chọn của lõi (vd. bảng màu Game Boy). Lõi đọc ngay ở khung kế tiếp hoặc khi nạp game. */
    fun setOption(key: String, value: String) = post { NativeCore.nativeSetOption(key, value) }

    fun reset() = post { if (loaded) NativeCore.nativeReset() }

    // ---------- Lưu trạng thái / cheat ----------
    fun saveState(): ByteArray? = call { if (loaded) NativeCore.nativeSerialize() else null }
    fun loadState(data: ByteArray): Boolean = call { loaded && NativeCore.nativeUnserialize(data) }

    /**
     * Áp danh sách cheat; trả về từng mã có được lõi nhận hay không (rỗng nếu chưa có game).
     * Lõi đã vá gỡ cheat cũ ngay; lõi gốc không gỡ bản vá ROM nên phải nạp lại game sạch.
     */
    fun setCheats(codes: List<String>): List<Boolean> = call {
        if (!loaded) return@call emptyList<Boolean>()
        if (cheatsActive && !NativeCore.nativeCheatsPatched()) cleanReload()
        NativeCore.nativeCheatReset()
        val accepted = codes.mapIndexed { i, c -> NativeCore.nativeCheatSet(i, true, c) }
        cheatsActive = codes.isNotEmpty()
        accepted
    }

    /** Nạp lại game từ file ROM gốc nhưng giữ nguyên trạng thái đang chơi và SRAM. Gọi trên luồng giả lập. */
    private fun cleanReload() {
        val path = romPath ?: return
        try {
            saveSramNow()
            val sram = NativeCore.nativeGetSram()
            val state = NativeCore.nativeSerialize()
            NativeCore.nativeUnloadGame()
            val err = NativeCore.nativeLoadGame(path)
            if (err != null) {
                loaded = false
                paused = true
                return
            }
            if (sram != null && sram.isNotEmpty()) NativeCore.nativeSetSram(sram)
            if (state != null) NativeCore.nativeUnserialize(state)
            av = NativeCore.nativeAvInfo()
            lastSramHash = NativeCore.nativeGetSram()?.contentHashCode() ?: 0
            pushInput()
            cheatsActive = false
        } catch (e: Throwable) {
            // giữ nguyên trạng thái hiện tại nếu có lỗi bất ngờ
        }
    }

    // ---------- Hình ảnh ----------
    fun copyFrame(buffer: ByteBuffer, info: IntArray): Long = NativeCore.nativeCopyFrame(buffer, info)

    /** Chụp khung hình hiện tại thành Bitmap (chỉ hỗ trợ RGB565). */
    fun snapshot(): Bitmap? {
        if (!loaded) return null
        val info = IntArray(3)
        val buf = ByteBuffer.allocateDirect(1024 * 1024 * 4)
        val serial = copyFrame(buf, info)
        val (w, h, fmt) = Triple(info[0], info[1], info[2])
        if (serial <= 0 || w <= 0 || h <= 0 || fmt != 2) return null
        return runCatching {
            buf.position(0)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            buf.limit(w * h * 2)
            bmp.copyPixelsFromBuffer(buf)
            bmp
        }.getOrNull()
    }

    // ---------- Âm thanh ----------
    private fun startAudio(rate: Int) {
        stopAudio()
        if (rate <= 0) return
        try {
            val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val frameBytes = (rate / 60) * 4
            val size = max(minBuf * 2, frameBytes * 8)
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(size)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_NONE)
                .build()
            t.setVolume(if (soundOn) 1f else 0f)
            track = t
            appliedSpeed = 1f
            t.play()
            applySpeedToTrack()
        } catch (e: Throwable) {
            track = null // không có âm thanh: dùng nhịp thời gian
        }
    }

    private fun stopAudio() {
        track?.let {
            runCatching { it.pause() }
            runCatching { it.flush() }
            runCatching { it.release() }
        }
        track = null
    }

    private var appliedSpeed = 1f

    private fun applySpeedToTrack() {
        val t = track ?: return
        val s = speed
        if (s == appliedSpeed) return
        runCatching {
            t.playbackParams = PlaybackParams().setSpeed(s).setPitch(s)
            appliedSpeed = s
        }
    }

    // ---------- SRAM ----------
    private fun saveSramNow() {
        val id = currentId ?: return
        val data = NativeCore.nativeGetSram() ?: return
        val h = data.contentHashCode()
        if (h != lastSramHash) {
            lastSramHash = h
            sramSink?.invoke(id, data)
        }
    }

    // ---------- Vòng lặp ----------
    private fun loop() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        val audioBuf = ShortArray(8192)
        var frames = 0
        var fpsStart = System.nanoTime()
        var nextNs = System.nanoTime()
        var coreNs = 0L
        while (alive) {
            drainTasks()
            if (paused || !loaded) {
                try {
                    Thread.sleep(6)
                } catch (_: InterruptedException) {
                }
                nextNs = System.nanoTime()
                fpsStart = nextNs
                frames = 0
                continue
            }

            val c0 = System.nanoTime()
            NativeCore.nativeRunFrame()
            coreNs += System.nanoTime() - c0
            val n = NativeCore.nativeDrainAudio(audioBuf)
            val t = track
            if (t != null && n > 0) {
                // Không chặn: nhịp game do đồng hồ thời gian thực quyết định; âm thanh chỉ được đẩy vào bộ đệm.
                t.write(audioBuf, 0, n * 2, AudioTrack.WRITE_NON_BLOCKING)
            }
            nextNs += (1_000_000_000.0 / (av[0].coerceAtLeast(1.0) * speed)).toLong()
            val d = nextNs - System.nanoTime()
            if (d > 0) {
                java.util.concurrent.locks.LockSupport.parkNanos(d)
            } else if (d < -100_000_000L) {
                nextNs = System.nanoTime()
            }

            frames++
            val now = System.nanoTime()
            if (now - fpsStart >= 1_000_000_000L) {
                onFps?.invoke(frames * 1e9f / (now - fpsStart))
                if (frames > 0) onCoreMs?.invoke(coreNs / 1e6f / frames)
                coreNs = 0L
                frames = 0
                fpsStart = now
            }
            if (now - lastSramSaveNs >= 3_000_000_000L) {
                lastSramSaveNs = now
                saveSramNow()
            }
        }
    }
}
