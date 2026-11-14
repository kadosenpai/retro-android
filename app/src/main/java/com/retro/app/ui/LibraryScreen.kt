package com.retro.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.BitmapFactory
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: AppModel) {
    var query by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<RomInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<RomInfo?>(null) }

    var exportTarget by remember { mutableStateOf<RomInfo?>(null) }
    var importTarget by remember { mutableStateOf<RomInfo?>(null) }
    var importConfirm by remember { mutableStateOf<RomInfo?>(null) }

    var coverTarget by remember { mutableStateOf<RomInfo?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val rom = coverTarget
        coverTarget = null
        if (rom != null && uri != null) model.pickCover(rom, uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val rom = exportTarget
        exportTarget = null
        if (rom != null && uri != null) model.exportSave(rom, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val rom = importTarget
        importTarget = null
        if (rom != null && uri != null) model.importSave(rom, uri)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        model.addRoms(uris)
    }

    val sort = model.settings.sort
    val shown = remember(model.roms, query, sort) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) model.roms else model.roms.filter { it.name.lowercase().contains(q) || it.id.startsWith(q) }
        when (sort) {
            SortKey.RECENT -> filtered.sortedByDescending { maxOf(it.lastPlayedAt, it.addedAt) }
            SortKey.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortKey.ADDED -> filtered.sortedByDescending { it.addedAt }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Rc.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Thanh tiêu đề
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            RoundIcon(Modifier.align(Alignment.CenterStart)) { settingsOpen = true }
            Text(
                "Thư viện",
                modifier = Modifier.align(Alignment.Center),
                color = Rc.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { picker.launch(arrayOf("*/*")) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm băng", tint = Rc.Text, modifier = Modifier.size(30.dp))
            }
        }

        // Ô tìm kiếm
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Rc.Card)
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Rc.Muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Rc.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Tìm tên ROM hoặc mã ID...", color = Rc.Faint, fontSize = 16.sp)
                        inner()
                    }
                },
            )
        }

        // Tiêu đề mục + sắp xếp
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("KỆ BĂNG CỦA BẠN", color = Rc.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Rc.Card)
                        .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp))
                        .clickable { sortMenu = true }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sort.label, color = Rc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Rc.Text, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortKey.values().forEach { k ->
                        DropdownMenuItem(
                            text = { Text(k.label) },
                            onClick = {
                                model.update { it.copy(sort = k) }
                                sortMenu = false
                            },
                        )
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            EmptyState(hasAny = model.roms.isNotEmpty())
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.id }) { rom ->
                    RomCard(
                        rom = rom,
                        onOpen = { model.open(rom) },
                        onRename = { renameTarget = rom },
                        onDelete = { deleteTarget = rom },
                        onExportSave = {
                            exportTarget = rom
                            exportLauncher.launch("${rom.name}.sav")
                        },
                        onImportSave = { importConfirm = rom },
                        onAutoCover = { model.fetchCoverNow(rom) },
                        onPickCover = {
                            coverTarget = rom
                            coverPicker.launch(arrayOf("image/*"))
                        },
                        cover = rememberCover(model, rom),
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = state,
            containerColor = Rc.Sheet,
            contentColor = Rc.Text,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                var settingsPage by remember { mutableStateOf(0) }
                if (settingsPage == 1) {
                    ControllerPage(model) { settingsPage = 0 }
                } else if (settingsPage == 2) {
                    AboutPage { settingsPage = 0 }
                } else {
                Text("Cài đặt", color = Rc.Text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                SettingsGroups(model.settings, { model.update(it) }, inGame = false)
                SectionTitle("Tay cầm")
                GroupCard { ActionRow("Kết nối và gán nút tay cầm") { settingsPage = 1 } }
                SectionTitle("Giới thiệu")
                GroupCard { ActionRow("Giới thiệu và giấy phép") { settingsPage = 2 } }
                }
            }
        }
    }

    renameTarget?.let { rom ->
        var text by remember(rom.id) { mutableStateOf(rom.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Đổi tên") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    model.rename(rom, text)
                    renameTarget = null
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Huỷ") } },
        )
    }

    importConfirm?.let { rom ->
        AlertDialog(
            onDismissRequest = { importConfirm = null },
            title = { Text("Nhập save cho \"${rom.name}\"?") },
            text = {
                Text(
                    "Save hiện tại của game này sẽ bị ghi đè bằng file .sav bạn chọn, " +
                        "và trạng thái \"Tự động\" sẽ bị xoá. Các ô Lưu 1–4 giữ nguyên.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importTarget = rom
                    importConfirm = null
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Chọn file .sav") }
            },
            dismissButton = { TextButton(onClick = { importConfirm = null }) { Text("Huỷ") } },
        )
    }

    deleteTarget?.let { rom ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá \"${rom.name}\"?") },
            text = { Text("Xoá cả file ROM và dữ liệu lưu của game này khỏi máy. Không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    model.delete(rom)
                    deleteTarget = null
                }) { Text("Xoá", color = Rc.Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") } },
        )
    }
}

