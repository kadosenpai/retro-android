package com.retro.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Nhóm nội dung dạng thẻ kính bo tròn, như `.group` của bản web. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Ds.CardRadius))
            .background(Ds.CardFill)
            .border(BorderStroke(1.dp, Ds.CardStroke), RoundedCornerShape(Ds.CardRadius)),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, top = Ds.SectionGap, bottom = Ds.GapSm),
        color = Rc.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp,
    )
}

/** Đường kẻ mảnh giữa hai hàng; thụt vào bằng chỗ biểu tượng (18 + 24 + 16 = 58dp). */
@Composable
fun RowDivider(inset: androidx.compose.ui.unit.Dp = 52.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(Ds.Divider),
    )
}

/** Khung một hàng: biểu tượng (tuỳ chọn) · tiêu đề + chú thích · phần cuối (công tắc, mũi tên...). */
@Composable
private fun RetroRow(
    modifier: Modifier,
    title: String,
    icon: String?,
    subtitle: String?,
    danger: Boolean,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Ds.RowHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.width(Ds.IconRow), contentAlignment = Alignment.Center) {
                RetroIcon(icon, size = Ds.IconRow, tint = if (danger) Rc.Danger else Rc.Muted)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) Rc.Danger else Rc.Text,
                fontSize = 16.sp,
                fontWeight = if (danger) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (subtitle != null) {
                Text(subtitle, color = Rc.Muted, fontSize = 13.sp)
            }
        }
        trailing()
    }
}

@Composable
fun ActionRow(
    title: String,
    danger: Boolean = false,
    icon: String? = null,
    subtitle: String? = null,
    chevron: Boolean = false,
    onClick: () -> Unit,
) {
    RetroRow(Modifier.clickable(onClick = onClick), title, icon, subtitle, danger) {
        if (chevron) {
            Spacer(Modifier.width(12.dp))
            // Cùng bề rộng với công tắc (52dp) và đặt sát mép phải: mũi tên chevron nằm lệch trong khung 24dp
            // (nét thật chỉ từ 9 đến 15) nên dịch sang phải 9dp để mép nét trùng mép công tắc.
            Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterEnd) {
                RetroIcon("chevron-right", modifier = Modifier.offset(x = 9.dp), tint = if (danger) Rc.Danger else Rc.Muted)
            }
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    icon: String? = null,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    RetroRow(Modifier.clickable { onChange(!checked) }, title, icon, subtitle, false) {
        Spacer(Modifier.width(12.dp))
        RetroSwitch(checked, onChange)
    }
}

/** Nút gạt như web: bật = nền trắng, núm đen; tắt = nền kính, núm trắng. */
@Composable
fun RetroSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x = animateDpAsState(if (checked) 24.dp else 3.dp, label = "thumb").value
    Box(
        Modifier
            .size(52.dp, 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) Color.White else Color(0x38FFFFFF))
            .clickable(role = Role.Switch) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = x, y = 3.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color.White),
        )
    }
}

/**
 * Đầu trang con: mũi tên quay lại trần (không nền, cùng kiểu icon ở thư viện) + tiêu đề lớn sát bên.
 * Cao cố định 48dp; khoảng cách xuống nội dung do SectionTitle / Spacer phía dưới quyết định (24dp).
 * Vùng chạm 44dp, nhưng nét mũi tên nằm sát lề trái nội dung.
 */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp)) {
            Box(
                Modifier
                    .requiredSize(44.dp)
                    .offset(x = (-10).dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                RetroIcon("chevron-left", size = 28.dp, tint = Rc.Text)
            }
        }
        Spacer(Modifier.width(2.dp))
        Text(
            title,
            color = Rc.Text,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
        )
    }
}

/** Nút bo tròn: `primary` = nền trắng chữ đen (như .btn.primary của web), ngược lại là kính đậm. */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    primary: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val fg = if (primary) Color.Black else Rc.Text
    Row(
        modifier
            .heightIn(min = if (compact) 36.dp else 44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) Color.White else Rc.GlassStrong)
            .border(BorderStroke(1.dp, if (primary) Color.White else Rc.Stroke), RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 16.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            RetroIcon(icon, size = 20.dp, tint = fg)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** clickable không gợn sóng (dùng cho các nút nhỏ phủ trên game). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

/**
 * Giới hạn chiều cao nội dung bảng: tối đa bằng màn hình trừ thanh trạng thái.
 * Trang ngắn thì bảng ôm sát nội dung (không còn khoảng trống phía trên); trang dài thì cuộn.
 */
@Composable
fun Modifier.sheetMaxHeight(): Modifier {
    val density = LocalDensity.current
    val view = LocalView.current
    val cfg = LocalConfiguration.current
    // keyed on the configuration so it is recomputed after rotation
    val rootH = remember(cfg.orientation, cfg.screenHeightDp, cfg.screenWidthDp) { view.rootView.height }
    val winH = if (rootH > 0) with(density) { rootH.toDp() } else cfg.screenHeightDp.dp
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    return this.heightIn(max = winH - top)
}
