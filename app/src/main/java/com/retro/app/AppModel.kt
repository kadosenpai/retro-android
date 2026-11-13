package com.retro.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retro.app.core.EmulatorEngine
import com.retro.app.data.ImportResult
import com.retro.app.data.Prefs
import com.retro.app.data.RomInfo
import com.retro.app.data.RomStore
import com.retro.app.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { LIBRARY, PLAY }

/** Trạng thái toàn ứng dụng. Sống ở mức Application nên không mất khi xoay màn hình. */
class AppModel(private val app: Application) {
    val store = RomStore(app)
    private val prefs = Prefs(app)
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
        private set
    var speed by mutableStateOf(1f)
        private set
    var menuOpen by mutableStateOf(false)

    init {
        EmulatorEngine.sramSink = { id, data -> runCatching { store.writeSram(id, data) } }
        EmulatorEngine.onFps = { fps = it }
        EmulatorEngine.soundOn = settings.sound
        refresh()
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

    // ---------- Chơi ----------
    fun open(rom: RomInfo) {
        if (loading) return
        scope.launch {
            loading = true
            val err = withContext(Dispatchers.IO) {
                val sram = store.readSram(rom.id)
                EmulatorEngine.load(app, store.romFile(rom.id), rom.id, sram)
            }
            loading = false
            if (err != null) {
                toast = err
                return@launch
            }
            speed = 1f
            EmulatorEngine.speed = 1f
            menuOpen = false
            current = rom
            screen = Screen.PLAY
            withContext(Dispatchers.IO) { store.touchPlayed(rom.id) }
            refresh()
        }
    }

    fun setSpeed(s: Float) {
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

    private suspend fun leaveGame() {
        withContext(Dispatchers.IO) { EmulatorEngine.unload() }
        current = null
        screen = Screen.LIBRARY
        refresh()
    }
}
