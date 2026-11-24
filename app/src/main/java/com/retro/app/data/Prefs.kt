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

/**
 * Bộ lọc hình. Thứ tự (ordinal) phải khớp `uMode` trong shader của GameRenderer: 0 thô, 1 sắc nét, 2 LCD, 3 CRT.
 */
enum class DisplayFilter(val label: String, val hint: String) {
    RAW("Pixel thô", "Từng pixel là một ô vuông, cạnh sắc nhất. Khi phóng lẻ, pixel có thể to nhỏ không đều"),
    SHARP("Sắc nét", "Giữ pixel nét, chỉ làm mịn mép khi phóng lẻ. Khuyên dùng"),
    LCD("LCD", "Lưới pixel mờ như màn hình máy cầm tay. Hợp Game Boy, GBA, NDS"),
    CRT("CRT", "Đường quét và chấm màu như TV ống. Hợp NES, SNES, Mega Drive, PlayStation"),
}

/** Cách phóng to hình game trong vùng hiển thị. */
enum class ScaleMode(val label: String, val hint: String) {
    INTEGER("Nguyên số", "Phóng 1×, 2×, 3×... Pixel đều và nét nhất, chừa viền đen"),
    FIT("Vừa màn hình", "Giữ tỉ lệ, phóng lớn nhất có thể. Nên dùng cùng bộ lọc Sắc nét"),
    SMALLER("Nhỏ hơn một bậc", "Nguyên số thấp hơn một bậc: pixel tinh hơn, viền đen rộng hơn"),
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
    /** Tab đang chọn trong thư viện: "all" (Tất cả) hoặc một id trong [GameSystem.TABS]. */
    val tab: String = "all",
    /** Bảng màu Game Boy đời đầu: tên preset của mGBA (Grayscale, DMG Green, GB Pocket, GB Light). */
    val gbPalette: String = "Grayscale",
    /** Hiện viền Super Game Boy (áp dụng khi mở lại game). */
    val sgbBorder: Boolean = true,
    /** Bộ lọc hình, cường độ của LCD/CRT (0..1) và cách phóng to. */
    val filter: DisplayFilter = DisplayFilter.SHARP,
    val filterLevel: Float = 0.6f,
    val scaleMode: ScaleMode = ScaleMode.INTEGER,
    /** Hiệu ứng kính (làm mờ và khúc xạ nền) ở thư viện; tắt thì dùng nền mờ đơn giản. */
    val glass: Boolean = true,
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
            filter = enumOf(sp.getString("filter", null), d.filter),
            filterLevel = sp.getFloat("filterLevel", d.filterLevel).coerceIn(0f, 1f),
            scaleMode = enumOf(sp.getString("scaleMode", null), d.scaleMode),
            glass = sp.getBoolean("glass", d.glass),
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
            .putString("filter", s.filter.name)
            .putFloat("filterLevel", s.filterLevel)
            .putString("scaleMode", s.scaleMode.name)
            .putBoolean("glass", s.glass)
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
