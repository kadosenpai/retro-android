package com.retro.app.render

import com.retro.app.data.DisplayFilter
import com.retro.app.data.ScaleMode

data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int)

/** Tỉ lệ khung hình ép cho hệ đang chơi (PlayStation, Mega Drive...); 0 = theo kích thước khung của lõi. */
object DisplayAspect {
    @Volatile var value: Float = 0f
}

/** Tuỳ chọn hiển thị mà luồng vẽ đọc mỗi khung; PlayScreen cập nhật từ Cài đặt. */
object RenderPrefs {
    @Volatile var filter: DisplayFilter = DisplayFilter.SHARP
    @Volatile var level: Float = 0.6f
    @Volatile var scaleMode: ScaleMode = ScaleMode.INTEGER
}

/**
 * Vùng hiển thị căn giữa. Giữ tỉ lệ gốc; có [aspect] thì ép đúng tỉ lệ đó
 * (khung có thể rộng 256/320/512/640 điểm nhưng vẫn hiển thị 4:3), phóng theo chiều cao.
 * [mode]: INTEGER phóng nguyên số khi có thể, FIT phóng lớn nhất (có thể lẻ), SMALLER nguyên số thấp hơn một bậc.
 */
fun computeViewport(
    canvasW: Int,
    canvasH: Int,
    srcW: Int,
    srcH: Int,
    aspect: Float = 0f,
    mode: ScaleMode = ScaleMode.INTEGER,
): Viewport {
    if (canvasW <= 0 || canvasH <= 0 || srcW <= 0 || srcH <= 0) return Viewport(0, 0, 0, 0)
    val dispW = if (aspect > 0f) srcH * aspect.toDouble() else srcW.toDouble()
    var scale = minOf(canvasH.toDouble() / srcH, canvasW.toDouble() / dispW)
    if (scale >= 1.0) {
        when (mode) {
            ScaleMode.FIT -> Unit
            ScaleMode.INTEGER -> scale = Math.floor(scale)
            ScaleMode.SMALLER -> scale = maxOf(1.0, Math.floor(scale) - 1.0)
        }
    }
    val w = maxOf(1, Math.round(dispW * scale).toInt())
    val h = maxOf(1, Math.round(srcH * scale).toInt())
    return Viewport((canvasW - w) / 2, (canvasH - h) / 2, w, h)
}
