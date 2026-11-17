package com.retro.app.data

/** Chuẩn hoá chữ trong game trước khi dịch và kiểm tra bản dịch không làm hỏng mã điều khiển. */
object TranslateText {
    private val tokenRe = Regex("""\{[^}]*\}|\\p""")
    private val lineRe = Regex("""\\[nl]""")
    private val spacesRe = Regex(" {2,}")
    private val letterRe = Regex("[A-Za-z]{2}")

    /**
     * Dạng chuẩn của một đoạn chữ: bỏ xuống dòng cứng (\n, \l) vì tiếng Việt sẽ tự ngắt dòng,
     * giữ \p (sang trang) và các mã {…}. Dùng làm khoá tra bản dịch.
     */
    fun normalize(src: String): String = src.replace(lineRe, " ").replace(spacesRe, " ").trim()

    /** Đoạn chữ có đáng dịch không (có ít nhất hai chữ cái liền nhau). */
    fun worthTranslating(normalized: String): Boolean = letterRe.containsMatchIn(normalized)

    private fun tokens(s: String): List<String> = tokenRe.findAll(s).map { it.value }.toList()

    /** Bản dịch hợp lệ khi có chữ và giữ nguyên đúng các mã {…} và số lần \p của bản gốc. */
    fun isValid(src: String, out: String): Boolean {
        if (out.isBlank()) return false
        return tokens(src).sorted() == tokens(out).sorted()
    }
}
