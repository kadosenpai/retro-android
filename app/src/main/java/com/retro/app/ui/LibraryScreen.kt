package com.retro.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: AppModel) {
    var query by remember { mutableStateOf("") }
    var sortTop by remember { mutableStateOf(false) }
    var sortPill by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var gameMenu by remember { mutableStateOf<RomInfo?>(null) }
    var renameTarget by remember { mutableStateOf<RomInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<RomInfo?>(null) }
    var deleteAllConfirm by remember { mutableStateOf(false) }

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
    // Màn đủ rộng thì hiện thêm số lượng băng và biểu tượng sắp xếp, như bản web.
    val roomy = LocalConfiguration.current.screenWidthDp > 420
    // Làm mờ phần nền phía sau khi có bảng mở (Android 12 trở lên).
    val behindSheet = settingsOpen || gameMenu != null
    val blurRadius = animateDpAsState(if (behindSheet) 18.dp else 0.dp, label = "blur").value

    Column(
        Modifier
            .fillMaxSize()
            .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            .retroBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Thanh tiêu đề
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                IconBtn("settings", "Cài đặt") { settingsOpen = true }
                Box {
                    IconBtn("menu", "Sắp xếp") { sortTop = true }
                    SortMenu(sortTop, { sortTop = false }, sort) { k -> model.update { it.copy(sort = k) } }
                }
            }
            Text(
                "Thư viện",
                modifier = Modifier.align(Alignment.Center),
                color = Rc.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            IconBtn("plus", "Thêm băng", Modifier.align(Alignment.CenterEnd)) { picker.launch(arrayOf("*/*")) }
        }

        // Ô tìm kiếm
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Rc.Glass)
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RetroIcon("search", size = 22.dp, tint = Rc.Muted)
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Rc.Text, fontSize = 16.sp, fontFamily = InterFamily),
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
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Rc.Line))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RetroIcon("layers", size = 22.dp, tint = Rc.Text)
            Spacer(Modifier.width(8.dp))
            Text(
                "KỆ BĂNG CỦA BẠN",
                color = Rc.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (roomy) {
                Spacer(Modifier.width(8.dp))
                Text("(${model.roms.size} băng)", color = Rc.Muted, fontSize = 14.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (roomy) {
                RetroIcon("sort", tint = Rc.Muted)
                Spacer(Modifier.width(10.dp))
            }
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Rc.Glass)
                        .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp))
                        .clickable { sortPill = true }
                        .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sort.label, color = Rc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    RetroIcon("chevron-down", size = 20.dp, tint = Rc.Text)
                }
                SortMenu(sortPill, { sortPill = false }, sort) { k -> model.update { it.copy(sort = k) } }
            }
        }

        if (shown.isEmpty()) {
            EmptyState(hasAny = model.roms.isNotEmpty(), onAdd = { picker.launch(arrayOf("*/*")) })
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
                        onMenu = { gameMenu = rom },
                        cover = rememberCover(model, rom),
                    )
                }
            }
        }
    }

    // Bảng của một game (Xuất/Nhập .sav, ảnh bìa, đổi tên, xoá)
    gameMenu?.let { rom ->
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { gameMenu = null },
            sheetState = state,
            containerColor = Rc.Sheet,
            contentColor = Rc.Text,
            scrimColor = Color(0x8C000000),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            dragHandle = null,
        ) {
            SheetWindowBlur()
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 24.dp),
            ) {
                SheetHeader(rom.name) { gameMenu = null }
                Spacer(Modifier.height(14.dp))
                GroupCard {
                    ActionRow("Xuất file .sav", icon = "download") {
                        gameMenu = null
                        exportTarget = rom
                        exportLauncher.launch("${rom.name}.sav")
                    }
                    RowDivider()
                    ActionRow("Nhập file .sav", icon = "upload") {
                        gameMenu = null
                        importConfirm = rom
                    }
                    RowDivider()
                    ActionRow("Tải ảnh bìa gốc", icon = "image") {
                        gameMenu = null
                        model.fetchCoverNow(rom)
                    }
                    RowDivider()
                    ActionRow("Chọn ảnh bìa từ máy", icon = "image") {
                        gameMenu = null
                        coverTarget = rom
                        coverPicker.launch(arrayOf("image/*"))
                    }
                    RowDivider()
                    ActionRow("Đổi tên", icon = "pencil") {
                        gameMenu = null
                        renameTarget = rom
                    }
                    RowDivider()
                    ActionRow("Xoá băng (kèm save)", danger = true, icon = "trash") {
                        gameMenu = null
                        deleteTarget = rom
                    }
                }
            }
        }
    }

    // Cài đặt
    if (settingsOpen) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = state,
            containerColor = Rc.Sheet,
            contentColor = Rc.Text,
            scrimColor = Color(0x8C000000),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            dragHandle = null,
        ) {
            SheetWindowBlur()
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 24.dp),
            ) {
                var settingsPage by remember { mutableStateOf(0) }
                val st = model.settings
                if (settingsPage == 1) {
                    ControllerPage(model) { settingsPage = 0 }
                } else if (settingsPage == 2) {
                    AboutPage { settingsPage = 0 }
                } else {
                    SheetHeader("Cài đặt") { settingsOpen = false }
                    SectionTitle("Âm thanh")
                    GroupCard {
                        ToggleRow("Bật âm thanh", st.sound, icon = "volume") { v -> model.update { it.copy(sound = v) } }
                    }
                    SectionTitle("Điều khiển")
                    GroupCard {
                        ToggleRow("Ẩn nút ảo", !st.showPad, icon = "pointer") { v -> model.update { it.copy(showPad = !v) } }
                        RowDivider()
                        ToggleRow("Rung khi nhấn nút", st.haptics, icon = "phone") { v -> model.update { it.copy(haptics = v) } }
                        RowDivider()
                        ActionRow("Tay cầm", icon = "gamepad", chevron = true) { settingsPage = 1 }
                    }
                    SectionTitle("Dữ liệu")
                    GroupCard {
                        ActionRow("Xóa hết game (kèm Save State & .sav)", danger = true, icon = "trash", chevron = true) {
                            deleteAllConfirm = true
                        }
                    }
                    SectionTitle("Giới thiệu")
                    GroupCard {
                        ActionRow("Giới thiệu và giấy phép", icon = "info", chevron = true) { settingsPage = 2 }
                    }
                    Text(
                        "Tay cầm ngoài sẽ tự nhận khi cắm USB hoặc kết nối Bluetooth. Tất cả dữ liệu nằm trên máy này.",
                        color = Rc.Muted,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 18.dp),
                    )
                }
            }
        }
    }

    if (deleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { deleteAllConfirm = false },
            title = { Text("Xoá hết game?") },
            text = {
                Text("Xoá toàn bộ ROM cùng ảnh bìa, cheat, Save State và file .sav của tất cả game trong app này. Không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteAllConfirm = false
                    settingsOpen = false
                    model.deleteAll()
                }) { Text("Xoá hết", color = Rc.Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteAllConfirm = false }) { Text("Huỷ") } },
        )
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


