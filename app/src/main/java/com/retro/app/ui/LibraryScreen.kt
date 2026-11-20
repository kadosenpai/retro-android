package com.retro.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.GameSystem
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var gameMenu by remember { mutableStateOf<RomInfo?>(null) } // game đang mở menu giữ lâu
    var menuAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
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
    // Làm mờ phần nền phía sau khi có bảng mở (Android 12 trở lên).
    val behindSheet = settingsOpen
    val blurRadius = if (behindSheet) 16.dp else 0.dp

    Column(
        Modifier
            .fillMaxSize()
            .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(Rc.Glass)
                        .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp))
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

    // Giữ lâu vào một game: game phóng to ở giữa, bảng thao tác nằm dưới, phần còn lại mờ đi.
    gameMenu?.let { rom ->
        GameMenuOverlay(
            rom = rom,
            anchor = menuAnchor,
            cover = rememberCover(model, rom),
            onDismiss = { gameMenu = null },
            actions = listOf(
                GameAction("Đổi tên", "pencil") { gameMenu = null; renameTarget = rom },
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

    // Cài đặt
    if (settingsOpen) {
      // key theo chiều xoay: tạo lại bảng để nó đo lại, tránh bị tụt xuống sau khi xoay
      androidx.compose.runtime.key(LocalConfiguration.current.orientation) {
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
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .sheetMaxHeight(full = true)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 24.dp),
                ) {
                    // 0 = chính, 1 = Tay cầm, 2 = Giới thiệu, 3 = Âm thanh, 4 = Cảm ứng & rung, 5 = Dữ liệu, 6 = Game Boy
                    var settingsPage by remember { mutableStateOf(0) }
                // Bảng Cài đặt che mất thông báo của màn hình chính nên hiện thông báo ngay trong bảng.
                model.toast?.let { ToastBubble(it, Modifier.align(Alignment.BottomCenter)) }
            }
                val st = model.settings
                when (settingsPage) {
                    1 -> ControllerPage(model) { settingsPage = 0 }
                    2 -> AboutPage { settingsPage = 0 }
                    6 -> GameBoyPage(model) { settingsPage = 0 }
                    7 -> BiosPage(model) { settingsPage = 0 }
                    3 -> AudioPage(model) { settingsPage = 0 }
                    4 -> {
                        PageHeader("Cảm ứng & rung") { settingsPage = 0 }
                        Spacer(Modifier.height(8.dp))
                        GroupCard {
                            ToggleRow("Hiện nút ảo", st.showPad, icon = "dpad") { v -> model.update { it.copy(showPad = v) } }
                            RowDivider()
                            ToggleRow("Rung khi nhấn nút", st.haptics, icon = "phone") { v -> model.update { it.copy(haptics = v) } }
                        }
                    }
                    5 -> {
                        PageHeader("Dữ liệu") { settingsPage = 0 }
                        Spacer(Modifier.height(8.dp))
                        GroupCard {
                            ActionRow("Xoá hết game", danger = true, icon = "trash", subtitle = "Kèm Save State và file .sav") {
                                deleteAllConfirm = true
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
                        SheetHeader("Cài đặt") { settingsOpen = false }
                        SectionTitle("Điều khiển")
                        GroupCard {
                            ActionRow("Tay cầm", icon = "gamepad", chevron = true) { settingsPage = 1 }
                            RowDivider()
                            ActionRow("Cảm ứng & rung", icon = "phone", chevron = true) { settingsPage = 4 }
                        }
                        SectionTitle("Trình giả lập")
                        GroupCard {
                            ActionRow("Âm thanh", icon = "volume", chevron = true) { settingsPage = 3 }
                            RowDivider()
                            ActionRow("Game Boy", icon = "image", chevron = true) { settingsPage = 6 }
                            RowDivider()
                            ActionRow("BIOS", icon = "database", chevron = true) { settingsPage = 7 }
                        }
                        SectionTitle("Dữ liệu")
                        GroupCard {
                            ActionRow(
                                "Thư mục game",
                                icon = "folder",
                                subtitle = model.gameFolderName ?: "Chưa chọn. Quét thư mục để thêm game vào thư viện",
                                chevron = true,
                            ) {
                                scanAfterPick = false
                                folderPicker.launch(null)
                            }
                            RowDivider()
                            ActionRow("Dữ liệu", icon = "database", chevron = true) { settingsPage = 5 }
                        }
                        SectionTitle("Khác")
                        GroupCard {
                            ActionRow("Giới thiệu và giấy phép", icon = "file-text", chevron = true) { settingsPage = 2 }
                        }
                    }
                }
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


/** Nút tròn có biểu tượng (cài đặt, sắp xếp, thêm game). */
@Composable
private fun IconBtn(icon: String, desc: String, modifier: Modifier = Modifier, small: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .size(if (small) 36.dp else 44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon(icon, size = if (small) 22.dp else 26.dp, tint = Rc.Text)
    }
}

/** Ô tìm kiếm dạng viên thuốc. */
@Composable
private fun SearchBox(query: String, onChange: (String) -> Unit, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Rc.Glass)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroIcon("search", size = 20.dp, tint = Rc.Muted)
        Spacer(Modifier.size(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Rc.Text, fontSize = 15.sp, fontFamily = InterFamily),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Tìm tên ROM hoặc mã ID...", color = Rc.Faint, fontSize = 15.sp)
                    inner()
                }
            },
        )
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
                "Không tìm thấy game nào",
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
        val desc = "Bấm dấu + để thêm file ROM (.gba, .gb, .gbc) của bạn. Chỉ dùng ROM bạn sở hữu hợp pháp; dữ liệu nằm trên máy này."
        if (compact) {
            // Màn ngang thấp: thẻ gọn một hàng để nằm vừa màn hình, không phải vuốt.
            Row(cardMod.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroIcon("layers", size = 40.dp, tint = Rc.Text)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa có game nào", color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    Text("Chưa có game nào", color = Rc.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                Text("Chưa có game nào", color = Rc.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(desc, color = Rc.Muted, fontSize = 17.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                PillButton("Thêm ROM", icon = "plus", onClick = onAdd)
            }
        }
    }
}

/** Menu nhỏ khi giữ lâu vào một game. */
private class GameAction(val label: String, val icon: String, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
private fun GameMenuOverlay(
    rom: RomInfo,
    cover: ImageBitmap?,
    anchor: androidx.compose.ui.geometry.Rect?,
    actions: List<GameAction>,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        // Tắt hiệu ứng mở cửa sổ mặc định của Dialog (nó phóng nhẹ từ giữa nên nội dung bị "trượt" sang trái).
        val dialogView = androidx.compose.ui.platform.LocalView.current
        // Làm mờ phần phía sau bằng hiệu ứng mờ cửa sổ của hệ điều hành (Android 12+, nếu máy bật); không có thì chỉ tối nền.
        val blurOk = remember {
            android.os.Build.VERSION.SDK_INT >= 31 &&
                dialogView.context.getSystemService(android.view.WindowManager::class.java)?.isCrossWindowBlurEnabled == true
        }
        androidx.compose.runtime.SideEffect {
            (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.apply {
                setWindowAnimations(-1)
                setDimAmount(0f)
                if (blurOk && android.os.Build.VERSION.SDK_INT >= 31) {
                    addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    attributes = attributes.also { it.blurBehindRadius = 150 }
                }
            }
        }
        val scale = remember { androidx.compose.animation.core.Animatable(1f) }
        val fade = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(Unit) {
            launch { fade.animateTo(1f, androidx.compose.animation.core.tween(140)) }
            scale.animateTo(1.08f, androidx.compose.animation.core.tween(160))
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
                .background(if (blurOk) Color(0x55000000) else Color(0xCC000000))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            val sw = with(density) { maxWidth.toPx() }
            val sh = with(density) { maxHeight.toPx() }
            val gap = with(density) { 12.dp.toPx() }
            val margin = with(density) { 12.dp.toPx() }
            val insetTop = maxOf(WindowInsets.statusBars.getTop(density).toFloat(), with(density) { 24.dp.toPx() }) + margin
            val insetBottom = maxOf(WindowInsets.navigationBars.getBottom(density).toFloat(), with(density) { 20.dp.toPx() }) + margin
            val a = anchor ?: androidx.compose.ui.geometry.Rect(sw / 2 - 90f, sh / 3, sw / 2 + 90f, sh / 3 + 180f)
            val coverW = a.width
            val menuW = with(density) { 260.dp.toPx() }
            val dens = density.density
            val coverBottom = a.top + coverW + coverW * 0.04f
            val titleDp = 30f
            val n = actions.size
            // Ưu tiên bảng nằm dưới game; không đủ chỗ thì sang cạnh game, đáy bảng bằng đáy game.
            var rowDp = 48f
            var mx = a.left.coerceIn(margin, (sw - menuW - margin).coerceAtLeast(margin))
            var my = coverBottom + gap
            if (my + (titleDp + n * rowDp) * dens > sh - insetBottom) {
                val bottomY = minOf(coverBottom, sh - insetBottom)
                rowDp = (((bottomY - insetTop) / dens - titleDp) / n).coerceIn(34f, 48f)
                my = bottomY - (titleDp + n * rowDp) * dens
                mx = if (a.right + gap + menuW <= sw - margin) a.right + coverW * 0.04f + gap
                else (a.left - coverW * 0.04f - gap - menuW).coerceAtLeast(margin)
            }
            // Game giữ nguyên vị trí cũ, phóng to nhẹ.
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(a.left.toInt(), a.top.toInt()) }
                    .size(with(density) { coverW.toDp() })
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101114))
                    .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    Image(bitmap = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                        color = Rc.Muted,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Bảng thao tác
            Column(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(mx.toInt(), my.toInt()) }
                    .width(260.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Rc.Sheet)
                    .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Text(
                    rom.name,
                    color = Rc.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(titleDp.dp).padding(horizontal = 16.dp).wrapContentHeight(Alignment.CenterVertically),
                )
                actions.forEach { act ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = act.onClick)
                            .height(rowDp.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            act.label,
                            color = if (act.danger) Rc.Danger else Rc.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        RetroIcon(act.icon, size = 20.dp, tint = if (act.danger) Rc.Danger else Rc.Muted)
                    }
                }
            }
        }
    }
}

/** Ô game trong lưới: bìa vuông, tên bên dưới. Chạm để chơi, giữ lâu để mở menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RomTile(
    rom: RomInfo,
    cover: ImageBitmap?,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu()
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101114))
                .border(BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                    color = Rc.Muted,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            rom.name,
            color = Rc.Text,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            relativeDay(maxOf(rom.lastPlayedAt, rom.addedAt)),
            color = Rc.Muted,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
        )
    }
}

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
