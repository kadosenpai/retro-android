package com.retro.app.data

/** Một cheat của một game. */
data class CheatItem(val id: String, val name: String, val code: String, val enabled: Boolean)

/**
 * Kiểm tra và chuẩn hoá mã cheat.
 * GBA: GameShark/Action Replay, mỗi dòng 12 hoặc 16 ký tự hex.
 * Game Boy/GBC: GameShark 8 ký tự hex (vd. 01FF16D1) hoặc Game Genie (XXX-XXX hoặc XXX-XXX-XXX).
 * SNES: Game Genie XXXX-XXXX (0-9, A-F) hoặc Pro Action Replay 8 ký tự hex (vd. 7E0DBE05).
 * NES: Game Genie 6 hoặc 8 chữ cái (APZLGITYEOXUKSVN), hoặc mã thô AAAA:VV / AAAA?CC:VV (hex).
 */
enum class CheatKind { GBA, GB, NES, SNES }

object CheatCodes {
    private val nesGenie = Regex("^[APZLGITYEOXUKSVN]{6}([APZLGITYEOXUKSVN]{2})?$")
    private val nesRaw = Regex("^[0-9A-F]{4}(\\?[0-9A-F]{2})?:[0-9A-F]{2}$")

    private val snesGenie = Regex("^[0-9A-F]{4}-[0-9A-F]{4}$")
    private val snesPar = Regex("^[0-9A-F]{8}$")

    private val hex = Regex("^[0-9A-F]+$")
    private val gbGameShark = Regex("^[0-9A-F]{8}$")
    private val gbGenie = Regex("^[0-9A-F]{3}-[0-9A-F]{3}(-[0-9A-F]{3})?$")

    /** Trả về lỗi, hoặc null nếu hợp lệ. */
    fun validate(code: String, kind: CheatKind): String? = when (kind) {
        CheatKind.GB -> validateGb(code)
        CheatKind.NES -> validateNes(code)
        CheatKind.SNES -> validateSnes(code)
        CheatKind.GBA -> validateGba(code)
    }

    /** Dạng đưa vào lõi: viết hoa, các mã ngăn bằng dấu "+". */
    fun normalize(code: String, kind: CheatKind): String = when (kind) {
        CheatKind.GB -> gbTokens(code).joinToString("+")
        CheatKind.NES -> nesTokens(code).joinToString("+")
        CheatKind.SNES -> snesTokens(code).joinToString("+")
        CheatKind.GBA -> code.uppercase().replace('-', ' ').split(Regex("[\\s+]+")).filter { it.isNotEmpty() }.joinToString("+")
    }

    private fun nesTokens(code: String): List<String> =
        code.uppercase().split(Regex("[\\s+,;]+")).filter { it.isNotEmpty() }

    private fun validateNes(code: String): String? {
        val tokens = nesTokens(code)
        if (tokens.isEmpty()) return "Chưa nhập mã"
        for (t in tokens) {
            if (!nesGenie.matches(t) && !nesRaw.matches(t)) {
                return "Mã NES: Game Genie 6 hoặc 8 chữ cái (vd. SXIOPO) hoặc mã thô dạng 0075:09"
            }
        }
        return null
    }

    /** Tách thành từng mã; Game Genie viết liền 8 ký tự hex thì giữ nguyên là mã PAR (hai loại không phân biệt được khi viết liền, nên Game Genie phải có dấu gạch). */
    private fun snesTokens(code: String): List<String> =
        code.uppercase().split(Regex("[\\s+,;]+")).filter { it.isNotEmpty() }

    private fun validateSnes(code: String): String? {
        val tokens = snesTokens(code)
        if (tokens.isEmpty()) return "Chưa nhập mã"
        for (t in tokens) {
            if (!snesGenie.matches(t) && !snesPar.matches(t)) {
                return "Mã SNES: Game Genie XXXX-XXXX (có dấu gạch) hoặc Pro Action Replay 8 ký tự (vd. 7E0DBE05)"
            }
        }
        return null
    }

    private fun validateGba(code: String): String? {
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

    /** Tách thành từng mã; Game Genie viết liền 6 hoặc 9 ký tự thì tự thêm dấu gạch. */
    private fun gbTokens(code: String): List<String> =
        code.uppercase().split(Regex("[\\s+,]+")).filter { it.isNotEmpty() }.map { t ->
            when {
                hex.matches(t) && t.length == 6 -> t.substring(0, 3) + "-" + t.substring(3)
                hex.matches(t) && t.length == 9 -> t.substring(0, 3) + "-" + t.substring(3, 6) + "-" + t.substring(6)
                else -> t
            }
        }

    private fun validateGb(code: String): String? {
        val tokens = gbTokens(code)
        if (tokens.isEmpty()) return "Chưa nhập mã"
        for (t in tokens) {
            if (!gbGameShark.matches(t) && !gbGenie.matches(t)) {
                return "Mã Game Boy: GameShark 8 ký tự (vd. 01FF16D1) hoặc Game Genie (XXX-XXX-XXX)"
            }
        }
        return null
    }
}
