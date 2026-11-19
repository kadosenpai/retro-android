package com.retro.app.ui

import androidx.compose.ui.graphics.Color

/* Định nghĩa skin: bảng toạ độ nút/màn hình cho từng hệ (dùng bởi SkinPad). */

internal enum class Ctl { DPAD, STICK, A, B, X, Y, L, R, START, SELECT, MENU, QSAVE, QLOAD, SWAP }

/** Một nút: khung (đơn vị của ảnh skin) và phần mở rộng vùng chạm mỗi cạnh. */
internal class SkinItem(
    val c: Ctl,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val eL: Float, val eT: Float, val eR: Float, val eB: Float,
    /** Ảnh nút (png trong assets) vẽ đè lên skin; null = nút đã nằm sẵn trong ảnh nền. */
    val img: String? = null,
)

/** Ảnh núm trượt: kích thước (w, h) và vị trí tâm (cx, cy) trong ảnh, đều theo đơn vị skin. */
internal class Knob(val asset: String, val w: Float, val h: Float, val cx: Float, val cy: Float)

/** Khung một màn hình NDS trên skin (đơn vị skin). */
internal class SkinScreen(val x: Float, val y: Float, val w: Float, val h: Float)

internal class SkinDef(
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

internal val PORTRAIT = SkinDef(
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

internal val LANDSCAPE = SkinDef(
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
internal val SNES_PORTRAIT = SkinDef(
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

internal val SNES_LANDSCAPE = SkinDef(
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
internal val NDS_PORTRAIT = SkinDef(
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

internal val NDS_LANDSCAPE = SkinDef(
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
