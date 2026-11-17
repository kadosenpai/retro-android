package com.retro.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.CheatItem
import com.retro.app.data.GameSystem

/** Trang Cheat trong menu ▲: danh sách, bật/tắt, xoá và thêm mới. */
@Composable
fun CheatsPage(model: AppModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PageHeader("Cheat", onBack)

    GroupCard {
        if (model.cheats.isEmpty()) {
            Text(
                "Chưa có cheat nào cho game này.",
                color = Rc.Muted,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            )
        } else {
            model.cheats.forEach { c -> CheatRow(c, model) }
        }
    }

    SectionTitle("Thêm cheat")
    GroupCard {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên (tuỳ chọn)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    error = null
                },
                label = { Text("Mã cheat") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Text(it, color = Rc.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        ActionRow("Thêm cheat") {
            val e = model.addCheat(name, code)
            if (e == null) {
                name = ""
                code = ""
                error = null
            } else {
                error = e
            }
        }
    }
    val gb = GameSystem.fromId(model.current?.system).isGameBoy
    Text(
        if (gb) "Mã GameShark (8 ký tự, vd. 01FF16D1) hoặc Game Genie (XXX-XXX-XXX) cho Game Boy / Game Boy Color. Nhiều mã cách nhau bằng dòng mới hoặc dấu cách. Cheat có thể làm game lỗi, hãy lưu trạng thái trước khi thử.\n\nApp chỉ biết lõi có đọc được mã hay không, không biết mã có tác dụng trong game này."
        else "Mã GameShark / Action Replay cho GBA: mỗi dòng 12 hoặc 16 ký tự (0-9, A-F). Một số mã cần đúng loại mới chạy được. Cheat có thể làm game lỗi, hãy lưu trạng thái trước khi thử.\n\nApp chỉ biết lõi có đọc được mã hay không, không biết mã có tác dụng trong game này.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
    )
}

@Composable
private fun CheatRow(c: CheatItem, model: AppModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(c.name, color = Rc.Text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                c.code.replace('\n', ' '),
                color = Rc.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        RetroSwitch(c.enabled) { model.toggleCheat(c.id, it) }
        Spacer(Modifier.width(10.dp))
        Text(
            "Xoá",
            color = Rc.Danger,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable { model.deleteCheat(c.id) }
                .padding(horizontal = 6.dp, vertical = 10.dp),
        )
    }
}
