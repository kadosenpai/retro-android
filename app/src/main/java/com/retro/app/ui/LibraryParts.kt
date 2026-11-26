package com.retro.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun consoleLabel(mode: String) = when (mode) {
    "ds" -> "DS"
    "dsi" -> "DSi"
    else -> "Tự động"
}

/** Đọc ảnh bìa của game từ bộ nhớ (ngoài luồng chính); null nếu chưa có. */
@Composable
internal fun rememberCover(model: AppModel, rom: RomInfo): ImageBitmap? {
    val img by produceState<ImageBitmap?>(null, rom.id, rom.coverAt) {
        value = if (rom.coverAt == 0L) {
            null
        } else {
            withContext(Dispatchers.IO) {
                model.store.readCover(rom.id)?.let { b ->
                    runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()
                }
            }
        }
    }
    return img
}

/** Nút tròn có biểu tượng (cài đặt, sắp xếp, thêm game). */
@Composable
internal fun IconBtn(icon: String, desc: String, modifier: Modifier = Modifier, small: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .size(if (small) 36.dp else 44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon(icon, size = if (small) 22.dp else 26.dp, tint = Rc.Text)
    }
}

/** Ô tìm kiếm dạng viên thuốc. */
@Composable
internal fun SearchBox(query: String, onChange: (String) -> Unit, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Rc.Glass)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroIcon("search", size = 20.dp, tint = Rc.Muted)
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Rc.Text, fontSize = 15.sp, fontFamily = InterFamily),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Tìm tên ROM hoặc mã ID...", color = Rc.Faint, fontSize = 15.sp)
                    inner()
                }
            },
        )
    }
}

/** Menu chọn kiểu sắp xếp: dấu ✓ ở kiểu đang dùng, dấu − ở các kiểu còn lại. */
@Composable
internal fun SortMenu(expanded: Boolean, onDismiss: () -> Unit, current: SortKey, onPick: (SortKey) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortKey.values().forEach { k ->
            DropdownMenuItem(
                leadingIcon = { RetroIcon(if (k == current) "check" else "minus", size = 20.dp, tint = Rc.Muted) },
                text = { Text(k.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
                onClick = {
                    onPick(k)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
internal fun EmptyState(hasAny: Boolean, onAdd: () -> Unit) {
    val cfg = LocalConfiguration.current
    val wide = cfg.screenWidthDp >= 600
    val compact = cfg.screenHeightDp < 480
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (hasAny) {
            Text(
                "Không tìm thấy game nào",
                color = Rc.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
            )
            Text(
                "Thử từ khoá khác.",
                color = Rc.Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            return@Column
        }
        val shape = RoundedCornerShape(28.dp)
        val cardMod = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Rc.Glass)
            .border(BorderStroke(1.dp, Rc.Stroke), shape)
        val desc = "Bấm dấu + để thêm file ROM (.gba, .gb, .gbc) của bạn. Chỉ dùng ROM bạn sở hữu hợp pháp; dữ liệu nằm trên máy này."
        if (compact) {
            // Màn ngang thấp: thẻ gọn một hàng để nằm vừa màn hình, không phải vuốt.
            Row(cardMod.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroIcon("layers", size = 40.dp, tint = Rc.Text)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa có game nào", color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(desc, color = Rc.Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(18.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        } else if (wide) {
            Row(cardMod.padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroIcon("layers", size = 56.dp, tint = Rc.Text)
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa có game nào", color = Rc.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(desc, color = Rc.Muted, fontSize = 16.sp)
                }
                Spacer(Modifier.width(22.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        } else {
            Column(cardMod.padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RetroIcon("layers", size = 56.dp, tint = Rc.Text)
                Spacer(Modifier.height(18.dp))
                Text("Chưa có game nào", color = Rc.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(desc, color = Rc.Muted, fontSize = 17.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        }
    }
}

/** Ô game trong lưới: bìa vuông, tên bên dưới. Chạm để chơi, giữ lâu để mở menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RomTile(
    rom: RomInfo,
    cover: ImageBitmap?,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu()
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101114))
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                    color = Rc.Muted,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            rom.name,
            color = Rc.Text,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            relativeDay(maxOf(rom.lastPlayedAt, rom.addedAt)),
            color = Rc.Muted,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
        )
    }
}

internal fun relativeDay(ts: Long): String {
    if (ts <= 0L) return "Chưa chơi"
    val days = ((System.currentTimeMillis() - ts) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Hôm nay"
        days == 1 -> "Hôm qua"
        days < 30 -> "$days ngày trước"
        else -> "Lâu rồi"
    }
}
