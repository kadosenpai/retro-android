package com.retro.app.ui

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.retro.app.core.Pad
import com.retro.app.render.DsLayout
import com.retro.app.render.DsQuad
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Giao diện nút ảo "Xám trong suốt": ảnh skin (định dạng Delta) vẽ đè lên game,
 * mỗi nút là một vùng chạm trong suốt nằm đúng chỗ trên ảnh.
 * Skin: "Atomic Advance - SmokeGray" của Stanley Sy (starvingartist).
 */

/** Giữ nút nhà/góc menu đủ thời gian này thì thoát thẳng về thư viện. */
const val EXIT_HOLD_MS = 400L

private fun bitOf(c: Ctl): Int = when (c) {
    Ctl.A -> Pad.A
    Ctl.B -> Pad.B
    Ctl.X -> Pad.X
    Ctl.Y -> Pad.Y
    Ctl.L -> Pad.L
    Ctl.R -> Pad.R
    Ctl.L2 -> Pad.L2
    Ctl.R2 -> Pad.R2
    Ctl.L3 -> Pad.L3
    Ctl.R3 -> Pad.R3
    Ctl.START -> Pad.START
    Ctl.SELECT -> Pad.SELECT
    else -> 0
}

private val LATCHABLE = listOf(Ctl.A, Ctl.B, Ctl.X, Ctl.Y, Ctl.L, Ctl.R, Ctl.L2, Ctl.R2, Ctl.START, Ctl.SELECT)

/** Điều khiển dạng tròn/chữ thập (D-pad, joycon, hai cần analog): xử lý theo vị trí ngón, không phải nút bấm. */
private fun Ctl.isDir() = this == Ctl.DPAD || this == Ctl.STICK || this == Ctl.LSTICK || this == Ctl.RSTICK

private fun Ctl.isAnalog() = this == Ctl.LSTICK || this == Ctl.RSTICK

private fun len(x: Float, y: Float): Float = sqrt(x * x + y * y)

/**
 * Màn chơi có skin: [game] được đặt đúng khung màn hình của skin, ảnh skin phủ lên trên,
 * cảm ứng đa điểm xử lý trực tiếp trên ảnh (D-pad trượt được, trượt ngón giữa các nút vẫn nhận,
 * nhả ngón trên nút nhà thì mở menu (giữ 0,4 giây thì thoát về thư viện), chạm Lưu/Nạp thì lưu/nạp vào ô 1).
 */
