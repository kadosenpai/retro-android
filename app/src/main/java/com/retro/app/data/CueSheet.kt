package com.retro.app.data

/** Đọc bảng mục lục đĩa (.cue): lấy tên các file track mà nó tham chiếu. */
object CueSheet {
    private val quoted = Regex("""^\s*FILE\s+"([^"]+)"""", RegexOption.IGNORE_CASE)
    private val bare = Regex("""^\s*FILE\s+(\S+)""", RegexOption.IGNORE_CASE)

    /** Có phải nội dung dạng .cue không (văn bản bắt đầu bằng một lệnh quen thuộc, không có byte 0). */
    fun looksLikeCue(head: ByteArray, n: Int): Boolean {
        if (n <= 0) return false
        for (i in 0 until n) if (head[i].toInt() == 0) return false
        val t = String(head, 0, n, Charsets.ISO_8859_1).trimStart('﻿', 'ï', '»', '¿', ' ', '\t', '\r', '\n').uppercase()
        return listOf("FILE", "REM", "CATALOG", "TITLE", "PERFORMER").any { t.startsWith(it) }
    }

    /** Tên (không kèm thư mục) các file track, không trùng nhau, theo thứ tự xuất hiện. */
    fun files(text: String): List<String> {
        val out = LinkedHashSet<String>()
        for (line in text.lineSequence()) {
            val m = quoted.find(line) ?: bare.find(line) ?: continue
            val name = m.groupValues[1].substringAfterLast('/').substringAfterLast('\\')
            if (name.isNotEmpty()) out.add(name)
        }
        return out.toList()
    }
}
