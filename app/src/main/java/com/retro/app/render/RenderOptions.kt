package com.retro.app.render

data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int)

/** Tỉ lệ khung hình ép cho hệ đang chơi (PlayStation, Mega Drive...); 0 = theo kích thước khung của lõi. */
object DisplayAspect {
    @Volatile var value: Float = 0f
}

/**
 * Vùng hiển thị căn giữa. Mặc định giữ tỉ lệ gốc và phóng nguyên số khi có thể; có [aspect] thì ép đúng tỉ lệ đó
 * (khung có thể rộng 256/320/512/640 điểm nhưng vẫn hiển thị 4:3), phóng theo chiều cao.
 */
fun computeViewport(canvasW: Int, canvasH: Int, srcW: Int, srcH: Int, aspect: Float = 0f): Viewport {
    if (canvasW <= 0 || canvasH <= 0 || srcW <= 0 || srcH <= 0) return Viewport(0, 0, 0, 0)
    val dispW = if (aspect > 0f) srcH * aspect.toDouble() else srcW.toDouble()
    var scale = minOf(canvasH.toDouble() / srcH, canvasW.toDouble() / dispW)
    if (scale >= 1.0) scale = Math.floor(scale)
    val w = maxOf(1, Math.round(dispW * scale).toInt())
    val h = maxOf(1, Math.round(srcH * scale).toInt())
    return Viewport((canvasW - w) / 2, (canvasH - h) / 2, w, h)
}
