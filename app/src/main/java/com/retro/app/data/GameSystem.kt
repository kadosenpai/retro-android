package com.retro.app.data

/** Hệ máy được hỗ trợ. Mỗi hệ có đuôi file ROM, tên DAT/thư mục ảnh bìa của libretro và tab hiển thị trong thư viện. */
enum class GameSystem(
    val id: String,
    val label: String,
    /** Id của tab trong thư viện (GB và GBC chung một tab). */
    val tab: String,
    val exts: Set<String>,
    val datName: String,
    val thumbDir: String,
    val minRomSize: Long,
    val hasCheats: Boolean,
) {
    GBA("gba", "GBA", "gba", setOf("gba", "agb", "bin"), "Nintendo - Game Boy Advance", "Nintendo_-_Game_Boy_Advance", 0x200, true),
    GB("gb", "Game Boy", "gb", setOf("gb"), "Nintendo - Game Boy", "Nintendo_-_Game_Boy", 0x150, false),
    GBC("gbc", "Game Boy Color", "gb", setOf("gbc", "cgb"), "Nintendo - Game Boy Color", "Nintendo_-_Game_Boy_Color", 0x150, false);

    companion object {
        fun fromId(id: String?): GameSystem = values().firstOrNull { it.id == id } ?: GBA

        fun fromExt(ext: String): GameSystem? = values().firstOrNull { ext in it.exts }

        /** Các tab theo thứ tự hiển thị: (id, nhãn). */
        val TABS: List<Pair<String, String>> = listOf("gba" to "GBA", "gb" to "GB / GBC")
    }
}
