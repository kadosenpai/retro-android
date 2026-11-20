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
import com.retro.app.data.GameSystem
import com.retro.app.data.ImportResult
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
    var gameFolder by mutableStateOf(prefs.gameFolder())
        private set
    var gameFolderName by mutableStateOf(prefs.gameFolderName())
        private set
    var scanning by mutableStateOf(false)
        private set

    init {
        EmulatorEngine.sramSink = { id, data -> runCatching { store.writeSram(id, data) } }
        EmulatorEngine.onFps = { fps = it }
        EmulatorEngine.onCoreMs = { coreMs = it }
        EmulatorEngine.soundOn = settings.sound
        EmulatorEngine.audioLatency = settings.audioLatency
        applyCoreOptions()
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
        EmulatorEngine.audioLatency = settings.audioLatency
        applyCoreOptions()
    }

    /** Đẩy các tuỳ chọn vào lõi (Game Boy, NES, NDS) (bảng màu đổi ngay; viền SGB có hiệu lực khi mở lại game). */
    private fun applyCoreOptions() {
        EmulatorEngine.setOption("mgba_gb_colors", settings.gbPalette)
        EmulatorEngine.setOption("mgba_sgb_borders", if (settings.sgbBorder) "ON" else "OFF")
        // NES (FCEUmm): cắt 8 hàng trên/dưới như màn hình TV (256x224), giữ nguyên hai bên.
        // NDS (melonDS): ghép hai màn dọc, không khe hở; cảm ứng kiểu chạm; chỉ dùng bộ vẽ phần mềm.
        EmulatorEngine.setOption("melonds_touch_mode", "Touch")
        EmulatorEngine.setOption("melonds_screen_layout", "Top/Bottom")
        EmulatorEngine.setOption("melonds_screen_gap", "0")
        EmulatorEngine.setOption("melonds_opengl_renderer", "disabled")
        EmulatorEngine.setOption("melonds_boot_directly", "enabled")
        EmulatorEngine.setOption("fceumm_overscan", "disabled")
        EmulatorEngine.setOption("fceumm_overscan_h_left", "0")
        EmulatorEngine.setOption("fceumm_overscan_h_right", "0")
        EmulatorEngine.setOption("fceumm_overscan_v_top", "8")
        EmulatorEngine.setOption("fceumm_overscan_v_bottom", "8")
    }

    fun showToast(msg: String) {
        toast = msg
    }

    // ---------- Thư viện ----------
    /** Lưu thư mục game người dùng vừa chọn (giữ quyền đọc qua các lần mở app). */
    fun setGameFolder(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        val id = try {
            android.provider.DocumentsContract.getTreeDocumentId(uri)
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Thư mục game"
        }
        val name = id.substringAfter(':', id).ifEmpty { "Bộ nhớ trong" }
        prefs.saveGameFolder(uri.toString(), name)
        gameFolder = uri.toString()
        gameFolderName = name
    }

    /** Quét thư mục game, chép mọi ROM mới vào thư viện. */
    fun scanGameFolder() {
        val tree = gameFolder ?: return
        if (scanning) return
        scanning = true
        toast = "Đang quét thư mục game…"
        scope.launch {
            val added = try {
                withContext(Dispatchers.IO) { store.scanTree(Uri.parse(tree)) }
            } catch (_: Exception) {
                null
            }
            scanning = false
            if (added == null) {
                toast = "Không đọc được thư mục game. Hãy chọn lại thư mục trong Cài đặt."
                return@launch
            }
            refresh()
            autoCoverMissing()
            added.lastOrNull()?.let { a ->
                val tab = GameSystem.fromId(a.system).tab
                if (settings.tab != tab) update { it.copy(tab = tab) }
            }
            toast = "Đã thêm ${added.size} game"
        }
    }

    fun addRoms(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val results = withContext(Dispatchers.IO) { store.importAll(uris) }
            refresh()
            autoCoverMissing()
            val added = results.filterIsInstance<ImportResult.Added>()
            val dup = results.filterIsInstance<ImportResult.Duplicate>().size
            val rejected = results.filterIsInstance<ImportResult.Rejected>()
            // Thêm game của hệ nào thì chuyển sang tab của hệ đó.
            added.lastOrNull()?.let { a ->
                val tab = GameSystem.fromId(a.rom.system).tab
                if (settings.tab != tab) update { it.copy(tab = tab) }
            }
            toast = when {
                rejected.isNotEmpty() -> rejected.first().reason
                added.size == 1 -> "Đã thêm \"${added[0].rom.name}\". Chạm vào game để chơi."
                added.size > 1 -> "Đã thêm ${added.size} game"
                dup > 0 -> "Game này đã có trong thư viện"
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

    /** Xoá toàn bộ game cùng ảnh bìa, cheat, Save State và .sav. */
    fun deleteAll() {
        scope.launch {
            if (current != null) leaveGame()
            withContext(Dispatchers.IO) {
                store.list().forEach { store.delete(it.id) }
            }
            refresh()
            toast = "Đã xoá hết game"
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

    // ---------- Cheat ----------
    /** Đưa các cheat đang bật vào lõi. Gọi trên luồng IO. */
    private fun applyCheatList(list: List<CheatItem>, rom: RomInfo): Set<String> {
        val kind = GameSystem.fromId(rom.system).cheatKind
        val items = list.filter { it.enabled }.map { it to CheatCodes.normalize(it.code, kind) }.filter { it.second.isNotEmpty() }
        val accepted = EmulatorEngine.setCheats(items.map { it.second })
        // Trả về id các cheat mà lõi không đọc được.
        return items.filterIndexed { i, _ -> !(accepted.getOrNull(i) ?: true) }.map { it.first.id }.toSet()
    }

    private fun commitCheats(list: List<CheatItem>) {
        val rom = current ?: return
        cheats = list
        scope.launch {
            val rejected = withContext(Dispatchers.IO) { applyCheatList(list, rom) }
            if (rejected.isEmpty()) {
                withContext(Dispatchers.IO) { runCatching { store.writeCheats(rom.id, list) } }
                return@launch
            }
            // Lõi không nhận mã: bỏ khỏi danh sách và báo cho người dùng.
            val names = list.filter { it.id in rejected }.joinToString { "\"${it.name}\"" }
            val keep = list.filter { it.id !in rejected }
            cheats = keep
            withContext(Dispatchers.IO) {
                runCatching { store.writeCheats(rom.id, keep) }
                applyCheatList(keep, rom)
            }
            toast = "Lõi không nhận mã $names. Kiểm tra lại loại mã (GameShark, Action Replay, Game Genie…)"
        }
    }

    /** Thêm cheat; trả về thông báo lỗi nếu mã không hợp lệ, null nếu đã thêm. */
    fun addCheat(name: String, code: String): String? {
        CheatCodes.validate(code, GameSystem.fromId(current?.system).cheatKind)?.let { return it }
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
                val e = EmulatorEngine.load(app, store.romFile(rom.id), rom.id, sram, GameSystem.fromId(rom.system))
                if (e == null) {
                    loadedCheats = store.readCheats(rom.id)
                    applyCheatList(loadedCheats, rom)
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
        current = null
        screen = Screen.LIBRARY
        refresh()
    }
}
