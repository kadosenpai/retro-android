package com.retro.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MGBA_URL = "https://github.com/mgba-emu/mgba"
private const val MPL_URL = "https://www.mozilla.org/en-US/MPL/2.0/"
private const val THUMBS_URL = "https://github.com/libretro-thumbnails/libretro-thumbnails"

/** Trang Giới thiệu và giấy phép phần mềm bên thứ ba. */
@Composable
fun AboutPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val appVersion = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }
    // Do CI ghi lúc build: "<nhánh/tag> (<commit ngắn>)"
    val coreVersion = remember {
        try {
            ctx.assets.open("mgba-version.txt").bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "không rõ"
        }
    }
    fun open(url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("‹ Giới thiệu", color = Rc.Text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    }

    GroupCard {
        Text(
            "Retro $appVersion\nTrình chơi game retro. Chỉ dùng ROM bạn sở hữu hợp pháp; app không kèm sẵn game nào.",
            color = Rc.Text,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }

    SectionTitle("Phần mềm bên thứ ba")
    GroupCard {
        Text(
            "mGBA — lõi giả lập Game Boy Advance\nPhiên bản: $coreVersion\n" +
                "Giấy phép: Mozilla Public License 2.0 (MPL 2.0).\n" +
                "Retro dùng mã nguồn mGBA gốc, không sửa đổi. Mã nguồn đầy đủ của đúng phiên bản này có tại " +
                "kho mGBA chính thức (xem link bên dưới).",
            color = Rc.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
        ActionRow("Mã nguồn mGBA") { open(MGBA_URL) }
        ActionRow("Nội dung giấy phép MPL 2.0") { open(MPL_URL) }
    }
    GroupCard(Modifier.padding(top = 12.dp)) {
        Text(
            "Ảnh bìa lấy từ kho libretro-thumbnails và cơ sở dữ liệu libretro-database (do cộng đồng libretro duy trì). " +
                "Thành phần giao diện dùng AndroidX / Jetpack Compose (Apache License 2.0). Phông chữ Inter (SIL Open Font License 1.1), biểu tượng theo bộ Lucide (ISC).",
            color = Rc.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
        ActionRow("libretro-thumbnails") { open(THUMBS_URL) }
    }
    GroupCard(Modifier.padding(top = 12.dp)) {
        Text(
            "Game Boy Advance là nhãn hiệu của Nintendo. Retro không liên kết hay được Nintendo xác nhận.",
            color = Rc.Faint,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}
