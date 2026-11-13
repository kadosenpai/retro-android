package com.retro.app.render

enum class ScaleMode(val label: String) {
    INTEGER("Mặc định"),
    FIT("Vừa"),
    FILL("Đầy"),
    WIDE("4:3"),
}

enum class FilterMode(val label: String, val code: Float) {
    SHARP("Thô", 0f),
    CRISP("Nét", 2f),
    SMOOTH("Mịn", 1f),
    LCD("LCD", 3f),
    CRT("CRT", 4f),
}

data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int)

/** Tính vùng hiển thị trong màn hình theo chế độ co giãn; luôn căn giữa. */
fun computeViewport(canvasW: Int, canvasH: Int, srcW: Int, srcH: Int, mode: ScaleMode): Viewport {
    if (canvasW <= 0 || canvasH <= 0 || srcW <= 0 || srcH <= 0) return Viewport(0, 0, 0, 0)
    if (mode == ScaleMode.FILL) return Viewport(0, 0, canvasW, canvasH)
    val aspect = if (mode == ScaleMode.WIDE) 4.0 / 3.0 else srcW.toDouble() / srcH
    var scale = minOf(canvasH.toDouble() / srcH, canvasW / (srcH * aspect))
    if (mode == ScaleMode.INTEGER && scale >= 1.0) scale = Math.floor(scale)
    val w = maxOf(1, Math.round(srcH * aspect * scale).toInt())
    val h = maxOf(1, Math.round(srcH * scale).toInt())
    return Viewport((canvasW - w) / 2, (canvasH - h) / 2, w, h)
}
