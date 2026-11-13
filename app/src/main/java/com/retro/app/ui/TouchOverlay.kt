package com.retro.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.retro.app.core.Pad
import kotlin.math.sqrt

private enum class Ctl { A, B, L, R, SELECT, START, MENU }

private class Layout(
    val land: Boolean,
    val stickC: Offset,
    val stickR: Float,
    val knobR: Float,
    val a: Offset,
    val b: Offset,
    val btnR: Float,
    val l: Rect,
    val r: Rect,
    val select: Rect,
    val start: Rect,
    val menu: Offset,
    val menuR: Float,
)

private fun makeLayout(w: Float, h: Float): Layout {
    val land = w > h * 1.2f
    return if (!land) {
        Layout(
            land = false,
            stickC = Offset(0.25f * w, 0.50f * h),
            stickR = 0.215f * w,
            knobR = 0.082f * w,
            a = Offset(0.87f * w, 0.33f * h),
            b = Offset(0.70f * w, 0.53f * h),
            btnR = 0.098f * w,
            l = Rect(0.035f * w, 0.00f * h, 0.035f * w + 0.30f * w, 0.00f * h + 0.13f * h),
            r = Rect(0.665f * w, 0.00f * h, 0.665f * w + 0.30f * w, 0.00f * h + 0.13f * h),
            select = Rect(0.19f * w, 0.78f * h, 0.19f * w + 0.235f * w, 0.78f * h + 0.105f * h),
            start = Rect(0.575f * w, 0.78f * h, 0.575f * w + 0.235f * w, 0.78f * h + 0.105f * h),
            menu = Offset(0.5f * w, 0.83f * h),
            menuR = 0.052f * w,
        )
    } else {
        val sr = 0.27f * h
        Layout(
            land = true,
            stickC = Offset(0.04f * w + sr + 0.02f * h, 0.62f * h),
            stickR = sr,
            knobR = 0.4f * sr,
            a = Offset(0.93f * w - 0.06f * h, 0.50f * h),
            b = Offset(0.93f * w - 0.06f * h - 0.17f * h - 0.04f * w, 0.68f * h),
            btnR = 0.115f * h,
            l = Rect(0.03f * w, 0.05f * h, 0.03f * w + 0.15f * w, 0.05f * h + 0.15f * h),
            r = Rect(0.82f * w, 0.05f * h, 0.82f * w + 0.15f * w, 0.05f * h + 0.15f * h),
            select = Rect(0.36f * w, 0.86f * h, 0.36f * w + 0.11f * w, 0.86f * h + 0.11f * h),
            start = Rect(0.53f * w, 0.86f * h, 0.53f * w + 0.11f * w, 0.86f * h + 0.11f * h),
            menu = Offset(0.495f * w + 0.0f * w, 0.915f * h),
            menuR = 0.05f * h,
        )
    }
}

private fun len(x: Float, y: Float): Float = sqrt(x * x + y * y)

private fun dist(a: Offset, b: Offset): Float = len(a.x - b.x, a.y - b.y)

/** Điều khiển nào đang nằm dưới ngón tay (null nếu không có). Chọn cái gần tâm nhất. */
private fun hitTest(lay: Layout, p: Offset): Ctl? {
    var best: Ctl? = null
    var bestD = Float.MAX_VALUE
    fun consider(c: Ctl, d: Float, ok: Boolean) {
        if (ok && d < bestD) {
            bestD = d
            best = c
        }
    }
    consider(Ctl.A, dist(p, lay.a), dist(p, lay.a) <= lay.btnR * 1.2f)
    consider(Ctl.B, dist(p, lay.b), dist(p, lay.b) <= lay.btnR * 1.2f)
    consider(Ctl.MENU, dist(p, lay.menu), dist(p, lay.menu) <= lay.menuR * 1.3f)
    val slop = lay.btnR * 0.25f
    fun rectHit(c: Ctl, r: Rect) = consider(
        c,
        dist(p, r.center),
        p.x >= r.left - slop && p.x <= r.right + slop && p.y >= r.top - slop && p.y <= r.bottom + slop,
    )
    rectHit(Ctl.L, lay.l)
    rectHit(Ctl.R, lay.r)
    rectHit(Ctl.SELECT, lay.select)
    rectHit(Ctl.START, lay.start)
    return best
}

