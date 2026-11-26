package com.retro.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.data.RomInfo
import java.util.Locale
import kotlinx.coroutines.launch

internal class GameAction(val label: String, val icon: String, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
internal fun GameMenuOverlay(
    rom: RomInfo,
    cover: ImageBitmap?,
    anchor: androidx.compose.ui.geometry.Rect?,
    actions: List<GameAction>,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        // Tắt hiệu ứng mở cửa sổ mặc định của Dialog (nó phóng nhẹ từ giữa nên nội dung bị "trượt" sang trái).
        val dialogView = androidx.compose.ui.platform.LocalView.current
        // Làm mờ phần phía sau bằng hiệu ứng mờ cửa sổ của hệ điều hành (Android 12+, nếu máy bật); không có thì chỉ tối nền.
        val blurOk = remember {
            android.os.Build.VERSION.SDK_INT >= 31 &&
                dialogView.context.getSystemService(android.view.WindowManager::class.java)?.isCrossWindowBlurEnabled == true
        }
        androidx.compose.runtime.SideEffect {
            (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.apply {
                setWindowAnimations(-1)
                setDimAmount(0f)
                if (blurOk && android.os.Build.VERSION.SDK_INT >= 31) {
                    addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    attributes = attributes.also { it.blurBehindRadius = 150 }
                }
            }
        }
        val scale = remember { androidx.compose.animation.core.Animatable(1f) }
        val fade = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(Unit) {
            launch { fade.animateTo(1f, androidx.compose.animation.core.tween(140)) }
            scale.animateTo(1.08f, androidx.compose.animation.core.tween(160))
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
                .background(if (blurOk) Color(0x55000000) else Color(0xCC000000))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            val sw = with(density) { maxWidth.toPx() }
            val sh = with(density) { maxHeight.toPx() }
            val gap = with(density) { 12.dp.toPx() }
            val margin = with(density) { 12.dp.toPx() }
            val insetTop = maxOf(WindowInsets.statusBars.getTop(density).toFloat(), with(density) { 24.dp.toPx() }) + margin
            val insetBottom = maxOf(WindowInsets.navigationBars.getBottom(density).toFloat(), with(density) { 20.dp.toPx() }) + margin
            val a0 = anchor ?: androidx.compose.ui.geometry.Rect(sw / 2 - 90f, sh / 3, sw / 2 + 90f, sh / 3 + 180f)
            val coverW = minOf(a0.width, a0.height)
            // Game ở sát mép màn hình: đẩy bìa vào vùng nhìn thấy để không bị cắt mất khúc nào.
            val grow = coverW * 0.08f
            val ay = a0.top.coerceIn(insetTop + grow / 2, (sh - insetBottom - coverW - grow / 2).coerceAtLeast(insetTop + grow / 2))
            val ax = a0.left.coerceIn(margin + grow / 2, (sw - margin - coverW - grow / 2).coerceAtLeast(margin))
            val a = androidx.compose.ui.geometry.Rect(ax, ay, ax + coverW, ay + coverW)
            val menuW = with(density) { 260.dp.toPx() }
            val dens = density.density
            val coverBottom = a.top + coverW + coverW * 0.04f
            val titleDp = 30f
            val n = actions.size
            // Vị trí bảng: ưu tiên dưới game, rồi trên game (co chiều cao dòng nếu cần, tối thiểu 34dp);
            // chỉ khi cả hai chiều đều không vừa mới đặt cạnh game. Bảng không bao giờ đè lên game.
            val coverTop = a.top - coverW * 0.04f
            val rowBelow = (((sh - insetBottom - (coverBottom + gap)) / dens - titleDp) / n)
            val rowAbove = ((((coverTop - gap) - insetTop) / dens - titleDp) / n)
            var rowDp = 48f
            var mx = a.left.coerceIn(margin, (sw - menuW - margin).coerceAtLeast(margin))
            var my = coverBottom + gap
            when {
                rowBelow >= 44f -> rowDp = rowBelow.coerceAtMost(48f)
                rowAbove >= 44f -> {
                    rowDp = rowAbove.coerceAtMost(48f)
                    my = coverTop - gap - (titleDp + n * rowDp) * dens
                }
                maxOf(rowBelow, rowAbove) >= 34f -> {
                    if (rowBelow >= rowAbove) {
                        rowDp = rowBelow
                    } else {
                        rowDp = rowAbove
                        my = coverTop - gap - (titleDp + n * rowDp) * dens
                    }
                }
                else -> {
                    val bottomY = minOf(coverBottom, sh - insetBottom)
                    rowDp = (((bottomY - insetTop) / dens - titleDp) / n).coerceIn(34f, 48f)
                    my = bottomY - (titleDp + n * rowDp) * dens
                    mx = if (a.right + gap + menuW <= sw - margin) a.right + coverW * 0.04f + gap
                    else (a.left - coverW * 0.04f - gap - menuW).coerceAtLeast(margin)
                }
            }
            // Game giữ nguyên vị trí cũ, phóng to nhẹ.
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(a.left.toInt(), a.top.toInt()) }
                    .size(with(density) { coverW.toDp() })
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101114))
                    .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    Image(bitmap = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                        color = Rc.Muted,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Bảng thao tác
            Column(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(mx.toInt(), my.toInt()) }
                    .width(260.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Rc.Sheet)
                    .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Text(
                    rom.name,
                    color = Rc.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(titleDp.dp).padding(horizontal = 16.dp).wrapContentHeight(Alignment.CenterVertically),
                )
                actions.forEach { act ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = act.onClick)
                            .height(rowDp.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            act.label,
                            color = if (act.danger) Rc.Danger else Rc.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        RetroIcon(act.icon, size = 20.dp, tint = if (act.danger) Rc.Danger else Rc.Muted)
                    }
                }
            }
        }
    }
}
