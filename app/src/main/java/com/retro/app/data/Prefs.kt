package com.retro.app.data

import android.content.Context

enum class SortKey(val label: String) {
    RECENT("Chơi gần đây"),
    NAME("Tên A–Z"),
    ADDED("Mới thêm"),
}

data class Settings(
    val sound: Boolean = true,
    val showPad: Boolean = true,
    val haptics: Boolean = true,
    val showStats: Boolean = false,
    /** Giữ nút: chạm một lần để giữ, chạm lần nữa để nhả. */
    val sticky: Boolean = false,
    val sort: SortKey = SortKey.RECENT,
    /** Tab đang chọn trong thư viện (xem [GameSystem.TABS]). */
    val tab: String = "gba",
    /** Bảng màu Game Boy đời đầu: tên preset của mGBA (Grayscale, DMG Green, GB Pocket, GB Light). */
    val gbPalette: String = "Grayscale",
    /** Hiện viền Super Game Boy (áp dụng khi mở lại game). */
    val sgbBorder: Boolean = true,
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("retro-settings", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            sound = sp.getBoolean("sound", d.sound),
            showPad = sp.getBoolean("showPad", d.showPad),
            haptics = sp.getBoolean("haptics", d.haptics),
            showStats = sp.getBoolean("showStats", d.showStats),
            sticky = sp.getBoolean("sticky", d.sticky),
            sort = enumOf(sp.getString("sort", null), d.sort),
            tab = sp.getString("tab", null) ?: d.tab,
            gbPalette = sp.getString("gbPalette", null) ?: d.gbPalette,
            sgbBorder = sp.getBoolean("sgbBorder", d.sgbBorder),
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putBoolean("sound", s.sound)
            .putBoolean("showPad", s.showPad)
            .putBoolean("haptics", s.haptics)
            .putBoolean("showStats", s.showStats)
            .putBoolean("sticky", s.sticky)
            .putString("sort", s.sort.name)
            .putString("tab", s.tab)
            .putString("gbPalette", s.gbPalette)
            .putBoolean("sgbBorder", s.sgbBorder)
            .apply()
    }

    /** Thư mục game đã chọn (Uri cây tài liệu) và tên hiển thị của nó; null = chưa chọn. */
    fun gameFolder(): String? = sp.getString("gameFolder", null)

    fun gameFolderName(): String? = sp.getString("gameFolderName", null)

    fun saveGameFolder(uri: String, name: String) {
        sp.edit().putString("gameFolder", uri).putString("gameFolderName", name).apply()
    }

    fun loadPadMap(): ControllerMap = ControllerMap.decode(sp.getString("padmap", null))

    fun savePadMap(m: ControllerMap) {
        sp.edit().putString("padmap", m.encode()).apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
