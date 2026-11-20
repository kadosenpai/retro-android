package com.retro.app.data

import android.content.Context

enum class SortKey(val label: String) {
    RECENT("Chơi gần đây"),
    NAME("Tên A–Z"),
    ADDED("Mới thêm"),
}

/** Độ trễ âm thanh: đệm sẵn [prefillMs] ms, hàng đợi vượt [maxMs] ms thì bỏ bớt (đệm ít = sát hình hơn nhưng dễ lẹt xẹt). */
enum class AudioLatency(val label: String, val hint: String, val prefillMs: Int, val maxMs: Int) {
    RESPONSIVE("Phản hồi nhanh", "Tiếng sát hình nhất. Máy yếu có thể bị lẹt xẹt (NDS, PlayStation)", 10, 25),
    BALANCED("Cân bằng", "Mặc định, hợp với hầu hết máy", 30, 60),
    STABLE("Tiếng ổn định", "Tiếng không bị đứt, trễ hơn một chút. Hợp máy yếu hoặc tai nghe Bluetooth", 60, 120),
}

/** Máy chủ thay thế cho Nintendo Wi-Fi Connection (đã đóng) mà lõi NDS dùng để chơi online; [value] là giá trị DNS đưa cho lõi. */
enum class WfcServer(val label: String, val hint: String, val value: String) {
    KAERU("Kaeru WFC", "Mặc định, hợp với nhiều game", "178.62.43.212"),
    WIIMMFI("Wiimmfi", "Hay dùng cho Mario Kart DS và vài game khác", "95.217.77.181"),
    ALTWFC("AltWFC", "Máy chủ cộng đồng khác", "172.104.88.237"),
}

data class Settings(
    val sound: Boolean = true,
    val audioLatency: AudioLatency = AudioLatency.BALANCED,
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
    /** Wi-Fi của máy NDS (chơi online). Tắt thì game không thấy mạng. */
    val wifi: Boolean = true,
    val wfcServer: WfcServer = WfcServer.KAERU,
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("retro-settings", Context.MODE_PRIVATE)

    fun load(): Settings {
        val d = Settings()
        return Settings(
            sound = sp.getBoolean("sound", d.sound),
            audioLatency = enumOf(sp.getString("audioLatency", null), d.audioLatency),
            showPad = sp.getBoolean("showPad", d.showPad),
            haptics = sp.getBoolean("haptics", d.haptics),
            showStats = sp.getBoolean("showStats", d.showStats),
            sticky = sp.getBoolean("sticky", d.sticky),
            sort = enumOf(sp.getString("sort", null), d.sort),
            tab = sp.getString("tab", null) ?: d.tab,
            gbPalette = sp.getString("gbPalette", null) ?: d.gbPalette,
            sgbBorder = sp.getBoolean("sgbBorder", d.sgbBorder),
            wifi = sp.getBoolean("wifi", d.wifi),
            wfcServer = enumOf(sp.getString("wfcServer", null), d.wfcServer),
        )
    }

    fun save(s: Settings) {
        sp.edit()
            .putBoolean("sound", s.sound)
            .putString("audioLatency", s.audioLatency.name)
            .putBoolean("showPad", s.showPad)
            .putBoolean("haptics", s.haptics)
            .putBoolean("showStats", s.showStats)
            .putBoolean("sticky", s.sticky)
            .putString("sort", s.sort.name)
            .putString("tab", s.tab)
            .putString("gbPalette", s.gbPalette)
            .putBoolean("sgbBorder", s.sgbBorder)
            .putBoolean("wifi", s.wifi)
            .putString("wfcServer", s.wfcServer.name)
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
