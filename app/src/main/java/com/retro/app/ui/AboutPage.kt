package com.retro.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MGBA_URL = "https://github.com/mgba-emu/mgba"
private const val MELONDS_URL = "https://github.com/libretro/melonDS"
private const val SNES9X_URL = "https://github.com/libretro/snes9x"
private const val GENESIS_URL = "https://github.com/libretro/Genesis-Plus-GX"
private const val PCSX_URL = "https://github.com/libretro/pcsx_rearmed"
private const val FCEUMM_URL = "https://github.com/libretro/libretro-fceumm"
private const val GPL_URL = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
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
    val nesVersion = remember {
        try {
            ctx.assets.open("fceumm-version.txt").bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "không rõ"
        }
    }
    val ndsVersion = remember {
        try {
            ctx.assets.open("melonds-version.txt").bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "không rõ"
        }
    }
    val snesVersion = remember {
        try {
            ctx.assets.open("snes9x-version.txt").bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "không rõ"
        }
    }
    val mdVersion = remember {
        try {
            ctx.assets.open("genesis-version.txt").bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "không rõ"
        }
    }
    val psxVersion = remember {
        try {
            ctx.assets.open("pcsx-version.txt").bufferedReader().use { it.readText().trim() }
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
        ActionRow("mGBA $coreVersion", subtitle = "Lõi giả lập GBA, Game Boy, Game Boy Color · MPL 2.0 · mã nguồn gốc, không sửa đổi", chevron = true) { open(MGBA_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("Nội dung giấy phép MPL 2.0", chevron = true) { open(MPL_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("FCEUmm $nesVersion", subtitle = "Lõi giả lập NES · GPL 2.0 · mã nguồn gốc, không sửa đổi", chevron = true) { open(FCEUMM_URL) }
        RowDivider(inset = 16.dp)
        RowDivider(inset = 16.dp)
        ActionRow("Snes9x $snesVersion", subtitle = "Lõi giả lập SNES · giấy phép phi thương mại của Snes9x · mã nguồn gốc, không sửa đổi", chevron = true) { open(SNES9X_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("melonDS $ndsVersion", subtitle = "Lõi giả lập Nintendo DS · GPL 3.0 · mã nguồn gốc, không sửa đổi", chevron = true) { open(MELONDS_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("Genesis Plus GX $mdVersion", subtitle = "Lõi giả lập Mega Drive · giấy phép phi thương mại của Genesis Plus GX · mã nguồn gốc, không sửa đổi", chevron = true) { open(GENESIS_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("PCSX ReARMed $psxVersion", subtitle = "Lõi giả lập PlayStation · GPL 2.0 · mã nguồn gốc, không sửa đổi", chevron = true) { open(PCSX_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("Nội dung giấy phép GPL 2.0", chevron = true) { open(GPL_URL) }
        RowDivider(inset = 16.dp)
        ActionRow("libretro-thumbnails", subtitle = "Nguồn ảnh bìa", chevron = true) { open(THUMBS_URL) }
    }
    Text(
        "Giao diện: AndroidX / Jetpack Compose (Apache 2.0), phông Inter (SIL OFL 1.1), biểu tượng Lucide (ISC). " +
            "Skin \"Atomic Advance – SmokeGray\", \"SNES Pocket (JoyCon)\", \"Atomic NDS 2 (Onyx Black)\" và \"DualShocked – Gray\" của Stanley Sy (starvingartist), ảnh thuộc về tác giả. " +
            "Game Boy Advance, Game Boy, NES, Super Nintendo và Nintendo DS là nhãn hiệu của Nintendo; PlayStation là nhãn hiệu của Sony; Mega Drive là nhãn hiệu của Sega. Retro không liên kết với các hãng này.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
}
