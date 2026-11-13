package com.retro.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Menu", color = Rc.Text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
            GroupCard {
                ActionRow("Tiếp tục", onClick = onClose)
                ActionRow("Reset game") {
                    model.resetGame()
                    onClose()
                }
                ActionRow("Thoát về Thư viện") {
                    onClose()
                    model.exitToLibrary()
                }
            }

            SectionTitle("Tốc độ game")
            GroupCard {
                ChipRow(SPEEDS, model.speed, { speedLabel(it) }, { model.changeSpeed(it) })
            }

            SettingsGroups(model.settings, { model.update(it) }, inGame = true)
        }
    }
}
