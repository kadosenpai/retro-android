package com.retro.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.NetTest
import com.retro.app.data.WfcServer
import com.retro.app.data.isIpv4
import kotlinx.coroutines.launch

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
        if (st.wfcServer == WfcServer.CUSTOM) {
            val bad = st.customDns.isNotBlank() && !isIpv4(st.customDns)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                OutlinedTextField(
                    value = st.customDns,
                    onValueChange = { v ->
                        model.update { it.copy(customDns = v.filter { c -> c.isDigit() || c == '.' }.take(15)) }
                    },
                    label = { Text("Địa chỉ DNS (vd. 95.217.77.181)") },
                    singleLine = true,
                    isError = bad,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (bad || st.customDns.isBlank()) {
                    Text(
                        if (bad) "Cần dạng a.b.c.d, mỗi số 0–255" else "Để trống thì dùng DNS có sẵn trong firmware",
                        color = if (bad) Rc.Danger else Rc.Faint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
    SectionTitle("Kiểm tra kết nối")
    ConnectionTest(st.customDns)
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

/** Thử từng máy chủ bằng mạng điện thoại: DNS có trả lời không, có kết nối được tới IP nhận về không. */
@Composable
private fun ConnectionTest(customDns: String) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf<List<Pair<String, NetTest.Result>>>(emptyList()) }
    GroupCard {
        ActionRow(
            if (running) "Đang kiểm tra…" else "Kiểm tra ngay",
            icon = "refresh",
            subtitle = "Thử từng máy chủ bằng mạng điện thoại, không cần mở game",
        ) {
            if (running) return@ActionRow
            running = true
            lines = emptyList()
            scope.launch {
                val targets = ArrayList<Pair<String, String>>()
                WfcServer.values().filter { it != WfcServer.CUSTOM }.forEach { targets.add(it.label to it.value) }
                if (isIpv4(customDns)) targets.add("DNS tự nhập" to customDns.trim())
                val acc = ArrayList<Pair<String, NetTest.Result>>()
                acc.add("DNS của điện thoại" to NetTest.testPhoneDns())
                lines = acc.toList()
                for ((name, ip) in targets) {
                    acc.add("$name ($ip)" to NetTest.testServer(ip))
                    lines = acc.toList()
                }
                running = false
            }
        }
        lines.forEach { (name, r) ->
            RowDivider(inset = 16.dp)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(name, color = Rc.Text, fontSize = 15.sp)
                Text(
                    (if (r.ok) "✓ " else "✗ ") + r.text,
                    color = if (r.ok) Rc.Muted else Rc.Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
    if (lines.isNotEmpty()) {
        Text(
            "✓ nghĩa là điện thoại tới được máy chủ. Máy chủ nào ✗ thì mạng nhà hoặc nhà mạng đang chặn nó: thử máy chủ khác hoặc đổi mạng.",
            color = Rc.Faint,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
        )
    }
}
