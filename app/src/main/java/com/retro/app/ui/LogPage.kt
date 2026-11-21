package com.retro.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.core.NativeCore

/** Nhật ký lõi: các dòng log gần nhất của lõi giả lập (kết nối mạng, lỗi BIOS...) để gỡ lỗi; có thể sao chép gửi đi. */
@Composable
fun LogPage(model: AppModel, onBack: () -> Unit) {
    var text by remember { mutableStateOf(readLog()) }
    val clipboard = LocalClipboardManager.current
    PageHeader("Nhật ký lõi", onBack)
    GroupCard {
        ActionRow("Làm mới", icon = "refresh", subtitle = "Lấy các dòng log mới nhất") { text = readLog() }
        RowDivider()
        ActionRow("Sao chép", icon = "copy", subtitle = "Chép toàn bộ nhật ký vào bộ nhớ tạm") {
            clipboard.setText(AnnotatedString(text.ifBlank { "(trống)" }))
            model.showToast("Đã sao chép nhật ký")
        }
        RowDivider()
        ActionRow("Xoá nhật ký", icon = "trash", danger = true) {
            NativeCore.nativeClearLog()
            text = readLog()
        }
    }
    SectionTitle("Nội dung")
    GroupCard {
        SelectionContainer {
            Text(
                text.ifBlank { "Chưa có dòng log nào. Mở một game NDS, thử kết nối online rồi quay lại đây bấm Làm mới." },
                color = Rc.Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
    Text(
        "Ký hiệu đầu dòng: E lỗi, W cảnh báo, I thông tin, D chi tiết, M thông báo của lõi, app là cài đặt app đã đưa cho lõi. " +
            "Nhật ký chỉ giữ 500 dòng gần nhất của lần mở game hiện tại.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )
}

private fun readLog(): String = try {
    NativeCore.nativeGetLog().trimEnd()
} catch (e: Throwable) {
    ""
}