@Composable
fun SkinPad(
    haptics: Boolean,
    sticky: Boolean,
    onMask: (Int) -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    /** NDS: chạm màn dưới. x, y chuẩn hoá -32768..32767 theo toàn khung hình, down = đang chạm. */
    onTouch: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    /** PlayStation: hai cần analog trên skin (-32768..32767): trái X, trái Y, phải X, phải Y. */
    onAnalog: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    /** "smoke" (GBA/GB/GBC/NES/Mega Drive), "snes", "nds" hoặc "psx". */
    skin: String = "smoke",
    game: @Composable (Modifier) -> Unit,
    hud: @Composable (Modifier, Color, Float) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val hapticsNow by rememberUpdatedState(haptics)
    val onMaskNow by rememberUpdatedState(onMask)
    val onMenuNow by rememberUpdatedState(onMenu)
    val onExitNow by rememberUpdatedState(onExit)
    val onSaveNow by rememberUpdatedState(onQuickSave)
    val onLoadNow by rememberUpdatedState(onQuickLoad)
    val onTouchNow by rememberUpdatedState(onTouch)
    val onAnalogNow by rememberUpdatedState(onAnalog)

    var pressed by remember { mutableStateOf(emptySet<Ctl>()) }
    var dirs by remember { mutableStateOf(emptyMap<Ctl, Int>()) }
    var knobs by remember { mutableStateOf(emptyMap<Ctl, Offset>()) }
    var swapped by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { DsLayout.quads = null } }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val land = wPx > hPx
        val def = when (skin) {
            "snes" -> if (land) SNES_LANDSCAPE else SNES_PORTRAIT
            "nds" -> if (land) NDS_LANDSCAPE else NDS_PORTRAIT
            "psx" -> if (land) PSX_LANDSCAPE else PSX_PORTRAIT
            else -> if (land) LANDSCAPE else PORTRAIT
        }
        val kx = wPx / def.mw
        val ky = hPx / def.mh
        val swappedNow = swapped && def.swap

        suspend fun loadAsset(name: String?): ImageBitmap? {
            if (name == null) return null
            return withContext(Dispatchers.IO) {
                try {
                    ctx.assets.open(name).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } catch (_: Throwable) {
                    null
                }
            }
        }

        val bmp by produceState<ImageBitmap?>(null, def.asset) { value = loadAsset(def.asset) }
        val knobBmp by produceState<ImageBitmap?>(null, def.knob?.asset) { value = loadAsset(def.knob?.asset) }
        val stickBmp by produceState<ImageBitmap?>(null, def.stickKnob?.asset) { value = loadAsset(def.stickKnob?.asset) }
        val overlays by produceState(emptyMap<String, ImageBitmap>(), def) {
            val m = HashMap<String, ImageBitmap>()
            for (n in def.items.mapNotNull { it.img }.distinct()) loadAsset(n)?.let { m[n] = it }
            value = m
        }

        // NDS: ô nào hiển thị nửa nào của khung hình (0 = màn trên, 1 = màn dưới).
        val scr = def.screens
        fun showing(slot: Int): Int = if (swappedNow) 1 - slot else slot
        fun slotRect(slot: Int): Rect {
            val s = scr!![slot]
            return Rect(s.x * kx, s.y * ky, (s.x + s.w) * kx, (s.y + s.h) * ky)
        }
        SideEffect {
            DsLayout.quads = scr?.mapIndexed { slot, s ->
                val top = showing(slot) == 0
                DsQuad(
                    if (top) 0f else 0.5f, if (top) 0.5f else 1f,
                    s.x / def.mw, s.y / def.mh, (s.x + s.w) / def.mw, (s.y + s.h) / def.mh,
                )
            }
        }
        // Vùng cảm ứng = ô đang hiện màn dưới; màn nhỏ (ô 1 của skin ngang) chỉ để chạm đổi chỗ.
        val touchRect: Rect? = if (scr == null) null else {
            val slot = if (showing(0) == 1) 0 else 1
            if (def.swap && slot != 0) null else slotRect(slot)
        }

        if (scr != null) {
            game(Modifier.fillMaxSize())
        } else {
            // Màn hình game nằm đúng khung trong skin.
            game(
                Modifier
                    .offset { IntOffset((def.sx * kx).roundToInt(), (def.sy * ky).roundToInt()) }
                    .size(with(density) { (def.sw * kx).toDp() }, with(density) { (def.sh * ky).toDp() }),
            )
        }

        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }

        fun frame(i: SkinItem) = Rect(i.x * kx, i.y * ky, (i.x + i.w) * kx, (i.y + i.h) * ky)
        fun hitRect(i: SkinItem) = Rect((i.x - i.eL) * kx, (i.y - i.eT) * ky, (i.x + i.w + i.eR) * kx, (i.y + i.h + i.eB) * ky)

        // Ảnh nút của skin (nếu có) vẽ đè lên ảnh nền.
        Canvas(Modifier.fillMaxSize()) {
            for (i in def.items) {
                val b = overlays[i.img ?: continue] ?: continue
                val f = frame(i)
                drawImage(
                    b,
                    dstOffset = IntOffset(f.left.roundToInt(), f.top.roundToInt()),
                    dstSize = IntSize(f.width.roundToInt(), f.height.roundToInt()),
                    filterQuality = FilterQuality.High,
                )
            }
        }

        // Hàng thông tin nằm trong vùng đen phía trên game, trên lớp skin.
        hud(
            Modifier
                .offset { IntOffset(((def.sx + def.hi) * kx).roundToInt(), (def.hy * ky).roundToInt()) }
                .width(with(density) { ((def.sw - 2f * def.hi) * kx).toDp() }),
            def.hudColor ?: Color(0xCCFFFFFF),
            def.hudSp,
        )

        val dItems = def.items.filter { it.c.isDir() }

        fun dirBits(fr: Rect, p: Offset): Int {
            val c = fr.center
            val dx = p.x - c.x
            val dy = p.y - c.y
            val d = len(dx, dy)
            if (d < fr.width * 0.11f) return 0
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
                if (i.c.isDir()) continue
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

        fun sendTouch(p: Offset, down: Boolean) {
            val r = touchRect ?: return
            val fx = ((p.x - r.left) / r.width).coerceIn(0f, 1f)
            val fy = ((p.y - r.top) / r.height).coerceIn(0f, 1f)
            // Khung hình NDS là 256x384: màn dưới là nửa dưới.
            val nx = (fx * 65535f - 32768f).roundToInt().coerceIn(-32768, 32767)
            val ny = ((0.5f + fy * 0.5f) * 65535f - 32768f).roundToInt().coerceIn(-32768, 32767)
            onTouchNow(nx, ny, down)
        }

        val input = Modifier.pointerInput(def, wPx, hPx, sticky, swappedNow) {
            val owner = HashMap<PointerId, Ctl?>()
            val dOwner = HashMap<PointerId, SkinItem>()
            var touchId: PointerId? = null
            var lastMask = 0
            var latched = 0 // các nút đang được "giữ" (khi bật Giữ nút)
            var touchedPrev = 0
            var menuId: PointerId? = null // ngón đang đặt trên nút nhà
            var menuDownAt = 0L
            var menuFired = false // đã giữ đủ lâu và đã thoát
            val lastAnalog = IntArray(4)
            onMaskNow(0)
            onAnalogNow(0, 0, 0, 0)
            onTouchNow(0, 0, false)
            pressed = emptySet()
            dirs = emptyMap()
            knobs = emptyMap()
            awaitPointerEventScope {
                while (true) {
                    val event = if (menuId != null && !menuFired) {
                        val remaining = EXIT_HOLD_MS - (SystemClock.uptimeMillis() - menuDownAt)
                        withTimeoutOrNull(maxOf(1L, remaining)) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }
                    if (event == null) {
                        // Giữ nút nhà đủ lâu: thoát thẳng về thư viện.
                        menuFired = true
                        if (hapticsNow) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onExitNow()
                        continue
                    }
                    val now = HashSet<Ctl>()
                    val dirNow = HashMap<Ctl, Int>()
                    val knobNow = HashMap<Ctl, Offset>()
                    val analogNow = HashMap<Ctl, Offset>()
                    var stickMask = 0
                    var btnMask = 0
                    var doMenu = false
                    var doSave = false
                    var doLoad = false
                    var doSwap = false
                    for (ch in event.changes) {
                        val id = ch.id
                        if (!ch.pressed) {
                            when (owner[id]) {
                                Ctl.MENU -> if (!(menuId == id && menuFired)) doMenu = true
                                Ctl.QSAVE -> doSave = true
                                Ctl.QLOAD -> doLoad = true
                                Ctl.SWAP -> doSwap = true
                                else -> {}
                            }
                            owner.remove(id)
                            if (menuId == id) {
                                menuId = null
                                menuFired = false
                            }
                            dOwner.remove(id)
                            if (touchId == id) {
                                sendTouch(ch.position, false)
                                touchId = null
                            }
                            ch.consume()
                            continue
                        }
                        var d = dOwner[id]
                        if (d == null && !ch.previousPressed) {
                            d = dItems.firstOrNull { hitRect(it).contains(ch.position) }
                            if (d != null) dOwner[id] = d
                        }
                        if (d != null) {
                            val fr = frame(d)
                            if (d.c.isAnalog()) {
                                // Cần analog: độ nghiêng = khoảng cách từ tâm / bán kính (tối đa 1); núm trượt theo, giữ trong vành.
                                val v = ch.position - fr.center
                                val l = len(v.x, v.y)
                                val rr = fr.width * 0.5f
                                val a = if (l > rr) v * (rr / l) else v
                                analogNow[d.c] = Offset(a.x / rr, a.y / rr)
                                knobNow[d.c] = a * 0.75f
                            } else {
                                val bits = dirBits(fr, ch.position)
                                stickMask = stickMask or bits
                                dirNow[d.c] = (dirNow[d.c] ?: 0) or bits
                                // Núm trượt theo ngón, tối đa 30% bề rộng khung.
                                val kd = ch.position - fr.center
                                val kl = len(kd.x, kd.y)
                                val kmax = fr.width * 0.3f
                                knobNow[d.c] = if (kl > kmax) kd * (kmax / kl) else kd
                            }
                            owner[id] = null
                        } else {
                            if (touchId == null && !ch.previousPressed && touchRect?.contains(ch.position) == true) touchId = id
                            if (touchId == id) {
                                sendTouch(ch.position, true)
                                owner[id] = null
                            } else {
                                val c = hit(ch.position)
                                owner[id] = c
                                if (c == Ctl.MENU) {
                                    if (menuId == null) {
                                        menuId = id
                                        menuDownAt = ch.uptimeMillis
                                        menuFired = false
                                    }
                                } else if (menuId == id) {
                                    menuId = null // trượt ngón ra khỏi nút nhà thì huỷ
                                }
                                if (c != null) {
                                    now.add(c)
                                    btnMask = btnMask or bitOf(c)
                                }
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
                    // Hai cần analog (PlayStation): chỉ gửi khi giá trị đổi; vùng chết nhỏ để cần không trôi.
                    fun axis(c: Ctl, x: Boolean): Int {
                        val o = analogNow[c] ?: return 0
                        if (len(o.x, o.y) < 0.12f) return 0
                        return ((if (x) o.x else o.y) * 32767f).roundToInt().coerceIn(-32768, 32767)
                    }
                    val an = intArrayOf(axis(Ctl.LSTICK, true), axis(Ctl.LSTICK, false), axis(Ctl.RSTICK, true), axis(Ctl.RSTICK, false))
                    if (!an.contentEquals(lastAnalog)) {
                        an.copyInto(lastAnalog)
                        onAnalogNow(an[0], an[1], an[2], an[3])
                    }
                    pressed = now
                    dirs = dirNow
                    knobs = knobNow
                    if (doMenu) onMenuNow()
                    if (doSave) onSaveNow()
                    if (doLoad) onLoadNow()
                    if (doSwap) swapped = !swapped
                }
            }
        }

        // Lớp sáng nhẹ lên nút đang bấm + nhận cảm ứng.
        Canvas(Modifier.fillMaxSize().then(input)) {
            val tint = Color(0x44FFFFFF)
            for (i in def.items) {
                if (i.c.isDir()) continue
                if (i.c !in pressed) continue
                val f = frame(i)
                when (i.c) {
                    Ctl.A, Ctl.B, Ctl.X, Ctl.Y, Ctl.START, Ctl.SELECT, Ctl.L3, Ctl.R3 ->
                        drawCircle(tint, radius = minOf(f.width, f.height) / 2f, center = f.center)
                    Ctl.QSAVE, Ctl.QLOAD, Ctl.L, Ctl.R, Ctl.L2, Ctl.R2, Ctl.MENU ->
                        drawRoundRect(tint, f.topLeft, Size(f.width, f.height), CornerRadius(f.height / 2f, f.height / 2f))
                    else -> {}
                }
            }
            for (i in dItems) {
                val f = frame(i)
                val dir = dirs[i.c] ?: 0
                val kd = if (i.c == Ctl.STICK || i.c.isAnalog()) def.stickKnob else def.knob
                val kb = if (i.c == Ctl.STICK || i.c.isAnalog()) stickBmp else knobBmp
                if (kd != null && kb != null) {
                    // Núm joycon/cần analog: vẽ ở giữa khung, trượt theo ngón.
                    val c = f.center + (knobs[i.c] ?: Offset.Zero)
                    drawImage(
                        kb,
                        dstOffset = IntOffset((c.x - kd.cx * kx).roundToInt(), (c.y - kd.cy * kx).roundToInt()),
                        dstSize = IntSize((kd.w * kx).roundToInt(), (kd.h * kx).roundToInt()),
                        filterQuality = FilterQuality.High,
                    )
                } else if (dir != 0 && !def.cross) {
                    // Chấm sáng ở phía ngón đang đẩy.
                    val c = f.center
                    val half = f.width / 2f
                    var ox = 0f
                    var oy = 0f
                    if (dir and Pad.UP != 0) oy -= 1f
                    if (dir and Pad.DOWN != 0) oy += 1f
                    if (dir and Pad.LEFT != 0) ox -= 1f
                    if (dir and Pad.RIGHT != 0) ox += 1f
                    val l = len(ox, oy)
                    if (l > 0f) drawCircle(tint, radius = half * 0.3f, center = Offset(c.x + ox / l * half * 0.4f, c.y + oy / l * half * 0.4f))
                } else if (dir != 0) {
                    val c = f.center
                    val half = f.width / 2f
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
}
