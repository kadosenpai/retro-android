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
import androidx.compose.ui.graphics.PathEffect
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
import com.retro.app.data.PadControl
import com.retro.app.data.PadLayout
import kotlin.math.max
import kotlin.math.sqrt

private class Layout(
    val land: Boolean,
    val stickC: Offset,
    val stickR: Float,
    val knobR: Float,
    val a: Offset,
    val aR: Float,
    val b: Offset,
    val bR: Float,
    val l: Rect,
    val r: Rect,
    val select: Rect,
    val start: Rect,
    val menu: Offset,
    val menuR: Float,
)

private fun makeLayout(w: Float, h: Float, ov: PadLayout): Layout {
    val land = w > h * 1.2f
    // Vị trí/kích thước mặc định.
    val stickC: Offset
    val stickR: Float
    val knobR: Float
    val a: Offset
    val b: Offset
    val btnR: Float
    val l: Rect
    val r: Rect
    val select: Rect
    val start: Rect
    val menu: Offset
    val menuR: Float
    if (!land) {
        stickC = Offset(0.25f * w, 0.50f * h)
        stickR = 0.215f * w
        knobR = 0.082f * w
        a = Offset(0.87f * w, 0.33f * h)
        b = Offset(0.70f * w, 0.53f * h)
        btnR = 0.098f * w
        l = Rect(0.035f * w, 0.00f * h, 0.035f * w + 0.30f * w, 0.00f * h + 0.13f * h)
        r = Rect(0.665f * w, 0.00f * h, 0.665f * w + 0.30f * w, 0.00f * h + 0.13f * h)
        select = Rect(0.19f * w, 0.78f * h, 0.19f * w + 0.235f * w, 0.78f * h + 0.105f * h)
        start = Rect(0.575f * w, 0.78f * h, 0.575f * w + 0.235f * w, 0.78f * h + 0.105f * h)
        menu = Offset(0.5f * w, 0.83f * h)
        menuR = 0.052f * w
    } else {
        val sr = 0.27f * h
        stickC = Offset(0.04f * w + sr + 0.02f * h, 0.62f * h)
        stickR = sr
        knobR = 0.4f * sr
        a = Offset(0.93f * w - 0.06f * h, 0.50f * h)
        b = Offset(0.93f * w - 0.06f * h - 0.17f * h - 0.04f * w, 0.68f * h)
        btnR = 0.115f * h
        l = Rect(0.03f * w, 0.05f * h, 0.03f * w + 0.15f * w, 0.05f * h + 0.15f * h)
        r = Rect(0.82f * w, 0.05f * h, 0.82f * w + 0.15f * w, 0.05f * h + 0.15f * h)
        select = Rect(0.36f * w, 0.86f * h, 0.36f * w + 0.11f * w, 0.86f * h + 0.11f * h)
        start = Rect(0.53f * w, 0.86f * h, 0.53f * w + 0.11f * w, 0.86f * h + 0.11f * h)
        menu = Offset(0.495f * w, 0.915f * h)
        menuR = 0.05f * h
    }

    // Áp bố cục người dùng đã chỉnh (vị trí tâm và hệ số kích thước).
    fun center(c: PadControl, base: Offset): Offset {
        val p = ov[c] ?: return base
        return Offset((p.cx ?: (base.x / w)) * w, (p.cy ?: (base.y / h)) * h)
    }

    fun rect(c: PadControl, base: Rect): Rect {
        val p = ov[c] ?: return base
        val ctr = Offset((p.cx ?: (base.center.x / w)) * w, (p.cy ?: (base.center.y / h)) * h)
        val hw = base.width * p.scale / 2f
        val hh = base.height * p.scale / 2f
        return Rect(ctr.x - hw, ctr.y - hh, ctr.x + hw, ctr.y + hh)
    }

    return Layout(
        land = land,
        stickC = center(PadControl.STICK, stickC),
        stickR = stickR * ov.scaleOf(PadControl.STICK),
        knobR = knobR * ov.scaleOf(PadControl.STICK),
        a = center(PadControl.A, a),
        aR = btnR * ov.scaleOf(PadControl.A),
        b = center(PadControl.B, b),
        bR = btnR * ov.scaleOf(PadControl.B),
        l = rect(PadControl.L, l),
        r = rect(PadControl.R, r),
        select = rect(PadControl.SELECT, select),
        start = rect(PadControl.START, start),
        menu = center(PadControl.MENU, menu),
        menuR = menuR * ov.scaleOf(PadControl.MENU),
    )
}

