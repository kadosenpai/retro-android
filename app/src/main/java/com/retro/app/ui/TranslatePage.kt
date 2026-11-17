package com.retro.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel

/** Trang Cài đặt → Dịch game: dán API key Gemini, chọn model, hướng dẫn lấy key. */
@Composable
fun TranslatePage(model: AppModel, onBack: () -> Unit) {
    val st = model.settings
    PageHeader("Dịch game", onBack)

    SectionTitle("Gemini")
    GroupCard {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = st.geminiKey,
                onValueChange = { v -> model.update { s -> s.copy(geminiKey = v.trim()) } },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = st.geminiModel,
                onValueChange = { v -> model.update { s -> s.copy(geminiModel = v.trim()) } },
                label = { Text("Tên model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Text(
        "Key chỉ lưu trên máy này và chỉ được gửi tới Google khi dịch. Tên model có thể đổi nếu Google ngừng model cũ (xem danh sách trong Google AI Studio).",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
    )

    SectionTitle("Cách lấy API key")
    GroupCard {
        Text(
            "1. Mở aistudio.google.com và đăng nhập bằng tài khoản Google.\n" +
                "2. Bấm \"Get API key\" (Lấy khoá API), rồi \"Create API key\".\n" +
                "3. Chọn hoặc tạo một dự án, bấm tạo. Sao chép chuỗi key (bắt đầu bằng AIza…).\n" +
                "4. Quay lại đây và dán vào ô API key.",
            color = Rc.Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(16.dp),
        )
    }

    SectionTitle("Game hỗ trợ")
    GroupCard {
        Text(
            "Pokémon Emerald (bản Mỹ, tiếng Anh). Giữ lâu vào băng game rồi chọn \"Dịch sang tiếng Việt\". " +
                "Nên để app mở cho tới khi dịch xong; nếu bị ngắt, bấm dịch lại để tiếp tục từ chỗ cũ.",
            color = Rc.Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(16.dp),
        )
    }
}
