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
    /** API key Gemini do người dùng tự dán; chỉ lưu trên máy này. */
    val geminiKey: String = "",
    /** Tên model Gemini dùng để dịch (có thể đổi vì Google đổi tên model theo thời gian). */
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
)

const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash-lite"

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
            geminiKey = sp.getString("geminiKey", null) ?: d.geminiKey,
            geminiModel = sp.getString("geminiModel", null) ?: d.geminiModel,
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
            .putString("geminiKey", s.geminiKey)
            .putString("geminiModel", s.geminiModel)
            .apply()
    }

    fun loadPadMap(): ControllerMap = ControllerMap.decode(sp.getString("padmap", null))

    fun savePadMap(m: ControllerMap) {
        sp.edit().putString("padmap", m.encode()).apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
