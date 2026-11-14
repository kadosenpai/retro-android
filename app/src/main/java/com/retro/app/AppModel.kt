package com.retro.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.retro.app.core.EmulatorEngine
import com.retro.app.data.CheatCodes
import com.retro.app.data.CheatItem
import com.retro.app.data.ControllerMap
import com.retro.app.data.CoverService
import com.retro.app.data.GbaButton
import com.retro.app.data.ImportResult
import com.retro.app.data.PadLayout
import com.retro.app.data.Prefs
import com.retro.app.data.RomInfo
import com.retro.app.data.RomStore
import com.retro.app.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class Screen { LIBRARY, PLAY }

/** Một ô trạng thái hiển thị trong menu. savedAt = 0 nghĩa là ô trống. */
class SlotView(val slot: Int, val savedAt: Long, val thumb: ImageBitmap?)

/** Trạng thái toàn ứng dụng. Sống ở mức Application nên không mất khi xoay màn hình. */
class AppModel(private val app: Application) {
    val store = RomStore(app)
    private val prefs = Prefs(app)
    private val covers = CoverService(app, store)
    private var coverJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var roms by mutableStateOf<List<RomInfo>>(emptyList())
        private set
    var screen by mutableStateOf(Screen.LIBRARY)
        private set
    var current by mutableStateOf<RomInfo?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var toast by mutableStateOf<String?>(null)
    var settings by mutableStateOf(prefs.load())
        private set
    var fps by mutableStateOf(0f)
    var coreMs by mutableStateOf(0f)
        private set
    var speed by mutableStateOf(1f)
        private set
    var menuOpen by mutableStateOf(false)
    var slots by mutableStateOf<List<SlotView>>(emptyList())
        private set
    var cheats by mutableStateOf<List<CheatItem>>(emptyList())
        private set
    var controllerMap by mutableStateOf(prefs.loadPadMap())
        private set
    var layoutPortrait by mutableStateOf(prefs.loadLayout(false))
        private set
    var layoutLandscape by mutableStateOf(prefs.loadLayout(true))
        private set
    /** Đang ở chế độ chỉnh bố cục nút ảo. */
    var editingLayout by mutableStateOf(false)

    init {
        EmulatorEngine.sramSink = { id, data -> runCatching { store.writeSram(id, data) } }
        EmulatorEngine.onFps = { fps = it }
        EmulatorEngine.onCoreMs = { coreMs = it }
        EmulatorEngine.soundOn = settings.sound
        refresh()
        autoCoverMissing()
    }

    fun refresh() {
        roms = store.list()
    }

    fun update(change: (Settings) -> Settings) {
        settings = change(settings)
        prefs.save(settings)
        EmulatorEngine.soundOn = settings.sound
    }

    fun showToast(msg: String) {
        toast = msg
    }

