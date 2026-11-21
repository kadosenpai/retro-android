package com.retro.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.DisplayFilter
import com.retro.app.data.ScaleMode

/** Trang cài đặt hình ảnh: bộ lọc (thô, sắc nét, LCD, CRT), cường độ và cách phóng to. */
@Composable
fun DisplayPage(model: AppModel, onBack: () -> Unit) {
    val st = model.settings
    PageHeader("Hình ảnh", onBack)
    SectionTitle("Bộ lọc hình")
    GroupCard {
        DisplayFilter.values().forEachIndexed { i, f ->
            if (i > 0) RowDivider(inset = 16.dp)
            ChoiceRow(f.label, f.hint, selected = st.filter == f) { model.update { it.copy(filter = f) } }
        }
    }
    if (st.filter == DisplayFilter.LCD || st.filter == DisplayFilter.CRT) {
        SectionTitle("Cường độ")
        GroupCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cường độ ${st.filter.label}", color = Rc.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("${Math.round(st.filterLevel * 100)}%", color = Rc.Muted, fontSize = 14.sp)
                }
                Slider(
                    value = st.filterLevel,
                    onValueChange = { model.previewFilterLevel(it) },
                    onValueChangeFinished = { model.commitSettings() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Rc.GlassStrong,
                    ),
                )
            }
        }
    }
    SectionTitle("Kích thước")
    GroupCard {
        ScaleMode.values().forEachIndexed { i, m ->
            if (i > 0) RowDivider(inset = 16.dp)
            ChoiceRow(m.label, m.hint, selected = st.scaleMode == m) { model.update { it.copy(scaleMode = m) } }
        }
    }
    Text(
        "Bộ lọc LCD và CRT đẹp nhất khi phóng nguyên số. Với NDS dùng skin, kích thước hai màn do skin quyết định.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
}

@Composable
private fun ChoiceRow(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Rc.Text, fontSize = 16.sp)
            Text(hint, color = Rc.Faint, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) RetroIcon("check", tint = Rc.Text)
    }
}
