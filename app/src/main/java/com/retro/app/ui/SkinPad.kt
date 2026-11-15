package com.retro.app.ui

import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.retro.app.core.Pad
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Giao diện nút ảo "Xám trong suốt": ảnh skin (định dạng Delta) vẽ đè lên game,
 * mỗi nút là một vùng chạm trong suốt nằm đúng chỗ trên ảnh.
 * Skin: "Atomic Advance - SmokeGray" của Stanley Sy (starvingartist).
 */

private enum class Ctl { DPAD, A, B, L, R, START, SELECT, MENU, QSAVE, QLOAD }

/** Một nút: khung (đơn vị của ảnh skin) và phần mở rộng vùng chạm mỗi cạnh. */
private class SkinItem(
    val c: Ctl,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val eL: Float, val eT: Float, val eR: Float, val eB: Float,
)

private class SkinDef(
    val asset: String,
    val mw: Float, val mh: Float,
    val sx: Float, val sy: Float, val sw: Float, val sh: Float,
    val items: List<SkinItem>,
)

private val PORTRAIT = SkinDef(
    asset = "skin/portrait.png",
    mw = 414f, mh = 898f,
    sx = 14f, sy = 135f, sw = 386f, sh = 260f,
    items = listOf(
        SkinItem(Ctl.DPAD, 35f, 612f, 142f, 142f, 12f, 12f, 12f, 12f),
        SkinItem(Ctl.A, 306f, 611f, 72f, 72f, 10f, 12f, 12f, 10f),
        SkinItem(Ctl.B, 236f, 680f, 72f, 72f, 12f, 10f, 10f, 12f),
        SkinItem(Ctl.L, 17f, 533f, 93f, 38f, 18f, 12f, 18f, 12f),
        SkinItem(Ctl.R, 304f, 533f, 93f, 38f, 18f, 10f, 18f, 10f),
        SkinItem(Ctl.START, 225f, 544f, 21f, 21f, 8f, 10f, 10f, 18f),
        SkinItem(Ctl.SELECT, 168f, 544f, 21f, 21f, 10f, 10f, 8f, 18f),
        SkinItem(Ctl.MENU, 182f, 814f, 50f, 27f, 8f, 8f, 8f, 8f),
        SkinItem(Ctl.QSAVE, 294f, 476f, 57f, 17f, 12f, 6f, 12f, 6f),
        SkinItem(Ctl.QLOAD, 64f, 476f, 57f, 17f, 12f, 6f, 12f, 6f),
    ),
)

private val LANDSCAPE = SkinDef(
    asset = "skin/landscape.png",
    mw = 898f, mh = 414f,
    sx = 234f, sy = 48f, sw = 430f, sh = 290f,
    items = listOf(
        SkinItem(Ctl.DPAD, 62f, 119f, 132f, 132f, 14f, 14f, 14f, 14f),
        SkinItem(Ctl.A, 772f, 110f, 68f, 68f, 10f, 20f, 20f, 10f),
        SkinItem(Ctl.B, 705f, 176f, 68f, 68f, 20f, 10f, 10f, 20f),
        SkinItem(Ctl.L, 55f, 33f, 114f, 38f, 14f, 14f, 14f, 10f),
        SkinItem(Ctl.R, 741f, 33f, 114f, 38f, 14f, 14f, 14f, 10f),
        SkinItem(Ctl.START, 707f, 300f, 21f, 21f, 10f, 10f, 16f, 10f),
        SkinItem(Ctl.SELECT, 707f, 352f, 21f, 21f, 10f, 10f, 16f, 10f),
        SkinItem(Ctl.MENU, 152f, 317f, 38f, 38f, 0f, 0f, 0f, 0f),
        SkinItem(Ctl.QSAVE, 584f, 349f, 57f, 17f, 0f, 6f, 0f, 6f),
        SkinItem(Ctl.QLOAD, 270f, 349f, 57f, 17f, 0f, 6f, 0f, 6f),
    ),
)

private fun bitOf(c: Ctl): Int = when (c) {
    Ctl.A -> Pad.A
    Ctl.B -> Pad.B
    Ctl.L -> Pad.L
    Ctl.R -> Pad.R
    Ctl.START -> Pad.START
    Ctl.SELECT -> Pad.SELECT
    else -> 0
}

private val LATCHABLE = listOf(Ctl.A, Ctl.B, Ctl.L, Ctl.R, Ctl.START, Ctl.SELECT)

private fun len(x: Float, y: Float): Float = sqrt(x * x + y * y)

/**
 * Màn chơi có skin: [game] được đặt đúng khung màn hình của skin, ảnh skin phủ lên trên,
 * cảm ứng đa điểm xử lý trực tiếp trên ảnh (D-pad trượt được, trượt ngón giữa các nút vẫn nhận,
 * nhả ngón trên nút nhà thì mở menu, chạm Lưu/Nạp thì lưu/nạp vào ô 1).
 */