/** Đọc ảnh bìa của game từ bộ nhớ (ngoài luồng chính); null nếu chưa có. */
@Composable
private fun rememberCover(model: AppModel, rom: RomInfo): ImageBitmap? {
    val img by produceState<ImageBitmap?>(null, rom.id, rom.coverAt) {
        value = if (rom.coverAt == 0L) {
            null
        } else {
            withContext(Dispatchers.IO) {
                model.store.readCover(rom.id)?.let { b ->
                    runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()
                }
            }
        }
    }
    return img
}

@Composable
private fun RoundIcon(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = Rc.Text, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun EmptyState(hasAny: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (hasAny) "Không tìm thấy băng nào" else "Chưa có băng nào",
            color = Rc.Text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasAny) "Thử từ khoá khác."
            else "Bấm dấu + ở góc trên để thêm file .gba của bạn. Chỉ dùng ROM bạn sở hữu hợp pháp; dữ liệu nằm trên máy này.",
            color = Rc.Muted,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun RomCard(
    rom: RomInfo,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExportSave: () -> Unit,
    onImportSave: () -> Unit,
    onAutoCover: () -> Unit,
    onPickCover: () -> Unit,
    cover: ImageBitmap?,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Rc.Card)
            .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(26.dp))
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF101114))
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp),
                )
            } else {
                Text(
                    rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                    color = Rc.Muted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rom.name,
                color = Rc.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatSize(rom.size)} · ${relativeDay(maxOf(rom.lastPlayedAt, rom.addedAt))}",
                color = Rc.Muted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { menu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = Rc.Text)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Đổi tên") }, onClick = {
                    menu = false
                    onRename()
                })
                DropdownMenuItem(text = { Text("Tải ảnh bìa gốc") }, onClick = {
                    menu = false
                    onAutoCover()
                })
                DropdownMenuItem(text = { Text("Chọn ảnh bìa từ máy") }, onClick = {
                    menu = false
                    onPickCover()
                })
                DropdownMenuItem(text = { Text("Xuất save (.sav)") }, onClick = {
                    menu = false
                    onExportSave()
                })
                DropdownMenuItem(text = { Text("Nhập save (.sav)") }, onClick = {
                    menu = false
                    onImportSave()
                })
                DropdownMenuItem(text = { Text("Xoá khỏi máy", color = Rc.Danger) }, onClick = {
                    menu = false
                    onDelete()
                })
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1024 * 1024) String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    else "${maxOf(1L, bytes / 1024)} KB"

private fun relativeDay(ts: Long): String {
    if (ts <= 0L) return "Chưa chơi"
    val days = ((System.currentTimeMillis() - ts) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Hôm nay"
        days == 1 -> "Hôm qua"
        days < 30 -> "$days ngày trước"
        else -> "Lâu rồi"
    }
}
