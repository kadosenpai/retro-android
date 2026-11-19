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

private enum class Ctl { DPAD, STICK, A, B, X, Y, L, R, START, SELECT, MENU, QSAVE, QLOAD, SWAP }

/** Một nút: khung (đơn vị của ảnh skin) và phần mở rộng vùng chạm mỗi cạnh. */
private class SkinItem(
    val c: Ctl,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val eL: Float, val eT: Float, val eR: Float, val eB: Float,
    /** Ảnh nút (png trong assets) vẽ đè lên skin; null = nút đã nằm sẵn trong ảnh nền. */
    val img: String? = null,
)

/** Ảnh núm trượt: kích thước (w, h) và vị trí tâm (cx, cy) trong ảnh, đều theo đơn vị skin. */
private class Knob(val asset: String, val w: Float, val h: Float, val cx: Float, val cy: Float)

/** Khung một màn hình NDS trên skin (đơn vị skin). */
private class SkinScreen(val x: Float, val y: Float, val w: Float, val h: Float)

private class SkinDef(
    val asset: String,
    val mw: Float, val mh: Float,
    val sx: Float, val sy: Float, val sw: Float, val sh: Float,
    /** Hàng thông tin (HUD): toạ độ y trong vùng đen phía trên game. */
    val hy: Float,
    val items: List<SkinItem>,
    /** D-pad hình chữ thập (vẽ sáng từng cánh); false = cần xoay tròn (joycon), chỉ vẽ chấm theo hướng. */
    val cross: Boolean = true,
    /** Thụt vào hai bên của hàng thông tin (đơn vị skin). */
    val hi: Float = 0f,
    /** Màu chữ hàng thông tin (null = trắng mờ, hợp nền đen). */
    val hudColor: Color? = null,
    /** Cỡ chữ hàng thông tin (sp). */
    val hudSp: Float = 12f,
    /** Núm joycon (vẽ đè lên D-pad và trượt theo ngón tay). */
    val knob: Knob? = null,
    /** Núm của cần analog (nút STICK). */
    val stickKnob: Knob? = null,
    /** NDS: khung hai màn hình (ô 0 = chính/trên, ô 1 = phụ/dưới); null = hệ một màn hình. */
    val screens: List<SkinScreen>? = null,
    /** NDS ngang: ô 1 là màn nhỏ, chạm vào để đổi chỗ hai màn. */
    val swap: Boolean = false,
)

