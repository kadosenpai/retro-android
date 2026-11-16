package com.retro.app.data

import java.net.URLEncoder

/** Phần thuần Kotlin (không phụ thuộc Android) của việc tìm ảnh bìa: đọc DAT libretro và dựng URL. */
object CoverNames {
    fun datUrl(sys: GameSystem): String =
        "https://raw.githubusercontent.com/libretro/libretro-database/master/dat/${enc(sys.datName)}.dat"

    private val nameRe = Regex("""name\s+"([^"]+)"""")
    private val crcRe = Regex("""\bcrc\s+([0-9A-Fa-f]{8})\b""")
    private val gameStart = Regex("""(?m)^\s*game\s*\(""")

    /** Đọc file DAT dạng clrmamepro: trả về bảng CRC32 của ROM -> tên game chuẩn (No-Intro). */
    fun parseDat(text: String): Map<Long, String> {
        val map = HashMap<Long, String>(8192)
        val blocks = gameStart.split(text)
        for (i in 1 until blocks.size) {
            val b = blocks[i]
            val name = nameRe.find(b)?.groupValues?.get(1) ?: continue
            for (m in crcRe.findAll(b)) {
                val crc = m.groupValues[1].toLong(16)
                if (!map.containsKey(crc)) map[crc] = name
            }
        }
        return map
    }

    /** libretro đổi các ký tự không hợp lệ trong tên file thành "_". */
    fun thumbFileName(name: String): String = name.replace(Regex("""[&*/:`<>?\\|]"""), "_")

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun boxArtUrl(sys: GameSystem, gameName: String): String =
        "https://raw.githubusercontent.com/libretro-thumbnails/${sys.thumbDir}/master/Named_Boxarts/${enc(thumbFileName(gameName))}.png"
}
