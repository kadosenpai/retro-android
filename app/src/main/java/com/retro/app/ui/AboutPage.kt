package com.retro.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
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

    PageHeader("Giới thiệu", onBack)

    GroupCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Retro $appVersion", color = Rc.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Trình chơi game retro. Chỉ dùng ROM bạn sở hữu hợp pháp; app không kèm game nào.",
                color = Rc.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }

    SectionTitle("Giấy phép")
    GroupCard {
        ActionRow("mGBA $coreVersion", subtitle = "Lõi giả lập GBA · MPL 2.0 · mã nguồn gốc, không sửa đổi", chevron = true) { open(MGBA_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("Nội dung giấy phép MPL 2.0", chevron = true) { open(MPL_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("libretro-thumbnails", subtitle = "Nguồn ảnh bìa", chevron = true) { open(THUMBS_URL) }
    }
    Text(
        "Giao diện: AndroidX / Jetpack Compose (Apache 2.0), phông Inter (SIL OFL 1.1), biểu tượng Lucide (ISC). " +
            "Skin \"Atomic Advance – SmokeGray\" của Stanley Sy (starvingartist), ảnh thuộc về tác giả. " +
            "Game Boy Advance là nhãn hiệu của Nintendo; Retro không liên kết với Nintendo.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
}