private fun len(x: Float, y: Float): Float = sqrt(x * x + y * y)

private fun dist(a: Offset, b: Offset): Float = len(a.x - b.x, a.y - b.y)

/** Điều khiển nào đang nằm dưới ngón tay (null nếu không có). Chọn cái gần tâm nhất. */
private fun hitTest(lay: Layout, p: Offset): PadControl? {
    var best: PadControl? = null
    var bestD = Float.MAX_VALUE
    fun consider(c: PadControl, d: Float, ok: Boolean) {
        if (ok && d < bestD) {
            bestD = d
            best = c
        }
    }
    consider(PadControl.A, dist(p, lay.a), dist(p, lay.a) <= lay.aR * 1.2f)
    consider(PadControl.B, dist(p, lay.b), dist(p, lay.b) <= lay.bR * 1.2f)
    consider(PadControl.MENU, dist(p, lay.menu), dist(p, lay.menu) <= lay.menuR * 1.3f)
    val slop = max(lay.aR, lay.bR) * 0.25f
    fun rectHit(c: PadControl, r: Rect) = consider(
        c,
        dist(p, r.center),
        p.x >= r.left - slop && p.x <= r.right + slop && p.y >= r.top - slop && p.y <= r.bottom + slop,
    )
    rectHit(PadControl.L, lay.l)
    rectHit(PadControl.R, lay.r)
    rectHit(PadControl.SELECT, lay.select)
    rectHit(PadControl.START, lay.start)
    return best
}

private fun controlCenter(lay: Layout, c: PadControl): Offset = when (c) {
    PadControl.STICK -> lay.stickC
    PadControl.A -> lay.a
    PadControl.B -> lay.b
    PadControl.L -> lay.l.center
    PadControl.R -> lay.r.center
    PadControl.SELECT -> lay.select.center
    PadControl.START -> lay.start.center
    PadControl.MENU -> lay.menu
}

/** Chế độ chỉnh: điều khiển nào nằm dưới ngón tay (gồm cả cần analog). */
private fun editHit(lay: Layout, p: Offset): PadControl? {
    var best: PadControl? = null
    var bestD = Float.MAX_VALUE
    for (c in PadControl.values()) {
        val ctr = controlCenter(lay, c)
        val d = dist(p, ctr)
        val ok = when (c) {
            PadControl.STICK -> d <= lay.stickR * 1.05f
            PadControl.A -> d <= lay.aR * 1.2f
            PadControl.B -> d <= lay.bR * 1.2f
            PadControl.MENU -> d <= lay.menuR * 1.4f
            PadControl.L -> inRect(lay.l, p)
            PadControl.R -> inRect(lay.r, p)
            PadControl.SELECT -> inRect(lay.select, p)
            PadControl.START -> inRect(lay.start, p)
        }
        if (ok && d < bestD) {
            bestD = d
            best = c
        }
    }
    return best
}

