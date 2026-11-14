package com.retro.app.data

/** Các điều khiển của nút ảo có thể kéo và đổi kích thước. */
enum class PadControl(val label: String) {
    STICK("Cần analog"),
    A("Nút A"),
    B("Nút B"),
    L("Nút L"),
    R("Nút R"),
    SELECT("Select"),
    START("Start"),
    MENU("Nút ▲"),
}

/** Vị trí tâm (tỉ lệ 0..1 của vùng nút ảo, null = vị trí mặc định) và hệ số kích thước. */
data class ControlPos(val cx: Float? = null, val cy: Float? = null, val scale: Float = 1f)

/** Bố cục nút ảo do người dùng chỉnh (riêng cho màn dọc và màn ngang). Logic thuần Kotlin. */
class PadLayout(val map: Map<PadControl, ControlPos> = emptyMap()) {
    operator fun get(c: PadControl): ControlPos? = map[c]

    fun scaleOf(c: PadControl): Float = map[c]?.scale ?: 1f

    fun moved(c: PadControl, cx: Float, cy: Float): PadLayout {
        val cur = map[c] ?: ControlPos()
        return PadLayout(map + (c to cur.copy(cx = clampPos(cx), cy = clampPos(cy))))
    }

    /** Đổi kích thước thêm [delta] (bước 0,1), giới hạn 0,5..2. */
    fun scaled(c: PadControl, delta: Float): PadLayout {
        val cur = map[c] ?: ControlPos()
        val s = Math.round((cur.scale + delta) * 10f) / 10f
        return PadLayout(map + (c to cur.copy(scale = s.coerceIn(MIN_SCALE, MAX_SCALE))))
    }

    fun encode(): String = map.entries.joinToString(";") { (c, p) ->
        "${c.name}:${p.cx?.let { fmt(it) } ?: ""},${p.cy?.let { fmt(it) } ?: ""},${fmt(p.scale)}"
    }

    private fun fmt(v: Float): String = String.format(java.util.Locale.US, "%.4f", v)

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2f
        private const val MARGIN = 0.03f

        fun clampPos(v: Float): Float = v.coerceIn(MARGIN, 1f - MARGIN)

        fun decode(s: String?): PadLayout {
            if (s.isNullOrBlank()) return PadLayout()
            val out = LinkedHashMap<PadControl, ControlPos>()
            for (part in s.split(';')) {
                val colon = part.indexOf(':')
                if (colon <= 0) continue
                val c = PadControl.values().firstOrNull { it.name == part.substring(0, colon) } ?: continue
                val f = part.substring(colon + 1).split(',')
                if (f.size != 3) continue
                val cx = f[0].toFloatOrNull()?.takeIf { it.isFinite() }?.let { clampPos(it) }
                val cy = f[1].toFloatOrNull()?.takeIf { it.isFinite() }?.let { clampPos(it) }
                val sc = f[2].toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(MIN_SCALE, MAX_SCALE) ?: 1f
                out[c] = ControlPos(cx, cy, sc)
            }
            return PadLayout(out)
        }
    }
}