    // ---------- Thư viện ----------
    fun addRoms(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val results = withContext(Dispatchers.IO) { uris.map { store.import(it) } }
            refresh()
            autoCoverMissing()
            val added = results.filterIsInstance<ImportResult.Added>()
            val dup = results.filterIsInstance<ImportResult.Duplicate>().size
            val rejected = results.filterIsInstance<ImportResult.Rejected>()
            toast = when {
                rejected.isNotEmpty() -> rejected.first().reason
                added.size == 1 -> "Đã thêm \"${added[0].rom.name}\". Chạm vào băng để chơi."
                added.size > 1 -> "Đã thêm ${added.size} băng"
                dup > 0 -> "Băng này đã có trong thư viện"
                else -> null
            }
        }
    }

    fun rename(rom: RomInfo, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) { store.rename(rom.id, n) }
            refresh()
        }
    }

    fun delete(rom: RomInfo) {
        scope.launch {
            if (current?.id == rom.id) leaveGame()
            withContext(Dispatchers.IO) { store.delete(rom.id) }
            refresh()
            toast = "Đã xoá \"${rom.name}\""
        }
    }

    // ---------- Tay cầm & bố cục nút ảo ----------
    fun assignKey(button: GbaButton, code: Int) {
        controllerMap = controllerMap.assign(button, code)
        prefs.savePadMap(controllerMap)
    }

    fun resetControllerMap() {
        controllerMap = ControllerMap.defaults()
        prefs.savePadMap(controllerMap)
    }

    fun setLayout(land: Boolean, layout: PadLayout) {
        if (land) layoutLandscape = layout else layoutPortrait = layout
        prefs.saveLayout(land, layout)
    }

    // ---------- Cheat ----------
    /** Đưa các cheat đang bật vào lõi. Gọi trên luồng IO. */
    private fun applyCheatList(list: List<CheatItem>) {
        val codes = list.filter { it.enabled }.map { CheatCodes.normalize(it.code) }.filter { it.isNotEmpty() }
        EmulatorEngine.setCheats(codes)
    }

    private fun commitCheats(list: List<CheatItem>) {
        val rom = current ?: return
        cheats = list
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { store.writeCheats(rom.id, list) }
                applyCheatList(list)
            }
        }
    }

    /** Thêm cheat; trả về thông báo lỗi nếu mã không hợp lệ, null nếu đã thêm. */
    fun addCheat(name: String, code: String): String? {
        CheatCodes.validate(code)?.let { return it }
        val n = name.trim().ifEmpty { "Cheat ${cheats.size + 1}" }
        commitCheats(cheats + CheatItem(java.util.UUID.randomUUID().toString(), n, code.trim(), true))
        return null
    }

    fun toggleCheat(id: String, enabled: Boolean) =
        commitCheats(cheats.map { if (it.id == id) it.copy(enabled = enabled) else it })

    fun deleteCheat(id: String) = commitCheats(cheats.filter { it.id != id })

    // ---------- Ảnh bìa ----------
    /** Tự tải ảnh bìa ở nền cho các game chưa có (mỗi game thử lại sau tối thiểu 7 ngày nếu thất bại). */
    fun autoCoverMissing() {
        if (coverJob?.isActive == true) return
        coverJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val next = withContext(Dispatchers.IO) {
                    store.list().firstOrNull { it.coverAt == 0L && now - it.coverTriedAt > 7L * 24 * 3600 * 1000 }
                } ?: break
                val ok = covers.fetchBoxArt(next)
                if (!ok) withContext(Dispatchers.IO) { store.markCoverTried(next.id) }
                refresh()
            }
        }
    }

    fun fetchCoverNow(rom: RomInfo) {
        scope.launch {
            toast = "Đang tải ảnh bìa…"
            val ok = covers.fetchBoxArt(rom)
            if (!ok) withContext(Dispatchers.IO) { store.markCoverTried(rom.id) }
            refresh()
            toast = if (ok) "Đã tải ảnh bìa cho \"${rom.name}\""
            else "Không tìm thấy ảnh bìa gốc (không có mạng hoặc tên game khác chuẩn). Bạn có thể chọn ảnh từ máy."
        }
    }

    fun pickCover(rom: RomInfo, uri: Uri) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val bytes = runCatching {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                val jpeg = if (bytes != null && bytes.size <= 20 * 1024 * 1024) CoverService.toCoverJpeg(bytes) else null
                if (jpeg != null) {
                    store.setCover(rom.id, jpeg)
                    true
                } else false
            }
            refresh()
            toast = if (ok) "Đã đổi ảnh bìa" else "Không đọc được file ảnh này"
        }
    }

    // ---------- Save game (.sav) ----------
    fun exportSave(rom: RomInfo, uri: Uri) {
        scope.launch {
            val err = withContext(Dispatchers.IO) { store.exportSram(rom.id, uri) }
            toast = err ?: "Đã xuất save của \"${rom.name}\""
        }
    }

    fun importSave(rom: RomInfo, uri: Uri) {
        scope.launch {
            val err = withContext(Dispatchers.IO) { store.importSram(rom.id, uri) }
            toast = err ?: "Đã nhập save cho \"${rom.name}\". Mở game để dùng."
        }
    }

    // ---------- Chơi ----------
    fun open(rom: RomInfo) {
        if (loading) return
        scope.launch {
            loading = true
            var resumed = false
            var loadedCheats: List<CheatItem> = emptyList()
            val err = withContext(Dispatchers.IO) {
                val sram = store.readSram(rom.id)
                val e = EmulatorEngine.load(app, store.romFile(rom.id), rom.id, sram)
                if (e == null) {
                    loadedCheats = store.readCheats(rom.id)
                    applyCheatList(loadedCheats)
                    // Tiếp tục từ lần chơi trước (trạng thái tự động); hỏng thì bỏ qua.
                    val auto = store.readState(rom.id, RomStore.AUTO_SLOT)
                    if (auto != null) {
                        resumed = EmulatorEngine.loadState(auto)
                        if (!resumed) store.deleteState(rom.id, RomStore.AUTO_SLOT)
                    }
                }
                e
            }
            loading = false
            if (err != null) {
                toast = err
                return@launch
            }
            cheats = loadedCheats
            if (resumed) toast = "Tiếp tục từ lần chơi trước"
            speed = 1f
            EmulatorEngine.speed = 1f
            menuOpen = false
            current = rom
            screen = Screen.PLAY
            withContext(Dispatchers.IO) { store.touchPlayed(rom.id) }
            refresh()
        }
    }

    fun changeSpeed(s: Float) {
        speed = s
        EmulatorEngine.speed = s
    }

    fun pauseGame() {
        if (screen == Screen.PLAY) EmulatorEngine.pause()
    }

    fun resumeGame() {
        if (screen == Screen.PLAY && !menuOpen) EmulatorEngine.resume()
    }

    fun resetGame() {
        EmulatorEngine.reset()
    }

    fun exitToLibrary() {
        scope.launch { leaveGame() }
    }

    // ---------- Trạng thái ----------
    private fun thumbPng(bmp: Bitmap?): ByteArray? {
        if (bmp == null) return null
        return runCatching {
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }.getOrNull()
    }

    /** Lưu trạng thái hiện tại vào ô [slot]; trả true nếu thành công. Gọi trên luồng IO. */
    private fun writeSlot(rom: RomInfo, slot: Int): Boolean {
        val data = EmulatorEngine.saveState() ?: return false
        val png = thumbPng(EmulatorEngine.snapshot())
        return runCatching { store.writeState(rom.id, slot, data, png) }.isSuccess
    }

    fun refreshSlots() {
        val rom = current ?: run {
            slots = emptyList()
            return
        }
        scope.launch {
            slots = withContext(Dispatchers.IO) {
                (0..RomStore.SLOT_COUNT).map { n ->
                    val t = store.stateTime(rom.id, n)
                    val img = if (t > 0) {
                        store.readThumb(rom.id, n)?.let { b ->
                            runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()
                        }
                    } else null
                    SlotView(n, t, img)
                }
            }
        }
    }

    fun saveSlot(slot: Int) {
        val rom = current ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { writeSlot(rom, slot) }
            toast = if (ok) "Đã lưu vào ô $slot" else "Không lưu được trạng thái"
            refreshSlots()
        }
    }

    /** Nạp trạng thái từ ô [slot]; trả về qua [done] (true nếu thành công). */
    fun loadSlot(slot: Int, done: (Boolean) -> Unit = {}) {
        val rom = current ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val data = store.readState(rom.id, slot) ?: return@withContext false
                EmulatorEngine.loadState(data)
            }
            toast = if (ok) "Đã nạp trạng thái" else "Không nạp được (trạng thái cũ hoặc lõi đã đổi)"
            done(ok)
        }
    }

    /** Tự lưu khi tạm dừng/thoát để lần sau chơi tiếp. */
    fun autoSave() {
        val rom = current ?: return
        if (screen != Screen.PLAY) return
        scope.launch { withContext(Dispatchers.IO) { writeSlot(rom, RomStore.AUTO_SLOT) } }
    }

    private suspend fun leaveGame() {
        current?.let { rom -> withContext(Dispatchers.IO) { writeSlot(rom, RomStore.AUTO_SLOT) } }
        withContext(Dispatchers.IO) { EmulatorEngine.unload() }
        cheats = emptyList()
        editingLayout = false
        current = null
        screen = Screen.LIBRARY
        refresh()
    }
}
