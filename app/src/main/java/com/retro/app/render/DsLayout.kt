package com.retro.app.render

/**
 * Một ô hiển thị của NDS: lấy dải dọc [v0]..[v1] của khung hình (0 = đầu khung) rồi vẽ vào hình chữ nhật
 * [x0]..[x1], [y0]..[y1] (tính theo tỉ lệ của toàn bộ bề mặt, y từ trên xuống).
 */
class DsQuad(
    val v0: Float, val v1: Float,
    val x0: Float, val y0: Float, val x1: Float, val y1: Float,
)

/** Cách bố trí hai màn hình NDS do skin đặt; null = hệ thường (một màn hình, tự căn giữa). */
object DsLayout {
    @Volatile
    var quads: List<DsQuad>? = null
}