private fun inRect(r: Rect, p: Offset): Boolean {
    val slop = 12f
    return p.x >= r.left - slop && p.x <= r.right + slop && p.y >= r.top - slop && p.y <= r.bottom + slop
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

private fun bitOf(c: PadControl): Int = when (c) {
    PadControl.A -> Pad.A
    PadControl.B -> Pad.B
    PadControl.L -> Pad.L
    PadControl.R -> Pad.R
    PadControl.SELECT -> Pad.SELECT
    PadControl.START -> Pad.START
    PadControl.MENU, PadControl.STICK -> 0
}

/**
 * Nút ảo cảm ứng đa điểm. Vẽ bằng Canvas, tự xử lý mọi ngón tay: cần analog kéo được,
 * trượt ngón qua lại giữa các nút vẫn nhận, nhả ngón trên ▲ thì mở menu.
 * Ở chế độ [editing]: chạm chọn một điều khiển rồi kéo để di chuyển (đổi kích thước do thanh công cụ làm).
 */
@Composable
fun TouchOverlay(
    haptics: Boolean,
    layout: PadLayout,
    onMask: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    sticky: Boolean = false,
    editing: Boolean = false,
    selected: PadControl? = null,
    onSelect: (PadControl?) -> Unit = {},
    onLayoutChange: (PadLayout) -> Unit = {},
) {
    val view = LocalView.current
    val measurer = rememberTextMeasurer()
    val hapticsNow by rememberUpdatedState(haptics)
    val onMaskNow by rememberUpdatedState(onMask)
    val onMenuNow by rememberUpdatedState(onMenu)
    val layoutNow by rememberUpdatedState(layout)
    val onSelectNow by rememberUpdatedState(onSelect)
    val onLayoutNow by rememberUpdatedState(onLayoutChange)

    var pressed by remember { mutableStateOf(emptySet<PadControl>()) }
    var knob by remember { mutableStateOf(Offset.Zero) } // độ lệch cần analog so với tâm

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val lay = remember(wPx, hPx, layout) { makeLayout(wPx, hPx, layout) }
        val layNow by rememberUpdatedState(lay)

        val input = if (editing) {
            Modifier.pointerInput(wPx, hPx) {
                awaitPointerEventScope {
                    var dragging: PadControl? = null
                    var dragId: PointerId? = null
                    var grab = Offset.Zero // khoảng cách từ ngón tay tới tâm điều khiển lúc bắt đầu kéo
                    while (true) {
                        val event = awaitPointerEvent()
                        for (ch in event.changes) {
                            if (dragId == null && ch.pressed && !ch.previousPressed) {
                                val c = editHit(layNow, ch.position)
                                onSelectNow(c)
                                if (c != null) {
                                    dragging = c
                                    dragId = ch.id
                                    grab = controlCenter(layNow, c) - ch.position
                                }
                                ch.consume()
                            } else if (ch.id == dragId) {
                                if (!ch.pressed) {
                                    dragging = null
                                    dragId = null
                                } else {
                                    val c = dragging
                                    if (c != null) {
                                        val t = ch.position + grab
                                        onLayoutNow(layoutNow.moved(c, t.x / wPx, t.y / hPx))
                                    }
                                }
                                ch.consume()
                            }
                        }
                    }
                }
            }
        } else {
            Modifier.pointerInput(lay, sticky) {
                val owner = HashMap<PointerId, PadControl?>()
                var stickId: PointerId? = null
                var lastMask = 0
                var latched = 0 // các nút đang được "giữ" (chỉ khi bật Giữ nút)
                var touchedPrev = 0 // các nút đang bị chạm ở lần xử lý trước
                onMaskNow(0)
                pressed = emptySet()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val nowPressed = HashSet<PadControl>()
                        var stickMask = 0
                        var btnMask = 0
                        var knobOff = Offset.Zero
                        var menuReleased = false
                        for (ch in event.changes) {
                            val id = ch.id
                            if (!ch.pressed) {
                                if (owner[id] == PadControl.MENU) menuReleased = true
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
                                stickMask = stickMask or stickBits(lay, ch.position)
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
                                    btnMask = btnMask or bitOf(c)
                                }
                            }
                            ch.consume()
                        }
                        // Giữ nút: mỗi lần mới chạm vào một nút (không phải cần analog) thì đảo trạng thái giữ.
                        if (sticky) {
                            latched = latched xor (btnMask and touchedPrev.inv())
                        } else {
                            latched = 0
                        }
                        touchedPrev = btnMask
                        val mask = stickMask or btnMask or latched
                        if (latched != 0) {
                            for (c in LATCHABLE) if (bitOf(c) and latched != 0) nowPressed.add(c)
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
            }
        }

        Canvas(Modifier.fillMaxSize().then(input)) {
            drawPad(lay, if (editing) emptySet() else pressed, if (editing) Offset.Zero else knob, measurer)
            if (editing) {
                drawEditOutlines(lay)
                drawSelection(lay, selected)
            }
        }
    }
}

