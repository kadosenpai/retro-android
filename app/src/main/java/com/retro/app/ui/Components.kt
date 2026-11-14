package com.retro.app.ui

import android.os.Build
import android.view.View
import android.view.WindowManager
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider

/** Nhóm nội dung dạng thẻ kính bo tròn, như `.group` của bản web. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Rc.Glass),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp),
        color = Rc.Text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Đường kẻ mảnh giữa hai hàng; thụt vào bằng chỗ biểu tượng (18 + 24 + 16 = 58dp). */
@Composable
fun RowDivider(inset: androidx.compose.ui.unit.Dp = 58.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(Rc.Line),
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
            .heightIn(min = 64.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                RetroIcon(icon, tint = if (danger) Rc.Danger else Rc.Muted)
            }
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) Rc.Danger else Rc.Text,
                fontSize = 18.sp,
                fontWeight = if (danger) FontWeight.Bold else FontWeight.Normal,
            )
            if (subtitle != null) {
                Text(subtitle, color = Rc.Muted, fontSize = 14.sp)
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
            RetroIcon("chevron-right", tint = if (danger) Rc.Danger else Rc.Muted)
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
    val x = animateDpAsState(if (checked) 30.dp else 4.dp, label = "thumb").value
    Box(
        Modifier
            .size(62.dp, 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (checked) Color.White else Color(0x38FFFFFF))
            .clickable(role = Role.Switch) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = x, y = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color.White),
        )
    }
}

/** Hàng chip nằm trên một dòng, các chip chia đều chiều rộng. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            val on = opt == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) Color.White else Rc.Glass)
                    .border(BorderStroke(1.dp, if (on) Color.White else Rc.Stroke), RoundedCornerShape(20.dp))
                    .clickable { onSelect(opt) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(opt),
                    color = if (on) Color.Black else Rc.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
fun SmallLabel(text: String) {
    Text(
        text,
        color = Rc.Muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** Nút tròn kính đậm có biểu tượng ✕ (đóng bảng). */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Rc.GlassStrong)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon("x", tint = Rc.Text)
    }
}

/** Đầu bảng: tiêu đề lớn bên trái, nút ✕ bên phải. */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            color = Rc.Text,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.56).sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        CloseButton(onClose)
    }
}

/** Nút bo tròn: `primary` = nền trắng chữ đen (như .btn.primary của web), ngược lại là kính đậm. */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val fg = if (primary) Color.Black else Rc.Text
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) Color.White else Rc.GlassStrong)
            .border(BorderStroke(1.dp, if (primary) Color.White else Rc.Stroke), RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            RetroIcon(icon, size = 22.dp, tint = fg)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Làm mờ thật phần nền phía sau bảng (Android 12 trở lên). Gọi bên trong nội dung của ModalBottomSheet.
 * Nếu máy không hỗ trợ thì không có gì xảy ra; vẫn còn lớp tối phủ phía sau.
 */
@Composable
fun SheetWindowBlur(radiusPx: Int = 40) {
    val view = LocalView.current
    SideEffect {
        if (Build.VERSION.SDK_INT < 31) return@SideEffect
        var v: View? = view
        var window: android.view.Window? = null
        var hops = 0
        while (v != null && window == null && hops < 6) {
            window = (v as? DialogWindowProvider)?.window
            v = v.parent as? View
            hops++
        }
        val w = window ?: return@SideEffect
        try {
            w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val lp = w.attributes
            lp.setBlurBehindRadius(radiusPx)
            w.attributes = lp
        } catch (_: Throwable) {
        }
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
