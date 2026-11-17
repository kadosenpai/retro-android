package com.retro.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel

/** Bảng màu Game Boy đời đầu: (tên preset của mGBA, tên hiển thị, 4 màu từ sáng đến tối). */
private class GbPalette(val key: String, val label: String, val colors: List<Long>)

private val palettes = listOf(
    GbPalette("Grayscale", "Xám (mặc định)", listOf(0xFFFFFFFF, 0xFFADADAD, 0xFF525252, 0xFF000000)),
    GbPalette("DMG Green", "Xanh lá cổ điển", listOf(0xFF8CA54A, 0xFF4A6B31, 0xFF294221, 0xFF192908)),
    GbPalette("GB Pocket", "Game Boy Pocket", listOf(0xFFA5B5A5, 0xFF849C84, 0xFF4A6352, 0xFF192921)),
    GbPalette("GB Light", "Game Boy Light", listOf(0xFF7BF7FF, 0xFF5AE6E6, 0xFF19B5AD, 0xFF007B73)),
)

/** Trang cài đặt Game Boy / Game Boy Color: bảng màu và viền Super Game Boy. */
@Composable
fun GameBoyPage(model: AppModel, onBack: () -> Unit) {
    val st = model.settings
    PageHeader("Game Boy", onBack)
    SectionTitle("Bảng màu")
    GroupCard {
        palettes.forEachIndexed { i, p ->
            if (i > 0) RowDivider(inset = 16.dp)
            PaletteRow(p, selected = st.gbPalette == p.key) {
                model.update { it.copy(gbPalette = p.key) }
                if (model.current != null) model.showToast("Reset game để áp dụng bảng màu")
            }
        }
    }
    Text(
        "Chỉ áp dụng cho game Game Boy đời đầu (đen trắng). Game Game Boy Color giữ màu gốc. Cần reset game hoặc mở lại game để áp dụng bảng màu mới.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
    SectionTitle("Super Game Boy")
    GroupCard {
        ToggleRow(
            "Hiện viền Super Game Boy",
            st.sgbBorder,
            icon = "image",
            subtitle = "Chỉ game hỗ trợ SGB. Có hiệu lực khi mở lại game",
        ) { v -> model.update { it.copy(sgbBorder = v) } }
    }
}

@Composable
private fun PaletteRow(p: GbPalette, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.clip(RoundedCornerShape(8.dp))) {
            p.colors.forEach { c -> Box(Modifier.size(width = 22.dp, height = 28.dp).background(Color(c))) }
        }
        Spacer(Modifier.width(14.dp))
        Text(p.label, color = Rc.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (selected) RetroIcon("check", tint = Rc.Text)
    }
}
