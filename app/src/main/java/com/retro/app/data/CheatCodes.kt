package com.retro.app.data

/** Một cheat của một game. */
data class CheatItem(val id: String, val name: String, val code: String, val enabled: Boolean)

/** Kiểm tra và chuẩn hoá mã cheat GBA (GameShark/Action Replay…); logic thuần Kotlin, giống bản web. */
object CheatCodes {
    private val hex = Regex("^[0-9A-F]+$")

    /** Mỗi dòng 12 hoặc 16 ký tự hex (có thể cách bằng khoảng trắng hoặc gạch). Trả về lỗi, hoặc null nếu hợp lệ. */
    fun validate(code: String): String? {
        val lines = code.uppercase()
            .split(Regex("[\n+]+"))
            .map { it.replace(Regex("[\\s-]+"), "") }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "Chưa nhập mã"
        for (l in lines) {
            if (!hex.matches(l)) return "Mã chỉ gồm chữ số 0-9 và chữ A-F"
            if (l.length != 12 && l.length != 16) return "Mỗi dòng mã phải đủ 12 hoặc 16 ký tự (tính cả hai nhóm)"
        }
        return null
    }

    /** Dạng đưa vào lõi: viết hoa, các nhóm ngăn bằng dấu "+". */
    fun normalize(code: String): String =
        code.uppercase().replace('-', ' ').split(Regex("[\\s+]+")).filter { it.isNotEmpty() }.joinToString("+")
}