private fun stickBits(lay: Layout, p: Offset): Int {
    val dx = p.x - lay.stickC.x
    val dy = p.y - lay.stickC.y
    val d = len(dx, dy)
    if (d < lay.stickR * 0.22f) return 0
    var m = 0
    if (dx > 0.38f * d) m = m or Pad.RIGHT
    if (dx < -0.38f * d) m = m or Pad.LEFT
    if (dy > 0.38f * d) m = m or Pad.DOWN
    if (dy < -0.38f * d) m = m or Pad.UP
    return m
}

private fun bitOf(c: Ctl): Int = when (c) {
    Ctl.A -> Pad.A
    Ctl.B -> Pad.B
    Ctl.L -> Pad.L
    Ctl.R -> Pad.R
    Ctl.SELECT -> Pad.SELECT
    Ctl.START -> Pad.START
    Ctl.MENU -> 0
}

/**
 * Nút ảo cảm ứng đa điểm. Vẽ bằng Canvas, tự xử lý mọi ngón tay: cần analog kéo được,
 * trượt ngón qua lại giữa các nút vẫn nhận, nhả ngón trên ▲ thì mở menu.
 */
@Composable
fun TouchOverlay(
    haptics: Boolean,
    onMask: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val measurer = rememberTextMeasurer()
    val hapticsNow by rememberUpdatedState(haptics)
    val onMaskNow by rememberUpdatedState(onMask)
    val onMenuNow by rememberUpdatedState(onMenu)

    var pressed by remember { mutableStateOf(emptySet<Ctl>()) }
    var knob by remember { mutableStateOf(Offset.Zero) } // độ lệch cần analog so với tâm

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val lay = remember(wPx, hPx) { makeLayout(wPx, hPx) }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(lay) {
                    val owner = HashMap<PointerId, Ctl?>()
                    var stickId: PointerId? = null
                    var lastMask = 0
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val nowPressed = HashSet<Ctl>()
                            var mask = 0
                            var knobOff = Offset.Zero
                            var menuReleased = false
                            for (ch in event.changes) {
                                val id = ch.id
                                if (!ch.pressed) {
                                    if (owner[id] == Ctl.MENU) menuReleased = true
                                    owner.remove(id)
                                    if (stickId == id) stickId = null
                                    ch.consume()
                                    continue
                                }
                                if (stickId == null && !ch.previousPressed &&
                                    dist(ch.position, lay.stickC) <= lay.stickR * 1.25f
                                ) {
                                    stickId = id
                                }
                                if (stickId == id) {
                                    mask = mask or stickBits(lay, ch.position)
                                    val v = ch.position - lay.stickC
                                    val d = len(v.x, v.y)
                                    val lim = lay.stickR - lay.knobR * 0.6f
                                    knobOff = if (d > lim && d > 0f) v * (lim / d) else v
                                    owner[id] = null
                                } else {
                                    val c = hitTest(lay, ch.position)
                                    owner[id] = c
                                    if (c != null) {
                                        nowPressed.add(c)
                                        mask = mask or bitOf(c)
                                    }
                                }
                                ch.consume()
                            }
                            if (mask != lastMask) {
                                val newBits = mask and lastMask.inv()
                                if (newBits != 0 && hapticsNow) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                lastMask = mask
                                onMaskNow(mask)
                            }
                            pressed = nowPressed
                            knob = knobOff
                            if (menuReleased) onMenuNow()
                        }
                    }
                },
        ) {
            drawPad(lay, pressed, knob, measurer)
        }
    }
}

private val btnTop = Color(0xFF2A2B30)
private val btnBottom = Color(0xFF0F1013)
private val btnTopOn = Color(0xFF4A4C54)
private val btnBottomOn = Color(0xFF2A2C32)
private val strokeColor = Color(0x40FFFFFF)

private fun DrawScope.fillBrush(r: Rect, on: Boolean) =
    Brush.verticalGradient(
        listOf(if (on) btnTopOn else btnTop, if (on) btnBottomOn else btnBottom),
        startY = r.top,
        endY = r.bottom,
    )

