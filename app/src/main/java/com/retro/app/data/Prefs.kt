package com.retro.app.data

import android.content.Context
import com.retro.app.render.FilterMode
import com.retro.app.render.ScaleMode

enum class SortKey(val label: String) {
    RECENT("Chơi gần đây"),
    NAME("Tên A–Z"),
    ADDED("Mới thêm"),
}

data class Settings(
    val scale: ScaleMode = ScaleMode.INTEGER,
    val filter: FilterMode = FilterMode.SHARP,
    val sound: Boolean = true,
    val showPad: Boolean = true,
    val haptics: Boolean = true,
    val showStats: Boolean = false,
    /** Giữ nút: chạm một lần để giữ, chạm lần nữa để nhả. */
    val sticky: Boolean = false,
    val sort: SortKey = SortKey.RECENT,
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("retro-settings", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            scale = enumOf(sp.getString("scale", null), d.scale),
            filter = enumOf(sp.getString("filter", null), d.filter),
            sound = sp.getBoolean("sound", d.sound),
            showPad = sp.getBoolean("showPad", d.showPad),
            haptics = sp.getBoolean("haptics", d.haptics),
            showStats = sp.getBoolean("showStats", d.showStats),
            sticky = sp.getBoolean("sticky", d.sticky),
            sort = enumOf(sp.getString("sort", null), d.sort),
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putString("scale", s.scale.name)
            .putString("filter", s.filter.name)
            .putBoolean("sound", s.sound)
            .putBoolean("showPad", s.showPad)
            .putBoolean("haptics", s.haptics)
            .putBoolean("showStats", s.showStats)
            .putBoolean("sticky", s.sticky)
            .putString("sort", s.sort.name)
            .apply()
    }

    fun loadPadMap(): ControllerMap = ControllerMap.decode(sp.getString("padmap", null))

    fun savePadMap(m: ControllerMap) {
        sp.edit().putString("padmap", m.encode()).apply()
    }

    /** Bố cục nút ảo riêng cho màn ngang ([land] = true) hoặc màn dọc. */
    fun loadLayout(land: Boolean): PadLayout = PadLayout.decode(sp.getString(if (land) "layoutL" else "layoutP", null))

    fun saveLayout(land: Boolean, l: PadLayout) {
        sp.edit().putString(if (land) "layoutL" else "layoutP", l.encode()).apply()
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