/** Nút tròn có biểu tượng (cài đặt, sắp xếp, thêm băng). */
@Composable
private fun IconBtn(icon: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon(icon, size = 26.dp, tint = Rc.Text)
    }
}

/** Menu chọn kiểu sắp xếp: dấu ✓ ở kiểu đang dùng, dấu − ở các kiểu còn lại. */
@Composable
private fun SortMenu(expanded: Boolean, onDismiss: () -> Unit, current: SortKey, onPick: (SortKey) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortKey.values().forEach { k ->
            DropdownMenuItem(
                leadingIcon = { RetroIcon(if (k == current) "check" else "minus", size = 20.dp, tint = Rc.Muted) },
                text = { Text(k.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
                onClick = {
                    onPick(k)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun EmptyState(hasAny: Boolean, onAdd: () -> Unit) {
    val cfg = LocalConfiguration.current
    val wide = cfg.screenWidthDp >= 600
    val compact = cfg.screenHeightDp < 480
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (hasAny) {
            Text(
                "Không tìm thấy băng nào",
                color = Rc.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
            )
            Text(
                "Thử từ khoá khác.",
                color = Rc.Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            return@Column
        }
        val shape = RoundedCornerShape(28.dp)
        val cardMod = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Rc.Glass)
            .border(BorderStroke(1.dp, Rc.Stroke), shape)
        val desc = "Bấm dấu + để thêm file ROM .gba của bạn. Chỉ dùng ROM bạn sở hữu hợp pháp; dữ liệu nằm trên máy này."
        if (compact) {
            // Màn ngang thấp: thẻ gọn một hàng để nằm vừa màn hình, không phải vuốt.
            Row(cardMod.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroIcon("layers", size = 40.dp, tint = Rc.Text)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa có băng nào", color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(desc, color = Rc.Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(18.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        } else if (wide) {
            Row(cardMod.padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroIcon("layers", size = 56.dp, tint = Rc.Text)
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa có băng nào", color = Rc.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(desc, color = Rc.Muted, fontSize = 16.sp)
                }
                Spacer(Modifier.width(22.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        } else {
            Column(cardMod.padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RetroIcon("layers", size = 56.dp, tint = Rc.Text)
                Spacer(Modifier.height(18.dp))
                Text("Chưa có băng nào", color = Rc.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(desc, color = Rc.Muted, fontSize = 17.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        }
    }
}

@Composable
private fun RomCard(
    rom: RomInfo,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    cover: ImageBitmap?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Rc.Glass)
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
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            RetroIcon("dots", tint = Rc.Text)
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
