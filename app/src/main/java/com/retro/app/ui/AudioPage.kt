package com.retro.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.AudioLatency

/** Trang cài đặt âm thanh: bật/tắt tiếng và độ trễ âm thanh. */
@Composable
fun AudioPage(model: AppModel, onBack: () -> Unit) {
    val st = model.settings
    PageHeader("Âm thanh", onBack)
    SectionTitle("Âm thanh")
    GroupCard {
        ToggleRow("Bật âm thanh", st.sound, icon = "volume") { v -> model.update { it.copy(sound = v) } }
    }
    SectionTitle("Độ trễ âm thanh")
    GroupCard {
        AudioLatency.values().forEachIndexed { i, l ->
            if (i > 0) RowDivider(inset = 16.dp)
            LatencyRow(l, selected = st.audioLatency == l) {
                model.update { it.copy(audioLatency = l) }
                if (model.current != null) model.showToast("Mở lại game để áp dụng đầy đủ")
            }
        }
    }
    Text(
        "Nếu tiếng bị trễ so với hình, chọn Phản hồi nhanh. Nếu tiếng bị lẹt xẹt hoặc đứt quãng, chọn Tiếng ổn định. Tai nghe Bluetooth tự trễ thêm, app không giảm được.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
}

@Composable
private fun LatencyRow(l: AudioLatency, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(l.label, color = Rc.Text, fontSize = 16.sp)
            Text(l.hint, color = Rc.Faint, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) RetroIcon("check", tint = Rc.Text)
    }
}
