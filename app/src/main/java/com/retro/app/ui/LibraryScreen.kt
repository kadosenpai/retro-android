package com.retro.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.GameSystem
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: AppModel) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focus.clearFocus()
        keyboard?.hide()
    }
    var sortPill by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf(0) }
    var gameMenu by remember { mutableStateOf<RomInfo?>(null) } // game đang mở menu giữ lâu
    var menuAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var renameTarget by remember { mutableStateOf<RomInfo?>(null) }
    var deleteTarget by remember { mutableStateOf<RomInfo?>(null) }
    var consoleTarget by remember { mutableStateOf<RomInfo?>(null) }
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

    // Chọn thư mục game; nếu bấm quét khi chưa chọn thì chọn xong sẽ quét luôn.
    var scanAfterPick by remember { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val scan = scanAfterPick
        scanAfterPick = false
        if (uri != null) {
            model.setGameFolder(uri)
            if (scan) model.scanGameFolder()
        }
    }
    // Chưa có game nào: dấu cộng (thêm ROM). Đã có game: nút quét lại thư mục game.
    val hasAnyRom = model.roms.isNotEmpty()
    val addOrScan: () -> Unit = {
        if (!hasAnyRom) {
            picker.launch(arrayOf("*/*"))
        } else if (model.gameFolder == null) {
            scanAfterPick = true
            folderPicker.launch(null)
        } else {
            model.scanGameFolder()
        }
    }

    val tab = model.settings.tab
    // Chỉ hệ máy nào đã có game mới có trang riêng; thêm game của hệ mới thì trang mới xuất hiện.
    val tabs = remember(model.roms) {
        GameSystem.TABS.filter { (id, _) -> model.roms.any { GameSystem.fromId(it.system).tab == id } }
    }
    val tabsNow = rememberUpdatedState(tabs)
    // Vuốt trái/phải để chuyển giữa các hệ máy (mỗi hệ một trang).
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0),
        pageCount = { tabsNow.value.size },
    )
    LaunchedEffect(tab, tabs) {
        val i = tabs.indexOfFirst { it.first == tab }
        if (i >= 0 && i != pagerState.currentPage) pagerState.animateScrollToPage(i)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { p ->
            val id = tabsNow.value.getOrNull(p)?.first ?: return@collect
            if (id != model.settings.tab) model.update { it.copy(tab = id) }
        }
    }
    val sort = model.settings.sort
    // Màn ngang (thấp): gộp thanh trên cùng thành một hàng và bỏ bớt khoảng trống để game + tên hiện to, đủ.
    val compact = LocalConfiguration.current.screenHeightDp < 480
    // Màn đủ rộng thì hiện thêm số lượng game và biểu tượng sắp xếp, như bản web.

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .retroBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (compact) {
            // Một hàng duy nhất: cài đặt, ô tìm kiếm, thêm game.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn("settings", "Cài đặt", small = true) {
                    hideKeyboard()
                    settingsOpen = true
                }
                Spacer(Modifier.width(8.dp))
                SearchBox(query, { query = it }, 36.dp, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                IconBtn(if (hasAnyRom) "refresh" else "plus", if (hasAnyRom) "Quét lại thư mục game" else "Thêm game", small = true) { addOrScan() }
            }
        } else {
            // Thanh tiêu đề
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                    IconBtn("settings", "Cài đặt") {
                        hideKeyboard()
                        settingsOpen = true
                    }
                }
                Text(
                    "Thư viện",
                    modifier = Modifier.align(Alignment.Center),
                    color = Rc.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconBtn(if (hasAnyRom) "refresh" else "plus", if (hasAnyRom) "Quét lại thư mục game" else "Thêm game", Modifier.align(Alignment.CenterEnd)) { addOrScan() }
            }

            // Ô tìm kiếm
            SearchBox(query, { query = it }, 42.dp, Modifier.padding(horizontal = 16.dp).fillMaxWidth())
        }

        // Tiêu đề mục + sắp xếp
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
        if (model.roms.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = if (compact) 0.dp else 4.dp, bottom = if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    GameSystem.fromId(tabs.getOrNull(pagerState.currentPage)?.first ?: tab).label.uppercase(),
                    color = Rc.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(Ds.CardRadius))
                        .background(Rc.Glass)
                        .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(Ds.CardRadius))
                        .clickable { sortPill = true }
                        .padding(start = 12.dp, end = 6.dp, top = if (compact) 3.dp else 6.dp, bottom = if (compact) 3.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sort.label, color = Rc.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(2.dp))
                    RetroIcon("chevron-down", size = 18.dp, tint = Rc.Text)
                }
                SortMenu(sortPill, { sortPill = false }, sort) { k -> model.update { it.copy(sort = k) } }
            }
        }
        }

        if (tabs.isEmpty()) {
            EmptyState(hasAny = false, onAdd = { picker.launch(arrayOf("*/*")) })
        } else HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            beyondViewportPageCount = 0,
        ) { page ->
            val pageTab = tabs.getOrNull(page)?.first ?: tab
            val inTab = remember(model.roms, pageTab) { model.roms.filter { GameSystem.fromId(it.system).tab == pageTab } }
            val shown = remember(inTab, query, sort) {
                val q = query.trim().lowercase()
                val filtered = if (q.isEmpty()) inTab else inTab.filter { it.name.lowercase().contains(q) || it.id.startsWith(q) }
                when (sort) {
                    SortKey.RECENT -> filtered.sortedByDescending { maxOf(it.lastPlayedAt, it.addedAt) }
                    SortKey.NAME -> filtered.sortedBy { it.name.lowercase() }
                    SortKey.ADDED -> filtered.sortedByDescending { it.addedAt }
                }
            }
            if (shown.isEmpty()) {
                EmptyState(hasAny = inTab.isNotEmpty(), onAdd = { picker.launch(arrayOf("*/*")) })
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                // Màn ngang: cỡ bìa tính theo chiều cao còn lại để cả bìa, tên 2 dòng và ngày chơi luôn nằm gọn trong một hàng.
                val coverSize = maxOf(90.dp, minOf(170.dp, maxHeight - 84.dp))
                LazyVerticalGrid(
                    columns = if (compact) GridCells.FixedSize(coverSize) else GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(shown, key = { it.id }) { rom ->
                        // Giữ vị trí ô bằng biến thường (không phải state) để cuộn danh sách không bị recompose liên tục.
                        val tileBounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(1) }
                        Box(Modifier.onGloballyPositioned { tileBounds[0] = it.boundsInWindow() }) {
                            RomTile(
                                rom = rom,
                                cover = rememberCover(model, rom),
                                onOpen = { model.open(rom) },
                                onMenu = {
                                    hideKeyboard()
                                    menuAnchor = tileBounds[0]
                                    gameMenu = rom
                                },
                            )
                        }
                    }
                }
                }
            }
        }
        // Chấm báo đang ở trang (hệ máy) nào (chỉ hiện khi có từ hai hệ máy trở lên).
        if (tabs.size > 1) Row(
            Modifier.fillMaxWidth().padding(vertical = if (compact) 4.dp else 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, _ ->
                val on = i == pagerState.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (on) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (on) Color.White else Rc.Faint),
                )
            }
        }
    }

    // Cài đặt: trang toàn màn hình phủ lên thư viện (thư viện vẫn giữ nguyên vị trí cuộn bên dưới).
    // Back lùi từng lớp: trang con -> trang Cài đặt -> thư viện.
    if (settingsOpen) {
        androidx.activity.compose.BackHandler { if (settingsPage != 0) settingsPage = 0 else settingsOpen = false }
        Box(Modifier.fillMaxSize().retroBackground()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Ds.Pad)
                    .padding(top = Ds.GapSm, bottom = Ds.SectionGap),
            ) {
                SettingsContent(
                    model = model,
                    page = settingsPage,
                    onPage = { settingsPage = it },
                    onClose = { settingsOpen = false },
                    onDeleteAll = { deleteAllConfirm = true },
                    onPickFolder = {
                        scanAfterPick = false
                        folderPicker.launch(null)
                    },
                )
            }
            model.toast?.let { ToastBubble(it, Modifier.align(Alignment.BottomCenter)) }
        }
    }
    }

    // Giữ lâu vào một game: game phóng to ở giữa, bảng thao tác nằm dưới, phần còn lại mờ đi.
    gameMenu?.let { rom ->
        GameMenuOverlay(
            rom = rom,
            anchor = menuAnchor,
            cover = rememberCover(model, rom),
            onDismiss = { gameMenu = null },
            actions = listOfNotNull(
                GameAction("Đổi tên", "pencil") { gameMenu = null; renameTarget = rom },
                if (rom.system == GameSystem.NDS.id) {
                    GameAction("Chế độ máy: " + consoleLabel(rom.consoleMode), "settings") { gameMenu = null; consoleTarget = rom }
                } else null,
                GameAction("Tải ảnh bìa gốc", "image") { gameMenu = null; model.fetchCoverNow(rom) },
                GameAction("Chọn ảnh bìa từ máy", "image") {
                    gameMenu = null
                    coverTarget = rom
                    coverPicker.launch(arrayOf("image/*"))
                },
                GameAction("Xuất file .sav", "download") {
                    gameMenu = null
                    exportTarget = rom
                    exportLauncher.launch("${rom.name}.sav")
                },
                GameAction("Nhập file .sav", "upload") { gameMenu = null; importConfirm = rom },
                GameAction("Xoá game (kèm save)", "trash", danger = true) { gameMenu = null; deleteTarget = rom },
            ),
        )
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

    consoleTarget?.let { rom ->
        // Chọn lại chế độ máy cho game NDS: Tự động / DS / DSi.
        AlertDialog(
            onDismissRequest = { consoleTarget = null },
            title = { Text("Chế độ máy") },
            text = {
                Column {
                    listOf(
                        Triple("auto", "Tự động", "Game dành riêng cho DSi chạy DSi, còn lại chạy DS"),
                        Triple("ds", "Nintendo DS", "Nhẹ hơn, có Save State và tiếp tục từ lần chơi trước"),
                        Triple("dsi", "Nintendo DSi", "Cần đủ file DSi và BIOS DS, nặng hơn, không có Save State"),
                    ).forEach { (key, title, hint) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Ds.RadiusSm))
                                .clickable {
                                    model.setConsoleMode(rom, key)
                                    consoleTarget = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(selected = rom.consoleMode == key, onClick = null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(title, fontWeight = FontWeight.SemiBold)
                                Text(hint, fontSize = 12.sp, color = Rc.Muted)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { consoleTarget = null }) { Text("Huỷ") } },
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
