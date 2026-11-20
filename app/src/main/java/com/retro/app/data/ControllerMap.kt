package com.retro.app.data

import com.retro.app.core.Pad

/** Các nút của GBA mà tay cầm có thể gán. */
enum class GbaButton(val label: String, val bit: Int) {
    UP("Lên", Pad.UP),
    DOWN("Xuống", Pad.DOWN),
    LEFT("Trái", Pad.LEFT),
    RIGHT("Phải", Pad.RIGHT),
    A("A", Pad.A),
    B("B", Pad.B),
    X("X (SNES)", Pad.X),
    Y("Y (SNES)", Pad.Y),
    L("L", Pad.L),
    R("R", Pad.R),
    L2("L2 (PS1)", Pad.L2),
    R2("R2 (PS1)", Pad.R2),
    L3("L3 (PS1)", Pad.L3),
    R3("R3 (PS1)", Pad.R3),
    START("Start", Pad.START),
    SELECT("Select", Pad.SELECT),
}

/**
 * Bảng gán nút tay cầm: mã phím Android -> nút GBA. Logic thuần Kotlin.
 * Mã phím mặc định (hằng số của android.view.KeyEvent):
 * DPAD_UP=19, DPAD_DOWN=20, DPAD_LEFT=21, DPAD_RIGHT=22, BUTTON_A=96, BUTTON_B=97,
 * BUTTON_L1=102, BUTTON_R1=103, BUTTON_L2=104, BUTTON_R2=105, THUMBL=106, THUMBR=107, BUTTON_START=108, BUTTON_SELECT=109.
 */
class ControllerMap(val keys: Map<GbaButton, Set<Int>>) {

    /** Tổng các bit GBA ứng với mã phím này (0 nếu chưa gán). */
    fun bitFor(code: Int): Int {
        var m = 0
        for ((b, ks) in keys) if (code in ks) m = m or b.bit
        return m
    }

    /** Gán [code] cho [button] (thay hết phím cũ của nút đó) và gỡ [code] khỏi các nút khác. */
    fun assign(button: GbaButton, code: Int): ControllerMap =
        ControllerMap(
            GbaButton.values().associateWith { b ->
                if (b == button) setOf(code) else (keys[b] ?: emptySet()) - code
            },
        )

    fun encode(): String =
        GbaButton.values().joinToString(";") { b -> b.name + "=" + (keys[b] ?: emptySet()).sorted().joinToString(",") }

    companion object {
        fun defaults(): ControllerMap = ControllerMap(
            mapOf(
                GbaButton.UP to setOf(19),
                GbaButton.DOWN to setOf(20),
                GbaButton.LEFT to setOf(21),
                GbaButton.RIGHT to setOf(22),
                // Nút dưới (A kiểu Xbox) = B của GBA, nút phải = A của GBA.
                GbaButton.A to setOf(97),
                GbaButton.B to setOf(96),
                // Bố cục Nintendo: nút trên (Y kiểu Xbox) = X, nút trái (X kiểu Xbox) = Y.
                GbaButton.X to setOf(100),
                GbaButton.Y to setOf(99),
                GbaButton.L to setOf(102),
                GbaButton.R to setOf(103),
                // Chỉ PlayStation dùng L2/R2/L3/R3; các hệ khác coi L2/R2 như L/R.
                GbaButton.L2 to setOf(104),
                GbaButton.R2 to setOf(105),
                GbaButton.L3 to setOf(106),
                GbaButton.R3 to setOf(107),
                GbaButton.START to setOf(108),
                GbaButton.SELECT to setOf(109),
            ),
        )

        /** Đọc từ chuỗi đã lưu; nút nào thiếu hoặc lỗi thì dùng mặc định. */
        fun decode(s: String?): ControllerMap {
            val def = defaults().keys
            if (s.isNullOrBlank()) return defaults()
            val parsed = HashMap<GbaButton, Set<Int>>()
            for (part in s.split(';')) {
                val eq = part.indexOf('=')
                if (eq <= 0) continue
                val b = GbaButton.values().firstOrNull { it.name == part.substring(0, eq) } ?: continue
                val raw = part.substring(eq + 1)
                if (raw.isBlank()) {
                    parsed[b] = emptySet() // nút được để trống có chủ ý
                } else {
                    val codes = raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                    if (codes.isNotEmpty()) parsed[b] = codes // chuỗi hỏng thì dùng mặc định
                }
            }
            // Bản lưu cũ (chưa có L2/R2): L2/R2 trước đây gộp vào L/R, nay tách riêng nên bỏ khỏi L/R.
            if (GbaButton.L2 !in parsed) {
                parsed[GbaButton.L]?.let { if (it.size > 1) parsed[GbaButton.L] = it - 104 }
                parsed[GbaButton.R]?.let { if (it.size > 1) parsed[GbaButton.R] = it - 105 }
            }
            return ControllerMap(GbaButton.values().associateWith { parsed[it] ?: def[it] ?: emptySet() })
        }
    }
}