@Composable
fun SkinPad(
    haptics: Boolean,
    sticky: Boolean,
    onMask: (Int) -> Unit,
    onMenu: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    game: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val hapticsNow by rememberUpdatedState(haptics)
    val onMaskNow by rememberUpdatedState(onMask)
    val onMenuNow by rememberUpdatedState(onMenu)
    val onSaveNow by rememberUpdatedState(onQuickSave)
    val onLoadNow by rememberUpdatedState(onQuickLoad)

    var pressed by remember { mutableStateOf(emptySet<Ctl>()) }
    var dir by remember { mutableStateOf(0) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val def = if (wPx > hPx) LANDSCAPE else PORTRAIT
        val kx = wPx / def.mw
        val ky = hPx / def.mh

        val bmp by produceState<ImageBitmap?>(null, def.asset) {
            value = withContext(Dispatchers.IO) {
                try {
                    ctx.assets.open(def.asset).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } catch (_: Throwable) {
                    null
                }
            }
        }

        // Màn hình game nằm đúng khung trong skin.
        game(
            Modifier
                .offset { IntOffset((def.sx * kx).roundToInt(), (def.sy * ky).roundToInt()) }
                .size(with(density) { (def.sw * kx).toDp() }, with(density) { (def.sh * ky).toDp() }),
        )

        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }

        val dpad = def.items.first { it.c == Ctl.DPAD }
        fun frame(i: SkinItem) = Rect(i.x * kx, i.y * ky, (i.x + i.w) * kx, (i.y + i.h) * ky)
        fun hitRect(i: SkinItem) = Rect((i.x - i.eL) * kx, (i.y - i.eT) * ky, (i.x + i.w + i.eR) * kx, (i.y + i.h + i.eB) * ky)
        val dFrame = frame(dpad)
        val dHit = hitRect(dpad)

        fun dirBits(p: Offset): Int {
            val c = dFrame.center
            val dx = p.x - c.x
            val dy = p.y - c.y
            val d = len(dx, dy)
            if (d < dFrame.width * 0.11f) return 0
            var m = 0
            if (dx > 0.38f * d) m = m or Pad.RIGHT
            if (dx < -0.38f * d) m = m or Pad.LEFT
            if (dy > 0.38f * d) m = m or Pad.DOWN
            if (dy < -0.38f * d) m = m or Pad.UP
            return m
        }

        fun hit(p: Offset): Ctl? {
            var best: Ctl? = null
            var bestD = Float.MAX_VALUE
            for (i in def.items) {
                if (i.c == Ctl.DPAD) continue
                if (!hitRect(i).contains(p)) continue
                val c = frame(i).center
                val d = len(p.x - c.x, p.y - c.y)
                if (d < bestD) {
                    bestD = d
                    best = i.c
                }
            }
            return best
        }

        val input = Modifier.pointerInput(def, wPx, hPx, sticky) {
            val owner = HashMap<PointerId, Ctl?>()
            var dpadId: PointerId? = null
            var lastMask = 0
            var latched = 0 // các nút đang được "giữ" (khi bật Giữ nút)
            var touchedPrev = 0
            onMaskNow(0)
            pressed = emptySet()
            dir = 0
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val now = HashSet<Ctl>()
                    var stickMask = 0
                    var btnMask = 0
                    var doMenu = false
                    var doSave = false
                    var doLoad = false
                    for (ch in event.changes) {
                        val id = ch.id
                        if (!ch.pressed) {
                            when (owner[id]) {
                                Ctl.MENU -> doMenu = true
                                Ctl.QSAVE -> doSave = true
                                Ctl.QLOAD -> doLoad = true
                                else -> {}
                            }
                            owner.remove(id)
                            if (dpadId == id) dpadId = null
                            ch.consume()
                            continue
                        }
                        if (dpadId == null && !ch.previousPressed && dHit.contains(ch.position)) dpadId = id
                        if (dpadId == id) {
                            stickMask = stickMask or dirBits(ch.position)
                            owner[id] = null
                        } else {
                            val c = hit(ch.position)
                            owner[id] = c
                            if (c != null) {
                                now.add(c)
                                btnMask = btnMask or bitOf(c)
                            }
                        }
                        ch.consume()
                    }
                    // Giữ nút: mỗi lần mới chạm vào một nút thì đảo trạng thái giữ.
                    latched = if (sticky) latched xor (btnMask and touchedPrev.inv()) else 0
                    touchedPrev = btnMask
                    val mask = stickMask or btnMask or latched
                    if (latched != 0) for (c in LATCHABLE) if (bitOf(c) and latched != 0) now.add(c)
                    if (mask != lastMask) {
                        if ((mask and lastMask.inv()) != 0 && hapticsNow) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        lastMask = mask
                        onMaskNow(mask)
                    }
                    pressed = now
                    dir = stickMask
                    if (doMenu) onMenuNow()
                    if (doSave) onSaveNow()
                    if (doLoad) onLoadNow()
                }
            }
        }

        // Lớp sáng nhẹ lên nút đang bấm + nhận cảm ứng.
        Canvas(Modifier.fillMaxSize().then(input)) {
            val tint = Color(0x44FFFFFF)
            for (i in def.items) {
                if (i.c == Ctl.DPAD) continue
                if (i.c !in pressed) continue
                val f = frame(i)
                when (i.c) {
                    Ctl.A, Ctl.B, Ctl.START, Ctl.SELECT ->
                        drawCircle(tint, radius = minOf(f.width, f.height) / 2f, center = f.center)
                    Ctl.QSAVE, Ctl.QLOAD, Ctl.L, Ctl.R, Ctl.MENU ->
                        drawRoundRect(tint, f.topLeft, Size(f.width, f.height), CornerRadius(f.height / 2f, f.height / 2f))
                    else -> {}
                }
            }
            if (dir != 0) {
                val c = dFrame.center
                val half = dFrame.width / 2f
                val t = half * 0.32f
                val cr = CornerRadius(t * 0.4f, t * 0.4f)
                fun arm(r: Rect) = drawRoundRect(tint, r.topLeft, Size(r.width, r.height), cr)
                if (dir and Pad.UP != 0) arm(Rect(c.x - t, c.y - half, c.x + t, c.y))
                if (dir and Pad.DOWN != 0) arm(Rect(c.x - t, c.y, c.x + t, c.y + half))
                if (dir and Pad.LEFT != 0) arm(Rect(c.x - half, c.y - t, c.x, c.y + t))
                if (dir and Pad.RIGHT != 0) arm(Rect(c.x, c.y - t, c.x + half, c.y + t))
            }
        }
    }
}
