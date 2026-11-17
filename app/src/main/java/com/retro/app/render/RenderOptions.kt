package com.retro.app.render

data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int)

/** Vùng hiển thị giữ tỉ lệ gốc, phóng nguyên số khi có thể; luôn căn giữa. */
fun computeViewport(canvasW: Int, canvasH: Int, srcW: Int, srcH: Int): Viewport {
    if (canvasW <= 0 || canvasH <= 0 || srcW <= 0 || srcH <= 0) return Viewport(0, 0, 0, 0)
    var scale = minOf(canvasH.toDouble() / srcH, canvasW.toDouble() / srcW)
    if (scale >= 1.0) scale = Math.floor(scale)
    val w = maxOf(1, Math.round(srcW * scale).toInt())
    val h = maxOf(1, Math.round(srcH * scale).toInt())
    return Viewport((canvasW - w) / 2, (canvasH - h) / 2, w, h)
}
