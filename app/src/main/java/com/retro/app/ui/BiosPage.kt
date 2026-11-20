package com.retro.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.BiosKind

/** Trang cài BIOS: chọn file BIOS/firmware lấy từ máy của bạn, app chép vào thư mục của lõi. */
@Composable
fun BiosPage(model: AppModel, onBack: () -> Unit) {
    var pending by remember { mutableStateOf<BiosKind?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val kind = pending
        pending = null
        if (kind != null && uri != null) model.installBios(kind, uri)
    }
    // Đọc lại trạng thái mỗi lần cài xong.
    @Suppress("UNUSED_VARIABLE") val version = model.biosVersion

    model.biosResetPrompt?.let { p ->
        AlertDialog(
            onDismissRequest = { model.dismissBiosReset() },
            title = { Text("Dùng BIOS mới?") },
            text = {
                Text(
                    "Có ${p.ids.size} game ${p.systemLabel} đang có điểm tự lưu nên sẽ tiếp tục chỗ cũ, không chạy qua BIOS mới. " +
                        "Khởi động lại từ đầu các game này? Save State do bạn tự lưu và file save trong game được giữ nguyên.",
                )
            },
            confirmButton = { TextButton(onClick = { model.confirmBiosReset() }) { Text("Khởi động lại") } },
            dismissButton = { TextButton(onClick = { model.dismissBiosReset() }) { Text("Giữ nguyên") } },
        )
    }

    PageHeader("BIOS", onBack)
    SectionTitle("PlayStation")
    GroupCard {
        BiosRow(BiosKind.PSX, model.bios.status(BiosKind.PSX)) { pending = BiosKind.PSX; picker.launch(arrayOf("*/*")) }
    }
    SectionTitle("Nintendo DS")
    GroupCard {
        val kinds = listOf(BiosKind.NDS7, BiosKind.NDS9, BiosKind.NDS_FW)
        kinds.forEachIndexed { i, k ->
            if (i > 0) RowDivider(inset = 16.dp)
            BiosRow(k, model.bios.status(k)) { pending = k; picker.launch(arrayOf("*/*")) }
        }
    }
    SectionTitle("Game Boy Advance")
    GroupCard {
        BiosRow(BiosKind.GBA, model.bios.status(BiosKind.GBA)) { pending = BiosKind.GBA; picker.launch(arrayOf("*/*")) }
    }
    Text(
        "BIOS là tuỳ chọn: thiếu thì lõi dùng bản giả lập. Chỉ dùng file BIOS lấy từ máy của chính bạn. Mở lại game để BIOS mới có hiệu lực. Mega Drive, NES và SNES không cần BIOS.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
}

@Composable
private fun BiosRow(kind: BiosKind, st: String?, onClick: () -> Unit) {
    // Trạng thái truyền vào làm tham số để dòng này vẽ lại ngay khi cài xong.
    val sub = if (st != null) "Đã cài: $st. Chạm để thay" else "Chưa cài. ${kind.hint}"
    ActionRow(kind.title, subtitle = sub, chevron = true, onClick = onClick)
}
