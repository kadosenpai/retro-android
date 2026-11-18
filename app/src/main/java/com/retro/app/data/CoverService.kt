package com.retro.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.CRC32

/**
 * Ảnh bìa game: tìm tên chuẩn theo CRC32 của ROM (dữ liệu libretro-database), rồi tải ảnh từ libretro-thumbnails.
 * Không khớp được thì thử theo tên file gốc.
 */
class CoverService(private val ctx: Context, private val store: RomStore) {
    private val mutex = Mutex()
    private val datMaps = HashMap<String, Map<Long, String>>()

    private fun datFile(sys: GameSystem) = File(ctx.cacheDir, "${sys.id}.dat")

    /** Tải ảnh bìa gốc và lưu lại. Trả true nếu đặt được ảnh bìa. */
    suspend fun fetchBoxArt(rom: RomInfo): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sys = GameSystem.fromId(rom.system)
            val file = store.romFile(rom.id)
            val names = LinkedHashSet<String>()
            for (crc in romCrcs(sys, file)) lookup(sys, crc)?.let { names.add(it) }
            val base = rom.fileBase.ifEmpty { rom.name }
            if (base.isNotEmpty()) names.add(base)
            // Không khớp mã ROM (bản dịch, bản lạ, tên kiểu GoodTools...): thử tên chuẩn gần đúng.
            fuzzyNames(sys, listOf(base, rom.name))?.let { names.addAll(it) }
            for (n in names) {
                val png = httpGet(CoverNames.boxArtUrl(sys, n)) ?: continue
                val jpeg = toCoverJpeg(png) ?: continue
                store.setCover(rom.id, jpeg)
                return@withLock true
            }
            false
        }
    }

    private val keyIndexes = HashMap<String, Map<String, List<String>>>()

    private fun fuzzyNames(sys: GameSystem, raws: List<String>): List<String>? {
        val map = datMaps[sys.id] ?: loadDat(sys, refresh = false) ?: return null
        val idx = keyIndexes.getOrPut(sys.id) { CoverNames.buildKeyIndex(map.values) }
        return CoverNames.fuzzyCandidates(idx, raws.filter { it.isNotEmpty() })
    }

    /** CRC32 của ROM; NES có thêm CRC không tính 16 byte đầu (DAT No-Intro thường tính không có header). */
    private fun romCrcs(sys: GameSystem, f: File): List<Long> {
        if (!f.exists()) return listOf(-1L)
        val full = crc32Of(f, 0)
        if (sys != GameSystem.NES) return listOf(full)
        val head = ByteArray(4)
        val isInes = runCatching { f.inputStream().use { it.read(head) == 4 } }.getOrDefault(false) &&
            head[0] == 'N'.code.toByte() && head[1] == 'E'.code.toByte() && head[2] == 'S'.code.toByte() && head[3] == 0x1A.toByte()
        return if (isInes) listOf(crc32Of(f, 16), full) else listOf(full)
    }

    private fun lookup(sys: GameSystem, crc: Long): String? {
        if (crc < 0) return null
        var map = datMaps[sys.id] ?: loadDat(sys, refresh = false)
        map?.get(crc)?.let { return it }
        // Không thấy: nếu bản DAT đã cũ (hơn 14 ngày) thì tải lại một lần.
        val f = datFile(sys)
        val old = !f.exists() || System.currentTimeMillis() - f.lastModified() > 14L * 24 * 3600 * 1000
        if (old) {
            map = loadDat(sys, refresh = true)
            return map?.get(crc)
        }
        return null
    }

    private fun loadDat(sys: GameSystem, refresh: Boolean): Map<Long, String>? {
        val f = datFile(sys)
        if (refresh || !f.exists()) {
            val bytes = httpGet(CoverNames.datUrl(sys), maxBytes = 16 * 1024 * 1024) ?: return datMaps[sys.id]
            runCatching { f.writeBytes(bytes) }
        }
        val parsed = runCatching { CoverNames.parseDat(f.readText()) }.getOrNull()
        if (parsed != null && parsed.isNotEmpty()) datMaps[sys.id] = parsed
        return datMaps[sys.id]
    }

    private fun crc32Of(f: File, skip: Long): Long {
        if (!f.exists()) return -1
        val c = CRC32()
        f.inputStream().use { input ->
            if (skip > 0) input.skip(skip)
            val buf = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                c.update(buf, 0, n)
            }
        }
        return c.value
    }

    private fun httpGet(url: String, maxBytes: Int = 4 * 1024 * 1024): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Retro/1.0")
            }
            if (conn.responseCode != 200) return null
            val out = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buf = ByteArray(32 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > maxBytes) return null
                }
            }
            out.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        /** Thu nhỏ ảnh (cạnh dài tối đa [maxSide]) và nén JPEG. Trả null nếu không đọc được ảnh. */
        fun toCoverJpeg(src: ByteArray, maxSide: Int = 320): ByteArray? = try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(src, 0, src.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                null
            } else {
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp = BitmapFactory.decodeByteArray(src, 0, src.size, opts)
                if (bmp == null) {
                    null
                } else {
                    val k = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
                    val w = maxOf(1, Math.round(bmp.width * k))
                    val h = maxOf(1, Math.round(bmp.height * k))
                    val scaled = if (w == bmp.width && h == bmp.height) bmp else Bitmap.createScaledBitmap(bmp, w, h, true)
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                }
            }
        } catch (e: Throwable) {
            null
        }
    }
}
