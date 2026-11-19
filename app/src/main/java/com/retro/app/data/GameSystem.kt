package com.retro.app.data

/** Lõi giả lập (libretro) đứng sau mỗi hệ máy; file lõi là `lib<id>_libretro.so`. */
enum class Core(val id: String) {
    MGBA("mgba"),
    FCEUMM("fceumm"),
    SNES9X("snes9x"),
    MELONDS("melonds"),
    GENESIS("genesis"),
    PCSX("pcsx");

    val libName: String get() = "lib${id}_libretro.so"
}

/** Hệ máy được hỗ trợ. Mỗi hệ có đuôi file ROM, tên DAT/thư mục ảnh bìa của libretro và tab hiển thị trong thư viện. */
enum class GameSystem(
    val id: String,
    /** Tên đầy đủ (hiện ở tiêu đề thư viện và thông báo). */
    val label: String,
    /** Id của tab trong thư viện (mỗi hệ một tab riêng). */
    val tab: String,
    val exts: Set<String>,
    val datName: String,
    val thumbDir: String,
    val minRomSize: Long,
    val core: Core,
    /** Loại mã cheat hệ này dùng. */
    val cheatKind: CheatKind,
    /** Tỉ lệ khung hình hiển thị (rộng/cao); 0 = theo đúng kích thước khung của lõi. */
    val aspect: Float = 0f,
) {
    GBA("gba", "Game Boy Advance", "gba", setOf("gba", "agb", "bin"), "Nintendo - Game Boy Advance", "Nintendo_-_Game_Boy_Advance", 0x200, Core.MGBA, CheatKind.GBA),
    GB("gb", "Game Boy", "gb", setOf("gb"), "Nintendo - Game Boy", "Nintendo_-_Game_Boy", 0x150, Core.MGBA, CheatKind.GB),
    GBC("gbc", "Game Boy Color", "gbc", setOf("gbc", "cgb"), "Nintendo - Game Boy Color", "Nintendo_-_Game_Boy_Color", 0x150, Core.MGBA, CheatKind.GB),
    NES("nes", "NES", "nes", setOf("nes", "unf", "unif"), "Nintendo - Nintendo Entertainment System", "Nintendo_-_Nintendo_Entertainment_System", 0x2010, Core.FCEUMM, CheatKind.NES),
    SNES("snes", "SNES", "snes", setOf("sfc", "smc", "swc", "fig"), "Nintendo - Super Nintendo Entertainment System", "Nintendo_-_Super_Nintendo_Entertainment_System", 0x8000, Core.SNES9X, CheatKind.SNES),
    NDS("nds", "NDS", "nds", setOf("nds", "dsi", "ids"), "Nintendo - Nintendo DS", "Nintendo_-_Nintendo_DS", 0x200, Core.MELONDS, CheatKind.NDS),
    // Mega Drive: ".bin" đã thuộc GBA nên chỉ nhận md/gen/smd. Màn 256 hoặc 320 điểm đều hiển thị 4:3.
    MD("md", "Mega Drive", "md", setOf("md", "gen", "smd"), "Sega - Mega Drive - Genesis", "Sega_-_Mega_Drive_-_Genesis", 0x1000, Core.GENESIS, CheatKind.MD, 4f / 3f),
    // PlayStation: chỉ nhận ảnh đĩa một file (chd, pbp, iso); lõi đọc thẳng từ file nên ảnh nặng không bị nạp vào RAM.
    PSX("psx", "PlayStation", "psx", setOf("chd", "pbp", "iso"), "Sony - PlayStation", "Sony_-_PlayStation", 0x10000, Core.PCSX, CheatKind.PSX, 4f / 3f);

    companion object {
        fun fromId(id: String?): GameSystem = values().firstOrNull { it.id == id } ?: GBA

        fun fromExt(ext: String): GameSystem? = values().firstOrNull { ext in it.exts }

        /** Các tab theo thứ tự hiển thị: (id, nhãn). */
        val TABS: List<Pair<String, String>> = listOf("gba" to "GBA", "gb" to "GB", "gbc" to "GBC", "nes" to "NES", "snes" to "SNES", "nds" to "NDS", "md" to "MD", "psx" to "PS1")
    }
}