private val LATCHABLE = listOf(
    PadControl.A, PadControl.B, PadControl.L, PadControl.R, PadControl.SELECT, PadControl.START,
)

/** Viền nét đứt quanh mọi điều khiển khi đang chỉnh bố cục (như bản web). */
private fun DrawScope.drawEditOutlines(lay: Layout) {
    val color = Color(0x99FFFFFF)
    val dash = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
    fun ring(c: Offset, r: Float) = drawCircle(color, radius = r * 1.04f, center = c, style = dash)
    fun box(r: Rect) = drawRoundRect(
        color,
        topLeft = Offset(r.left - 4f, r.top - 4f),
        size = Size(r.width + 8f, r.height + 8f),
        cornerRadius = CornerRadius(r.height / 2f, r.height / 2f),
        style = dash,
    )
    ring(lay.stickC, lay.stickR)
    ring(lay.a, lay.aR)
    ring(lay.b, lay.bR)
    ring(lay.menu, lay.menuR)
    box(lay.l)
    box(lay.r)
    box(lay.select)
    box(lay.start)
}

private fun DrawScope.drawSelection(lay: Layout, sel: PadControl?) {
    if (sel == null) return
    val color = Color(0xFFFFFFFF)
    val stroke = Stroke(width = 4f)
    fun rectRing(r: Rect) = drawRoundRect(
        color,
        topLeft = Offset(r.left - 8f, r.top - 8f),
        size = Size(r.width + 16f, r.height + 16f),
        cornerRadius = CornerRadius(16f, 16f),
        style = stroke,
    )
    when (sel) {
        PadControl.STICK -> drawCircle(color, radius = lay.stickR * 1.06f, center = lay.stickC, style = stroke)
        PadControl.A -> drawCircle(color, radius = lay.aR * 1.12f, center = lay.a, style = stroke)
        PadControl.B -> drawCircle(color, radius = lay.bR * 1.12f, center = lay.b, style = stroke)
        PadControl.MENU -> drawCircle(color, radius = lay.menuR * 1.2f, center = lay.menu, style = stroke)
        PadControl.L -> rectRing(lay.l)
        PadControl.R -> rectRing(lay.r)
        PadControl.SELECT -> rectRing(lay.select)
        PadControl.START -> rectRing(lay.start)
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

private fun DrawScope.drawPad(lay: Layout, pressed: Set<PadControl>, knob: Offset, m: androidx.compose.ui.text.TextMeasurer) {
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
    circleButton(lay.a, lay.aR, PadControl.A in pressed, "A", m)
    circleButton(lay.b, lay.bR, PadControl.B in pressed, "B", m)

    shoulder(lay.l, left = true, on = PadControl.L in pressed, label = "L", m = m)
    shoulder(lay.r, left = false, on = PadControl.R in pressed, label = "R", m = m)
    pill(lay.select, PadControl.SELECT in pressed, "SELECT", m)
    pill(lay.start, PadControl.START in pressed, "START", m)

    // Nút ▲ (menu)
    val mr = lay.menuR
    val mRect = Rect(lay.menu.x - mr, lay.menu.y - mr, lay.menu.x + mr, lay.menu.y + mr)
    val on = PadControl.MENU in pressed
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