private fun DrawScope.circleButton(c: Offset, rad: Float, on: Boolean, label: String, m: androidx.compose.ui.text.TextMeasurer) {
    val r = Rect(c.x - rad, c.y - rad, c.x + rad, c.y + rad)
    drawCircle(fillBrush(r, on), radius = rad, center = c)
    drawCircle(strokeColor, radius = rad, center = c, style = Stroke(width = rad * 0.03f + 1.5f))
    centerText(m, label, c, (rad * 0.62f).toSp(), FontWeight.Bold)
}

private fun DrawScope.centerText(
    m: androidx.compose.ui.text.TextMeasurer,
    text: String,
    center: Offset,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
) {
    val layout = m.measure(text, TextStyle(color = Color.White, fontSize = size, fontWeight = weight))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

private fun DrawScope.drawPad(lay: Layout, pressed: Set<Ctl>, knob: Offset, m: androidx.compose.ui.text.TextMeasurer) {
    // Cần analog
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF17181B), Color(0xFF0B0C0E)),
            center = lay.stickC,
            radius = lay.stickR,
        ),
        radius = lay.stickR,
        center = lay.stickC,
    )
    drawCircle(Color(0x33FFFFFF), radius = lay.stickR, center = lay.stickC, style = Stroke(width = 3f))
    val kc = lay.stickC + knob
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFC4C4C9), Color(0xFF6F6F76)),
            center = Offset(kc.x - lay.knobR * 0.25f, kc.y - lay.knobR * 0.3f),
            radius = lay.knobR * 1.4f,
        ),
        radius = lay.knobR,
        center = kc,
    )
    circleButton(lay.a, lay.btnR, Ctl.A in pressed, "A", m)
    circleButton(lay.b, lay.btnR, Ctl.B in pressed, "B", m)

    shoulder(lay.l, left = true, on = Ctl.L in pressed, label = "L", m = m)
    shoulder(lay.r, left = false, on = Ctl.R in pressed, label = "R", m = m)
    pill(lay.select, Ctl.SELECT in pressed, "SELECT", m)
    pill(lay.start, Ctl.START in pressed, "START", m)

    // Nút ▲ (menu)
    val mr = lay.menuR
    val mRect = Rect(lay.menu.x - mr, lay.menu.y - mr, lay.menu.x + mr, lay.menu.y + mr)
    val on = Ctl.MENU in pressed
    drawCircle(fillBrush(mRect, on), radius = mr, center = lay.menu)
    drawCircle(strokeColor, radius = mr, center = lay.menu, style = Stroke(width = 2.5f))
    val tri = Path().apply {
        val s = mr * 0.42f
        moveTo(lay.menu.x, lay.menu.y - s)
        lineTo(lay.menu.x + s, lay.menu.y + s * 0.7f)
        lineTo(lay.menu.x - s, lay.menu.y + s * 0.7f)
        close()
    }
    drawPath(tri, Color.White)
}

private fun DrawScope.pill(r: Rect, on: Boolean, label: String, m: androidx.compose.ui.text.TextMeasurer) {
    val cr = CornerRadius(r.height / 2f, r.height / 2f)
    drawRoundRect(fillBrush(r, on), topLeft = r.topLeft, size = Size(r.width, r.height), cornerRadius = cr)
    drawRoundRect(strokeColor, topLeft = r.topLeft, size = Size(r.width, r.height), cornerRadius = cr, style = Stroke(width = 2.5f))
    centerText(m, label, r.center, (r.height * 0.27f).toSp(), FontWeight.ExtraBold)
}

private fun DrawScope.shoulder(r: Rect, left: Boolean, on: Boolean, label: String, m: androidx.compose.ui.text.TextMeasurer) {
    // Hình bình hành nghiêng, góc ngoài bo tròn.
    val slant = r.height * 0.35f
    val path = Path().apply {
        if (left) {
            moveTo(r.left + slant, r.top)
            lineTo(r.right, r.top)
            lineTo(r.right - slant, r.bottom)
            lineTo(r.left, r.bottom)
        } else {
            moveTo(r.left, r.top)
            lineTo(r.right - slant, r.top)
            lineTo(r.right, r.bottom)
            lineTo(r.left + slant, r.bottom)
        }
        close()
    }
    drawPath(path, fillBrush(r, on))
    drawPath(path, strokeColor, style = Stroke(width = 2.5f))
    centerText(m, label, r.center, (r.height * 0.3f).toSp(), FontWeight.Bold)
}
