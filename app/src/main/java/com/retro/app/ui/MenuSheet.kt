package com.retro.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.retro.app.SlotView
import com.retro.app.data.RomStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel

/** Menu ▲ khi đang chơi: thanh trên (Thoát · Tạm dừng · Tiếp tục), lưới biểu tượng, vài cài đặt gọn. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayMenuSheet(model: AppModel, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = state,
        containerColor = Color.Transparent,
        contentColor = Rc.Text,
        scrimColor = Color(0x59000000),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xB8101216))
                .background(
                    Brush.linearGradient(
                        0f to Color(0x2E6A4BFF),
                        0.55f to Color(0x24E0398A),
                        1f to Color(0x2E10B896),
                    ),
                )
                .glassSurface(32.dp, tint = Color(0x0FFFFFFF))
                .sheetMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            // 0 = menu chính, 1 = Cheat, 2 = Tay cầm, 3 = Lưu trạng thái, 4 = Nạp trạng thái, 5 = Hình ảnh
            var page by remember { mutableStateOf(0) }
            val st = model.settings
            when (page) {
                5 -> DisplayPage(model) { page = 0 }
                1 -> CheatsPage(model) { page = 0 }
                2 -> ControllerPage(model) { page = 0 }
                3 -> StatesPage(model, save = true, onClose = onClose) { page = 0 }
                4 -> StatesPage(model, save = false, onClose = onClose) { page = 0 }
                else -> {
                    // Thanh trên
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            PillButton("Thoát", compact = true, primary = false, onClick = {
                                onClose()
                                model.exitToLibrary()
                            })
                        }
                        Text("Tạm dừng", color = Rc.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            PillButton("Tiếp tục", compact = true, primary = false, onClick = onClose)
                        }
                    }
                    Text(
                        model.current?.name ?: "",
                        color = Rc.Faint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )

                    // Lưới biểu tượng 3 x 2
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        MenuTile("save", "Lưu", Modifier.weight(1f)) { page = 3 }
                        MenuTile("upload", "Nạp", Modifier.weight(1f)) { page = 4 }
                        MenuTile("star", "Cheat", Modifier.weight(1f)) {
                            page = 1
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        MenuTile("fast-forward", "Tua nhanh", Modifier.weight(1f), on = model.speed != 1f) {
                            val fast = model.speed == 1f
                            model.changeSpeed(if (fast) 2f else 1f)
                            model.showToast(if (fast) "Tua nhanh: 2×" else "Tua nhanh: tắt")
                        }
                        MenuTile("lock", "Giữ nút", Modifier.weight(1f), on = st.sticky) {
                            val on = !st.sticky
                            model.update { it.copy(sticky = on) }
                            model.showToast(if (on) "Giữ nút: bật" else "Giữ nút: tắt")
                        }
                        MenuTile("reset", "Reset", Modifier.weight(1f)) {
                            model.resetGame()
                            onClose()
                        }
                    }

                    SectionTitle("Âm thanh & điều khiển")
                    GroupCard {
                        ToggleRow("Âm thanh", st.sound, icon = "volume") { v -> model.update { it.copy(sound = v) } }
                        RowDivider()
                        ToggleRow("Hiện nút ảo", st.showPad, icon = "dpad") { v -> model.update { it.copy(showPad = v) } }
                        RowDivider()
                        ActionRow("Tay cầm", icon = "gamepad", chevron = true) { page = 2 }
                        RowDivider()
                        ActionRow("Hình ảnh", icon = "image", chevron = true, subtitle = st.filter.label) { page = 5 }
                        RowDivider()
                        ToggleRow("Hiện thông số", st.showStats, icon = "eye") { v ->
                            model.update { it.copy(showStats = v) }
                        }
                    }
                }
            }
        }
    }
}

/** Ô biểu tượng trong lưới menu: ô kính bo tròn; bật = nền trắng biểu tượng đen. */
@Composable
private fun MenuTile(icon: String, label: String, modifier: Modifier = Modifier, on: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier.clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .glassSurface(22.dp, tint = if (on) Color(0xF2FFFFFF) else Color(0x2EFFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            RetroIcon(icon, size = 28.dp, tint = if (on) Color.Black else Rc.Text)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (on) Rc.Text else Color(0xD9FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private val timeFmt = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())

/** Lưới ô trạng thái (2 cột, ảnh chụp + ngày giờ). save = chạm để lưu; ngược lại chạm để nạp (kèm ô Tự động). */
@Composable
private fun StatesPage(model: AppModel, save: Boolean, onClose: () -> Unit, onBack: () -> Unit) {
    PageHeader(if (save) "Lưu trạng thái" else "Nạp trạng thái", onBack)
    LaunchedEffect(Unit) { model.refreshSlots() }
    Spacer(Modifier.height(12.dp))
    val bySlot = model.slots.associateBy { it.slot }
    val ids = (1..RomStore.SLOT_COUNT).toList() + if (save) emptyList() else listOf(RomStore.AUTO_SLOT)
    ids.chunked(2).forEachIndexed { i, pair ->
        if (i > 0) Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (n in pair) {
                val view = bySlot[n]
                val filled = view != null && view.savedAt > 0
                SlotCard(
                    title = if (n == RomStore.AUTO_SLOT) "Tự động" else "Ô $n",
                    view = view,
                    enabled = save || filled,
                    modifier = Modifier.weight(1f),
                ) {
                    if (save) model.saveSlot(n) else model.loadSlot(n) { ok -> if (ok) onClose() }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlotCard(title: String, view: SlotView?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val filled = view != null && view.savedAt > 0
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Rc.Glass)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .background(Rc.Chip),
            contentAlignment = Alignment.Center,
        ) {
            val img = view?.thumb
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                RetroIcon("image", size = 26.dp, tint = Rc.Faint)
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(title, color = if (enabled) Rc.Text else Rc.Faint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (filled) timeFmt.format(Date(view!!.savedAt)) else "Trống",
                color = Rc.Muted,
                fontSize = 12.sp,
            )
        }
    }
}
