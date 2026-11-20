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
import com.retro.app.data.WfcServer

/** Trang Wi-Fi của máy NDS: bật/tắt mạng và chọn máy chủ thay thế cho Nintendo Wi-Fi Connection. */
@Composable
fun WifiPage(model: AppModel, onBack: () -> Unit) {
    val st = model.settings
    fun applied() {
        if (model.current != null) model.showToast("Mở lại game để áp dụng")
    }
    PageHeader("Wi-Fi (NDS)", onBack)
    SectionTitle("Mạng")
    GroupCard {
        ToggleRow("Bật Wi-Fi", st.wifi, icon = "wifi", subtitle = "Dùng mạng của điện thoại cho game NDS có chơi online") { v ->
            model.update { it.copy(wifi = v) }
            applied()
        }
    }
    SectionTitle("Máy chủ")
    GroupCard {
        WfcServer.values().forEachIndexed { i, srv ->
            if (i > 0) RowDivider(inset = 16.dp)
            ServerRow(srv, selected = st.wfcServer == srv) {
                model.update { it.copy(wfcServer = srv) }
                applied()
            }
        }
    }
    Text(
        "Máy chủ gốc của Nintendo đã đóng, nên chơi online phải dùng máy chủ cộng đồng. Game nào chơi được còn tuỳ máy chủ, " +
            "không phải game nào cũng hỗ trợ. Một số nhà mạng chặn các máy chủ này. Các máy chủ này không liên quan đến app hay Nintendo, " +
            "hãy tôn trọng người chơi khác và tuân theo luật của họ. Đổi cài đặt có hiệu lực khi mở lại game.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
}

@Composable
private fun ServerRow(srv: WfcServer, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(srv.label, color = Rc.Text, fontSize = 16.sp)
            Text(srv.hint, color = Rc.Faint, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) RetroIcon("check", tint = Rc.Text)
    }
}
