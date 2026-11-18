package com.retro.app.data

/** Lõi giả lập (libretro) đứng sau mỗi hệ máy; file lõi là `lib<id>_libretro.so`. */
enum class Core(val id: String) {
    MGBA("mgba"),
    FCEUMM("fceumm"),
    SNES9X("snes9x");

    val libName: String get() = "lib${id}_libretro.so"
}

/** Hệ máy được hỗ trợ. Mỗi hệ có đuôi file ROM, tên DAT/thư mục ảnh bìa của libretro và tab hiển thị trong thư viện. */
enum class GameSystem(
    val id: String,
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
) {
    GBA("gba", "GBA", "gba", setOf("gba", "agb", "bin"), "Nintendo - Game Boy Advance", "Nintendo_-_Game_Boy_Advance", 0x200, Core.MGBA, CheatKind.GBA),
    GB("gb", "Game Boy", "gb", setOf("gb"), "Nintendo - Game Boy", "Nintendo_-_Game_Boy", 0x150, Core.MGBA, CheatKind.GB),
    GBC("gbc", "Game Boy Color", "gbc", setOf("gbc", "cgb"), "Nintendo - Game Boy Color", "Nintendo_-_Game_Boy_Color", 0x150, Core.MGBA, CheatKind.GB),
    NES("nes", "NES", "nes", setOf("nes", "unf", "unif"), "Nintendo - Nintendo Entertainment System", "Nintendo_-_Nintendo_Entertainment_System", 0x2010, Core.FCEUMM, CheatKind.NES),
    SNES("snes", "SNES", "snes", setOf("sfc", "smc", "swc", "fig"), "Nintendo - Super Nintendo Entertainment System", "Nintendo_-_Super_Nintendo_Entertainment_System", 0x8000, Core.SNES9X, CheatKind.SNES);

    companion object {
        fun fromId(id: String?): GameSystem = values().firstOrNull { it.id == id } ?: GBA

        fun fromExt(ext: String): GameSystem? = values().firstOrNull { ext in it.exts }

        /** Các tab theo thứ tự hiển thị: (id, nhãn). */
        val TABS: List<Pair<String, String>> = listOf("gba" to "GBA", "gb" to "GB", "gbc" to "GBC", "nes" to "NES", "snes" to "SNES")
    }
}