private val PORTRAIT = SkinDef(
    asset = "skin/portrait.png",
    mw = 414f, mh = 898f,
    sx = 14f, sy = 135f, sw = 386f, sh = 260f,
    hy = 92f,
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
    hy = 28f,
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

/** Skin "SNES Pocket (JoyCon)": toạ độ theo đơn vị mapping của file Delta (375x812 và 812x375). */
private val SNES_PORTRAIT = SkinDef(
    asset = "skin/snes_portrait.png",
    mw = 375f, mh = 812f,
    sx = 13f, sy = 126f, sw = 348f, sh = 261f,
    hy = 96f,
    cross = false,
    knob = Knob("skin/snes_knob.png", 190f, 200f, 94f, 94.7f),
    items = listOf(
        SkinItem(Ctl.DPAD, 25f, 558f, 121f, 121f, 20f, 20f, 20f, 20f),
        SkinItem(Ctl.X, 240f, 535f, 73f, 73f, 6f, 8f, 6f, 0f),
        SkinItem(Ctl.A, 294f, 581f, 73f, 73f, 0f, 6f, 8f, 6f),
        SkinItem(Ctl.B, 239f, 627f, 73f, 73f, 6f, 0f, 6f, 8f),
        SkinItem(Ctl.Y, 186f, 581f, 73f, 73f, 8f, 6f, 0f, 6f),
        SkinItem(Ctl.L, 10f, 477f, 126f, 34f, 10f, 10f, 10f, 10f),
        SkinItem(Ctl.R, 238f, 477f, 126f, 34f, 10f, 10f, 10f, 10f),
        SkinItem(Ctl.SELECT, 130f, 713f, 48f, 43f, 8f, 8f, 8f, 14f),
        SkinItem(Ctl.START, 195f, 713f, 48f, 43f, 8f, 8f, 8f, 14f),
        SkinItem(Ctl.QSAVE, 40f, 426f, 50f, 19f, 10f, 6f, 10f, 6f),
        SkinItem(Ctl.QLOAD, 285f, 426f, 50f, 19f, 10f, 6f, 10f, 6f),
        SkinItem(Ctl.MENU, 170f, 422f, 33f, 32f, 5f, 5f, 5f, 5f),
    ),
)

private val SNES_LANDSCAPE = SkinDef(
    asset = "skin/snes_landscape.png",
    mw = 812f, mh = 375f,
    sx = 198f, sy = 21f, sw = 416f, sh = 312f,
    // Hàng thông tin vắt qua mép trên màn hình (dải xám mỏng + viền đen), chữ nhỏ, trắng có bóng.
    hy = 9f,
    hi = 27f,
    hudColor = Color(0xF2FFFFFF),
    hudSp = 9f,
    cross = false,
    knob = Knob("skin/snes_knob.png", 190f, 200f, 94f, 94.7f),
    items = listOf(
        SkinItem(Ctl.DPAD, 52f, 123f, 121f, 121f, 20f, 20f, 20f, 20f),
        SkinItem(Ctl.X, 676f, 98f, 73f, 73f, 6f, 8f, 6f, 0f),
        SkinItem(Ctl.A, 729f, 145f, 73f, 73f, 0f, 6f, 8f, 6f),
        SkinItem(Ctl.B, 674f, 192f, 73f, 73f, 6f, 0f, 6f, 8f),
        SkinItem(Ctl.Y, 621f, 145f, 73f, 73f, 8f, 6f, 0f, 6f),
        SkinItem(Ctl.L, 27f, 26f, 126f, 34f, 10f, 10f, 10f, 10f),
        SkinItem(Ctl.R, 658f, 26f, 126f, 34f, 10f, 10f, 10f, 10f),
        SkinItem(Ctl.SELECT, 136f, 295f, 48f, 43f, 8f, 8f, 8f, 8f),
        SkinItem(Ctl.START, 633f, 295f, 48f, 43f, 8f, 8f, 8f, 8f),
        SkinItem(Ctl.QSAVE, 286f, 342f, 50f, 19f, 10f, 6f, 10f, 6f),
        SkinItem(Ctl.QLOAD, 477f, 342f, 50f, 19f, 10f, 6f, 10f, 6f),
        SkinItem(Ctl.MENU, 32f, 311f, 34f, 35f, 5f, 5f, 5f, 5f),
    ),
)

/** Skin "Atomic NDS 2 (Onyx Black)": toạ độ theo đơn vị mapping của file skin (414x896 và 896x414). */
private val NDS_PORTRAIT = SkinDef(
    asset = "skin/nds_portrait.png",
    mw = 414f, mh = 896f,
    sx = 17f, sy = 51f, sw = 380f, sh = 570f,
    hy = 6f,
    hudSp = 10f,
    screens = listOf(SkinScreen(17f, 51f, 380f, 285f), SkinScreen(17f, 336f, 380f, 285f)),
    items = listOf(
        SkinItem(Ctl.DPAD, 43f, 704f, 133f, 133f, 16f, 2f, 12f, 12f, "skin/nds_dpad.png"),
        SkinItem(Ctl.A, 335f, 746f, 50f, 50f, 4f, 4f, 14f, 6f, "skin/nds_btn_a.png"),
        SkinItem(Ctl.B, 284f, 796f, 50f, 50f, 6f, 0f, 6f, 13f, "skin/nds_btn_b.png"),
        SkinItem(Ctl.Y, 230f, 746f, 50f, 50f, 10f, 4f, 4f, 6f, "skin/nds_btn_y.png"),
        SkinItem(Ctl.X, 284f, 695f, 50f, 50f, 6f, 6f, 6f, 0f, "skin/nds_btn_x.png"),
        SkinItem(Ctl.L, 12f, 666f, 54f, 38f, 11f, 11f, 0f, 0f, "skin/nds_l1.png"),
        SkinItem(Ctl.R, 345f, 666f, 54f, 38f, 0f, 11f, 13f, 0f, "skin/nds_r1.png"),
        SkinItem(Ctl.START, 225f, 676f, 21f, 21f, 10f, 10f, 10f, 10f, "skin/nds_start.png"),
        SkinItem(Ctl.SELECT, 167f, 676f, 21f, 21f, 10f, 10f, 10f, 0f, "skin/nds_start.png"),
        SkinItem(Ctl.MENU, 193f, 822f, 29f, 28f, 2f, 11f, 10f, 14f, "skin/nds_menu.png"),
        SkinItem(Ctl.QSAVE, 91f, 632f, 58f, 16f, 0f, 2f, 0f, 4f),
        SkinItem(Ctl.QLOAD, 267f, 632f, 58f, 16f, 0f, 2f, 0f, 4f),
    ),
)

private val NDS_LANDSCAPE = SkinDef(
    asset = "skin/nds_landscape.png",
    mw = 896f, mh = 414f,
    sx = 216f, sy = 20f, sw = 466f, sh = 349f,
    hy = 3f,
    hi = 12f,
    hudSp = 9f,
    hudColor = Color(0xF2FFFFFF),
    // Ô 0 = màn lớn, ô 1 = màn nhỏ ở góc phải dưới (chạm để đổi chỗ).
    screens = listOf(SkinScreen(216f, 20f, 466f, 349f), SkinScreen(716f, 255f, 146f, 107f)),
    swap = true,
    stickKnob = Knob("skin/nds_thumbstick.png", 210f, 210f, 105f, 105f),
    items = listOf(
        SkinItem(Ctl.DPAD, 57f, 70f, 125f, 125f, 10f, 6f, 10f, 10f, "skin/nds_dpad.png"),
        SkinItem(Ctl.STICK, 53f, 235f, 134f, 134f, 10f, 10f, 10f, 10f),
        SkinItem(Ctl.A, 814f, 108f, 50f, 50f, 4f, 4f, 20f, 6f, "skin/nds_btn_a.png"),
        SkinItem(Ctl.B, 764f, 158f, 50f, 50f, 6f, 2f, 6f, 10f, "skin/nds_btn_b.png"),
        SkinItem(Ctl.Y, 710f, 108f, 50f, 50f, 10f, 4f, 4f, 6f, "skin/nds_btn_y.png"),
        SkinItem(Ctl.X, 764f, 57f, 50f, 50f, 6f, 8f, 6f, 2f, "skin/nds_btn_x.png"),
        SkinItem(Ctl.L, 22f, 20f, 54f, 38f, 22f, 20f, 0f, 0f, "skin/nds_l1.png"),
        SkinItem(Ctl.R, 823f, 20f, 54f, 38f, 0f, 20f, 22f, 0f, "skin/nds_r1.png"),
        SkinItem(Ctl.START, 713f, 24f, 21f, 21f, 10f, 10f, 10f, 10f, "skin/nds_start.png"),
        SkinItem(Ctl.SELECT, 166f, 24f, 21f, 21f, 10f, 10f, 10f, 10f, "skin/nds_start.png"),
        SkinItem(Ctl.SWAP, 716f, 255f, 146f, 107f, 0f, 0f, 0f, 0f),
        SkinItem(Ctl.MENU, 27f, 358f, 29f, 28f, 10f, 0f, 0f, 10f, "skin/nds_menu.png"),
        SkinItem(Ctl.QSAVE, 275f, 375f, 56f, 20f, 0f, 0f, 0f, 10f),
        SkinItem(Ctl.QLOAD, 563f, 375f, 56f, 20f, 0f, 0f, 0f, 10f),
    ),
)

private fun bitOf(c: Ctl): Int = when (c) {
    Ctl.A -> Pad.A
    Ctl.B -> Pad.B
    Ctl.X -> Pad.X
    Ctl.Y -> Pad.Y
    Ctl.L -> Pad.L
    Ctl.R -> Pad.R
    Ctl.START -> Pad.START
    Ctl.SELECT -> Pad.SELECT
    else -> 0
}

private val LATCHABLE = listOf(Ctl.A, Ctl.B, Ctl.X, Ctl.Y, Ctl.L, Ctl.R, Ctl.START, Ctl.SELECT)

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
    /** "smoke" (GBA/GB/GBC/NES), "snes" hoặc "nds". */
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

        val dItems = def.items.filter { it.c == Ctl.DPAD || it.c == Ctl.STICK }

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
                if (i.c == Ctl.DPAD || i.c == Ctl.STICK) continue
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
            onMaskNow(0)
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
                            val bits = dirBits(fr, ch.position)
                            stickMask = stickMask or bits
                            dirNow[d.c] = (dirNow[d.c] ?: 0) or bits
                            // Núm trượt theo ngón, tối đa 30% bề rộng khung.
                            val kd = ch.position - fr.center
                            val kl = len(kd.x, kd.y)
                            val kmax = fr.width * 0.3f
                            knobNow[d.c] = if (kl > kmax) kd * (kmax / kl) else kd
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
                if (i.c == Ctl.DPAD || i.c == Ctl.STICK) continue
                if (i.c !in pressed) continue
                val f = frame(i)
                when (i.c) {
                    Ctl.A, Ctl.B, Ctl.X, Ctl.Y, Ctl.START, Ctl.SELECT ->
                        drawCircle(tint, radius = minOf(f.width, f.height) / 2f, center = f.center)
                    Ctl.QSAVE, Ctl.QLOAD, Ctl.L, Ctl.R, Ctl.MENU ->
                        drawRoundRect(tint, f.topLeft, Size(f.width, f.height), CornerRadius(f.height / 2f, f.height / 2f))
                    else -> {}
                }
            }
            for (i in dItems) {
                val f = frame(i)
                val dir = dirs[i.c] ?: 0
                val kd = if (i.c == Ctl.STICK) def.stickKnob else def.knob
                val kb = if (i.c == Ctl.STICK) stickBmp else knobBmp
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
