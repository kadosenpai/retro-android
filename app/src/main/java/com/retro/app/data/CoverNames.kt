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

    /**
     * Khoá so sánh gần đúng giữa tên file ROM và tên chuẩn: bỏ phần trong ngoặc ( ) [ ], bỏ dấu, chữ thường,
     * chỉ giữ chữ-số, bỏ mạo từ "the". Vd. "Legend of Zelda, The - Link's Awakening DX (USA)" và
     * "Legend of Zelda The Link's Awakening DX [!]" cùng ra "legend of zelda link s awakening dx".
     */
    fun normalizeKey(name: String): String {
        var s = name.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), " ")
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("""\p{M}+"""), "")
        return s.lowercase().split(Regex("""[^a-z0-9]+""")).filter { it.isNotEmpty() && it != "the" }.joinToString(" ")
    }

    private fun candidateRank(n: String): Int {
        var r = when {
            "(World" in n -> 0
            "(USA" in n -> 1
            "(Europe" in n -> 2
            "(Japan" in n -> 3
            else -> 4
        }
        if (Regex("""\((Beta|Proto|Demo|Sample|Unl|Pirate|Hack)""").containsMatchIn(n)) r += 10
        return r
    }

    /** Dựng chỉ mục khoá gần đúng -> các tên chuẩn (đã xếp theo vùng: World, USA, Europe, Japan...). */
    fun buildKeyIndex(names: Collection<String>): Map<String, List<String>> {
        val idx = HashMap<String, MutableList<String>>(names.size * 2)
        for (n in names.toSet()) idx.getOrPut(normalizeKey(n)) { ArrayList() }.add(n)
        for (list in idx.values) list.sortWith(compareBy({ candidateRank(it) }, { it.length }))
        return idx
    }

    /** Các tên chuẩn gần đúng với [rawNames] (tối đa [limit]). */
    fun fuzzyCandidates(index: Map<String, List<String>>, rawNames: List<String>, limit: Int = 3): List<String> {
        val out = LinkedHashSet<String>()
        for (raw in rawNames) {
            val key = normalizeKey(raw)
            if (key.isEmpty()) continue
            index[key]?.let { out.addAll(it) }
        }
        return out.take(limit)
    }

    /** libretro đổi các ký tự không hợp lệ trong tên file thành "_". */
    fun thumbFileName(name: String): String = name.replace(Regex("""[&*/:`<>?\\|]"""), "_")

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun boxArtUrl(sys: GameSystem, gameName: String): String =
        "https://raw.githubusercontent.com/libretro-thumbnails/${sys.thumbDir}/master/Named_Boxarts/${enc(thumbFileName(gameName))}.png"
}
