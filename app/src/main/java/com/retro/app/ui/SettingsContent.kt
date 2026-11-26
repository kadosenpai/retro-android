package com.retro.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel

/** Menu nhỏ khi giữ lâu vào một game. */
/**
 * Nội dung trang Cài đặt: trang chính và các trang con (Tay cầm, Âm thanh, Hình ảnh, ...).
 * page: 0 = chính, 1 = Tay cầm, 2 = Giới thiệu, 3 = Âm thanh, 4 = Cảm ứng & rung, 5 = Dữ liệu, 6 = Game Boy, 7 = BIOS, 8 = Hình ảnh.
 */
@Composable
internal fun SettingsContent(
    model: AppModel,
    page: Int,
    onPage: (Int) -> Unit,
    onClose: () -> Unit,
    onDeleteAll: () -> Unit,
    onPickFolder: () -> Unit,
) {
    val st = model.settings
    when (page) {
        1 -> ControllerPage(model) { onPage(0) }
        2 -> AboutPage { onPage(0) }
        6 -> GameBoyPage(model) { onPage(0) }
        7 -> BiosPage(model) { onPage(0) }
        8 -> DisplayPage(model) { onPage(0) }
        3 -> AudioPage(model) { onPage(0) }
        4 -> {
            PageHeader("Cảm ứng & rung") { onPage(0) }
            Spacer(Modifier.height(Ds.SectionGap))
            GroupCard {
                ToggleRow("Hiện nút ảo", st.showPad, icon = "dpad") { v -> model.update { it.copy(showPad = v) } }
                RowDivider()
                ToggleRow("Rung khi nhấn nút", st.haptics, icon = "phone") { v -> model.update { it.copy(haptics = v) } }
            }
        }
        5 -> {
            PageHeader("Dữ liệu") { onPage(0) }
            Spacer(Modifier.height(Ds.SectionGap))
            GroupCard {
                ActionRow("Xoá hết game", danger = true, icon = "trash", subtitle = "Kèm Save State và file .sav") {
                    onDeleteAll()
                }
            }
            Text(
                "Tất cả dữ liệu nằm trên máy này.",
                color = Rc.Faint,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
            )
        }
        else -> {
            PageHeader("Cài đặt", onClose)
            SectionTitle("Điều khiển")
            GroupCard {
                ActionRow("Tay cầm", icon = "gamepad", chevron = true) { onPage(1) }
                RowDivider()
                ActionRow("Cảm ứng & rung", icon = "phone", chevron = true) { onPage(4) }
            }
            SectionTitle("Trình giả lập")
            GroupCard {
                ActionRow("Hình ảnh", icon = "image", chevron = true, subtitle = model.settings.filter.label) { onPage(8) }
                RowDivider()
                ActionRow("Âm thanh", icon = "volume", chevron = true) { onPage(3) }
                RowDivider()
                ActionRow("Game Boy", icon = "image", chevron = true) { onPage(6) }
                RowDivider()
                ActionRow("BIOS", icon = "database", chevron = true) { onPage(7) }
            }
            SectionTitle("Dữ liệu")
            GroupCard {
                ActionRow(
                    "Thư mục game",
                    icon = "folder",
                    subtitle = model.gameFolderName ?: "Chưa chọn. Quét thư mục để thêm game vào thư viện",
                    chevron = true,
                ) {
                    onPickFolder()
                }
                RowDivider()
                ActionRow("Dữ liệu", icon = "database", chevron = true) { onPage(5) }
            }
            SectionTitle("Khác")
            GroupCard {
                ActionRow("Giới thiệu và giấy phép", icon = "file-text", chevron = true) { onPage(2) }
            }
        }
    }
}
