package com.retro.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.retro.app.data.Settings
import com.retro.app.render.FilterMode
import com.retro.app.render.ScaleMode

private val SPEEDS = listOf(0.5f, 1f, 1.5f, 2f, 3f, 4f)

private fun speedLabel(s: Float): String = if (s == s.toInt().toFloat()) "${s.toInt()}×" else "${s}×"

/** Nhóm "Hình ảnh" + "Âm thanh & điều khiển", dùng chung cho menu khi chơi và Cài đặt. */
@Composable
fun SettingsGroups(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, inGame: Boolean) {
    SectionTitle("Hình ảnh")
    GroupCard {
        SmallLabel("Co giãn màn hình")
        ChipRow(ScaleMode.values().toList(), settings.scale, { it.label }, { v -> onChange { it.copy(scale = v) } })
        SmallLabel("Bộ lọc")
        ChipRow(FilterMode.values().toList(), settings.filter, { it.label }, { v -> onChange { it.copy(filter = v) } })
    }
    SectionTitle("Âm thanh & điều khiển")
    GroupCard {
        ToggleRow("Bật âm thanh", settings.sound) { v -> onChange { it.copy(sound = v) } }
        ToggleRow("Hiện nút ảo", settings.showPad) { v -> onChange { it.copy(showPad = v) } }
        ToggleRow("Rung khi chạm", settings.haptics) { v -> onChange { it.copy(haptics = v) } }
        if (inGame) ToggleRow("Hiện thông số", settings.showStats) { v -> onChange { it.copy(showStats = v) } }
    }
}

/** Menu ▲ khi đang chơi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayMenuSheet(model: AppModel, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = state,
        containerColor = Rc.Sheet,
        contentColor = Rc.Text,
        scrimColor = Color(0x8C000000),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = null,
    ) {
        SheetWindowBlur()
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            var page by remember { mutableStateOf(0) }
            if (page == 1) {
                CheatsPage(model) { page = 0 }
            } else if (page == 2) {
                ControllerPage(model) { page = 0 }
            } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Menu", color = Rc.Text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
            GroupCard {
                ActionRow("Tiếp tục", onClick = onClose)
                ActionRow("Cheat") { page = 1 }
                ActionRow("Tay cầm") { page = 2 }
                ActionRow("Chỉnh bố cục nút ảo") {
                    onClose()
                    model.editingLayout = true
                }
                ActionRow("Reset game") {
                    model.resetGame()
                    onClose()
                }
                ActionRow("Thoát về Thư viện") {
                    onClose()
                    model.exitToLibrary()
                }
            }

            SectionTitle("Trạng thái")
            LaunchedEffect(Unit) { model.refreshSlots() }
            GroupCard {
                val bySlot = model.slots.associateBy { it.slot }
                for (n in 1..RomStore.SLOT_COUNT) {
                    SlotRow(
                        title = "Ô $n",
                        view = bySlot[n],
                        canSave = true,
                        onSave = { model.saveSlot(n) },
                        onLoad = { model.loadSlot(n) { ok -> if (ok) onClose() } },
                    )
                }
                SlotRow(
                    title = "Tự động",
                    view = bySlot[RomStore.AUTO_SLOT],
                    canSave = false,
                    onSave = {},
                    onLoad = { model.loadSlot(RomStore.AUTO_SLOT) { ok -> if (ok) onClose() } },
                )
            }

            SectionTitle("Tốc độ game")
            GroupCard {
                ChipRow(SPEEDS, model.speed, { speedLabel(it) }, { model.changeSpeed(it) })
            }

            SettingsGroups(model.settings, { model.update(it) }, inGame = true)
            }
        }
    }
}


private val timeFmt = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())

@Composable
private fun SlotRow(title: String, view: SlotView?, canSave: Boolean, onSave: () -> Unit, onLoad: () -> Unit) {
    val filled = view != null && view.savedAt > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(72.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Rc.Chip)
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val img = view?.thumb
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp, 48.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Rc.Text, fontSize = 16.sp)
            Text(
                if (filled) timeFmt.format(Date(view!!.savedAt)) else "Trống",
                color = Rc.Muted,
                fontSize = 12.sp,
            )
        }
        if (canSave) {
            SlotButton("Lưu", enabled = true, onClick = onSave)
            Spacer(Modifier.width(8.dp))
        }
        SlotButton("Nạp", enabled = filled, onClick = onLoad)
    }
}

@Composable
private fun SlotButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Rc.Chip)
            .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(50))
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Rc.Text else Rc.Faint,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
