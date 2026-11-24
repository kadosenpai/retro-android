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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
    var settingsOpen by remember { mutableStateOf(false) }
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
    val sort = model.settings.sort
    // Màn ngang (thấp): gộp thanh trên thành một hàng, bỏ thanh dưới để game hiện to, đủ.
    val compact = LocalConfiguration.current.screenHeightDp < 480
    var view by remember { mutableStateOf(LibView.LIBRARY) }
    var searchOpen by remember { mutableStateOf(false) }
    // Tab: "Tất cả" cùng các hệ máy đã có game; thêm game của hệ mới thì tab mới xuất hiện.
    val tabs = remember(model.roms) {
        val systems = GameSystem.TABS.filter { (id, _) -> model.roms.any { GameSystem.fromId(it.system).tab == id } }
        if (systems.isEmpty()) emptyList() else listOf(ALL_TAB to "Tất cả") + systems
    }
    val tabsNow = rememberUpdatedState(tabs)
    // Vuốt trái/phải để chuyển giữa các tab (mỗi tab một trang).
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
    fun closeSearch() {
        searchOpen = false
        query = ""
        hideKeyboard()
    }
    androidx.activity.compose.BackHandler(enabled = searchOpen) { closeSearch() }
    androidx.activity.compose.BackHandler(enabled = !searchOpen && view != LibView.LIBRARY) { view = LibView.LIBRARY }

    // Làm mờ phần nền phía sau khi có bảng mở (Android 12 trở lên).
    val blurRadius = if (settingsOpen) 16.dp else 0.dp
    val glassState = rememberGlassState()
    val density = LocalDensity.current
    // Chiều cao thanh trên và thanh dưới: nội dung cuộn xuống dưới chúng (để kính có gì để làm mờ) nhưng đầu và cuối lưới chừa chỗ đúng bằng chúng.
    var headerPx by remember { mutableStateOf(0) }
    var bottomPx by remember { mutableStateOf(0) }
    val topPad = with(density) { headerPx.toDp() } + 12.dp
    val bottomPad = if (compact) 8.dp else with(density) { bottomPx.toDp() } + 12.dp
    val onMenu: (RomInfo, androidx.compose.ui.geometry.Rect?) -> Unit = { rom, rect ->
        hideKeyboard()
        menuAnchor = rect
        gameMenu = rom
    }
    val pickRoms = { picker.launch(arrayOf("*/*")) }

    CompositionLocalProvider(LocalGlass provides (if (model.settings.glass) glassState else null)) {
        Box(Modifier.fillMaxSize().blur(blurRadius, BlurredEdgeTreatment.Unbounded)) {
            // Vùng nguồn của kính: nền và nội dung cuộn nằm phía sau các thanh.
            Box(
                Modifier
                    .fillMaxSize()
                    .glassSource(glassState)
                    .retroBackground()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    tabs.isEmpty() -> Box(Modifier.padding(top = topPad, bottom = bottomPad)) {
                        EmptyState(hasAny = false, onAdd = pickRoms)
                    }
                    view == LibView.LIBRARY -> HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        val pageTab = tabs.getOrNull(page)?.first ?: tab
                        val inTab = remember(model.roms, pageTab) {
                            if (pageTab == ALL_TAB) model.roms else model.roms.filter { GameSystem.fromId(it.system).tab == pageTab }
                        }
                        val shown = remember(inTab, query, sort) { sortRoms(matchQuery(inTab, query), sort) }
                        // Thẻ "Chơi tiếp": game chơi gần nhất, chỉ ở trang Tất cả và khi không tìm kiếm.
                        val hero = remember(model.roms, pageTab, query) {
                            if (pageTab == ALL_TAB && query.isBlank()) model.roms.filter { it.lastPlayedAt > 0 }.maxByOrNull { it.lastPlayedAt } else null
                        }
                        if (shown.isEmpty()) {
                            Box(Modifier.padding(top = topPad, bottom = bottomPad)) {
                                EmptyState(hasAny = inTab.isNotEmpty(), onAdd = pickRoms)
                            }
                        } else {
                            GamesGrid(shown, model, compact, topPad, bottomPad, showBadge = pageTab == ALL_TAB, hero = hero, onMenu = onMenu)
                        }
                    }
                    else -> {
                        val recent = view == LibView.RECENT
                        val list = remember(model.roms, view, query, sort) {
                            if (recent) {
                                matchQuery(model.roms.filter { it.lastPlayedAt > 0 }, query).sortedByDescending { it.lastPlayedAt }
                            } else {
                                sortRoms(matchQuery(model.roms.filter { it.favorite }, query), sort)
                            }
                        }
                        if (list.isEmpty()) {
                            Box(Modifier.padding(top = topPad, bottom = bottomPad)) {
                                if (query.isNotBlank()) {
                                    EmptyState(hasAny = true, onAdd = pickRoms)
                                } else if (recent) {
                                    SimpleEmpty("Chưa có game nào gần đây", "Chơi một game, nó sẽ hiện ở đây.")
                                } else {
                                    SimpleEmpty("Chưa có game yêu thích", "Giữ lâu vào một game rồi chọn \"Thêm vào Yêu thích\".")
                                }
                            }
                        } else {
                            GamesGrid(list, model, compact, topPad, bottomPad, showBadge = true, hero = null, onMenu = onMenu)
                        }
                    }
                }
            }

            // Thanh trên (kính): tiêu đề, tìm kiếm, quét lại; hàng tab hệ máy và sắp xếp.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .onSizeChanged { headerPx = it.height }
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp),
            ) {
                val refreshIcon = if (hasAnyRom) "refresh" else "plus"
                val refreshDesc = if (hasAnyRom) "Quét lại thư mục game" else "Thêm game"
                if (compact) {
                    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassIconBtn("settings", "Cài đặt") {
                            hideKeyboard()
                            settingsOpen = true
                        }
                        Spacer(Modifier.width(8.dp))
                        if (searchOpen) {
                            GlassSearchBox(query, { query = it }, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            GlassIconBtn("x", "Đóng tìm kiếm") { closeSearch() }
                        } else {
                            if (tabs.isNotEmpty()) {
                                GlassTabBar(tabs, pagerState.currentPage, Modifier.weight(1f)) { id ->
                                    if (id != model.settings.tab) model.update { it.copy(tab = id) }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.width(8.dp))
                            SortButton(sort) { k -> model.update { it.copy(sort = k) } }
                            Spacer(Modifier.width(8.dp))
                            GlassIconBtn("search", "Tìm kiếm") { searchOpen = true }
                            Spacer(Modifier.width(8.dp))
                            GlassIconBtn(refreshIcon, refreshDesc) { addOrScan() }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (searchOpen) {
                            GlassSearchBox(query, { query = it }, Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            GlassIconBtn("x", "Đóng tìm kiếm") { closeSearch() }
                        } else {
                            Text(
                                when (view) {
                                    LibView.LIBRARY -> "Thư viện"
                                    LibView.RECENT -> "Gần đây"
                                    LibView.FAVORITES -> "Yêu thích"
                                },
                                modifier = Modifier.weight(1f),
                                color = Rc.Text,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                            )
                            GlassIconBtn("search", "Tìm kiếm") { searchOpen = true }
                            Spacer(Modifier.width(8.dp))
                            GlassIconBtn(refreshIcon, refreshDesc) { addOrScan() }
                        }
                    }
                    if (view == LibView.LIBRARY && tabs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            GlassTabBar(tabs, pagerState.currentPage, Modifier.weight(1f)) { id ->
                                if (id != model.settings.tab) model.update { it.copy(tab = id) }
                            }
                            Spacer(Modifier.width(10.dp))
                            SortButton(sort) { k -> model.update { it.copy(sort = k) } }
                        }
                    } else if (view == LibView.FAVORITES && tabs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            SortButton(sort) { k -> model.update { it.copy(sort = k) } }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            // Thanh dưới (kính): Thư viện · Gần đây · Yêu thích · Cài đặt.
            if (!compact) {
                GlassBottomBar(
                    selected = view,
                    onSelect = { v ->
                        hideKeyboard()
                        view = v
                    },
                    onSettings = {
                        hideKeyboard()
                        settingsOpen = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomPx = it.height }
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                        .padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
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
            actions = listOfNotNull(
                GameAction(if (rom.favorite) "Bỏ khỏi Yêu thích" else "Thêm vào Yêu thích", "star") {
                    gameMenu = null
                    model.toggleFavorite(rom)
                },
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

    // Cài đặt
    if (settingsOpen) {
      // key theo chiều xoay: tạo lại bảng để nó đo lại, tránh bị tụt xuống sau khi xoay
      androidx.compose.runtime.key(LocalConfiguration.current.orientation) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = state,
            containerColor = Rc.SheetGlass,
            contentColor = Rc.Text,
            scrimColor = Color(0x59000000),
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
                    // 0 = chính, 1 = Tay cầm, 2 = Giới thiệu, 3 = Âm thanh, 4 = Cảm ứng & rung, 5 = Dữ liệu, 6 = Game Boy, 7 = BIOS, 8 = Hình ảnh
                    var settingsPage by remember { mutableStateOf(0) }
                val st = model.settings
                when (settingsPage) {
                    1 -> ControllerPage(model) { settingsPage = 0 }
                    2 -> AboutPage { settingsPage = 0 }
                    6 -> GameBoyPage(model) { settingsPage = 0 }
                    7 -> BiosPage(model) { settingsPage = 0 }
                    8 -> DisplayPage(model) { settingsPage = 0 }
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
                            ActionRow("Hình ảnh", icon = "image", chevron = true, subtitle = model.settings.filter.label) { settingsPage = 8 }
                            RowDivider()
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
                // Bảng Cài đặt che mất thông báo của màn hình chính nên hiện thông báo ngay trong bảng.
                model.toast?.let { ToastBubble(it, Modifier.align(Alignment.BottomCenter)) }
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
                                .clip(RoundedCornerShape(10.dp))
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

private fun consoleLabel(mode: String) = when (mode) {
    "ds" -> "DS"
    "dsi" -> "DSi"
    else -> "Tự động"
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


private enum class LibView { LIBRARY, RECENT, FAVORITES }

/** Id của tab "Tất cả" (các tab hệ máy dùng id trong [GameSystem.TABS]). */
private const val ALL_TAB = "all"

private fun matchQuery(list: List<RomInfo>, query: String): List<RomInfo> {
    val q = query.trim().lowercase()
    return if (q.isEmpty()) list else list.filter { it.name.lowercase().contains(q) || it.id.startsWith(q) }
}

private fun sortRoms(list: List<RomInfo>, sort: SortKey): List<RomInfo> = when (sort) {
    SortKey.RECENT -> list.sortedByDescending { maxOf(it.lastPlayedAt, it.addedAt) }
    SortKey.NAME -> list.sortedBy { it.name.lowercase() }
    SortKey.ADDED -> list.sortedByDescending { it.addedAt }
}

/** Nút tròn kính có biểu tượng (tìm kiếm, sắp xếp, quét lại...). */
@Composable
private fun GlassIconBtn(icon: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .glass(24.dp, blur = 4.dp, bevel = 14.dp, refraction = 9.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon(icon, size = 22.dp, tint = Rc.Text)
    }
}

/** Nút sắp xếp kèm menu chọn kiểu sắp xếp. */
@Composable
private fun SortButton(sort: SortKey, onPick: (SortKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        GlassIconBtn("sort", "Sắp xếp") { open = true }
        SortMenu(open, { open = false }, sort, onPick)
    }
}

/** Ô tìm kiếm dạng viên thuốc bằng kính; tự mở bàn phím khi xuất hiện. */
@Composable
private fun GlassSearchBox(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier
            .height(44.dp)
            .glass(22.dp, blur = 4.dp, bevel = 14.dp, refraction = 9.dp)
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
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Tìm tên ROM hoặc mã ID...", color = Rc.Faint, fontSize = 15.sp)
                    inner()
                }
            },
        )
    }
}

/** Thanh tab hệ máy dạng viên thuốc kính, cuộn ngang khi nhiều hệ; tab đang chọn sáng hơn và tự cuộn vào tầm nhìn. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassTabBar(tabs: List<Pair<String, String>>, selected: Int, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val even = tabs.size <= 4
    Row(
        modifier
            .height(48.dp)
            .glass(24.dp, blur = 4.dp, bevel = 18.dp, refraction = 12.dp)
            .padding(4.dp)
            .then(if (even) Modifier else Modifier.horizontalScroll(rememberScrollState())),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, (id, label) ->
            val on = i == selected
            val requester = remember { BringIntoViewRequester() }
            LaunchedEffect(on) { if (on) requester.bringIntoView() }
            Box(
                Modifier
                    .bringIntoViewRequester(requester)
                    .then(if (even) Modifier.weight(1f) else Modifier.widthIn(min = 64.dp))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .then(if (on) Modifier.background(Color(0x42FFFFFF)).border(1.dp, Color(0x4DFFFFFF), CircleShape) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(id) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) Rc.Text else Color(0xE6FFFFFF),
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Thanh dưới nổi bằng kính. "Cài đặt" mở bảng cài đặt chứ không đổi mục đang xem. */
@Composable
private fun GlassBottomBar(selected: LibView, onSelect: (LibView) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .glass(34.dp, blur = 3.dp, bevel = 26.dp, refraction = 18.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarItem("grid", "Thư viện", selected == LibView.LIBRARY, Modifier.weight(1f)) { onSelect(LibView.LIBRARY) }
        BarItem("clock", "Gần đây", selected == LibView.RECENT, Modifier.weight(1f)) { onSelect(LibView.RECENT) }
        BarItem("star", "Yêu thích", selected == LibView.FAVORITES, Modifier.weight(1f)) { onSelect(LibView.FAVORITES) }
        BarItem("settings", "Cài đặt", false, Modifier.weight(1f)) { onSettings() }
    }
}

@Composable
private fun BarItem(icon: String, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .then(if (on) Modifier.background(Color(0x3DFFFFFF)).border(1.dp, Color(0x4DFFFFFF), CircleShape) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RetroIcon(icon, size = 22.dp, tint = if (on) Rc.Text else Color(0xD9FFFFFF))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (on) Rc.Text else Color(0xD9FFFFFF),
            fontSize = 10.5.sp,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** Thẻ "Chơi tiếp": game chơi gần nhất, chạm để vào chơi. Nằm trong lưới nên dùng mặt kính nhẹ (không đọc nền). */
@Composable
private fun HeroCard(rom: RomInfo, cover: ImageBitmap?, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .glassSurface(26.dp, tint = Color(0x24FFFFFF))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF101114)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(bitmap = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                    color = Rc.Muted,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("CHƠI TIẾP", color = Rc.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(
                rom.name,
                color = Rc.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "Chơi lần cuối · " + relativeTime(rom.lastPlayedAt),
                color = Rc.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x38FFFFFF)), contentAlignment = Alignment.Center) {
            RetroIcon("play", size = 22.dp, tint = Rc.Text)
        }
    }
}

/** Lưới game (cùng thẻ "Chơi tiếp" ở đầu nếu có). Đầu và cuối chừa chỗ cho thanh trên và thanh dưới nổi phía trên. */
@Composable
private fun GamesGrid(
    shown: List<RomInfo>,
    model: AppModel,
    compact: Boolean,
    topPad: androidx.compose.ui.unit.Dp,
    bottomPad: androidx.compose.ui.unit.Dp,
    showBadge: Boolean,
    hero: RomInfo?,
    onMenu: (RomInfo, androidx.compose.ui.geometry.Rect?) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Màn ngang: cỡ bìa tính theo chiều cao còn lại để cả bìa, tên 2 dòng và ngày chơi luôn nằm gọn trong một hàng.
        val coverSize = maxOf(90.dp, minOf(170.dp, maxHeight - topPad - bottomPad - 84.dp))
        LazyVerticalGrid(
            columns = if (compact) GridCells.FixedSize(coverSize) else GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad, bottom = bottomPad),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (hero != null) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                    HeroCard(hero, rememberCover(model, hero), onOpen = { model.open(hero) })
                }
            }
            items(shown, key = { it.id }) { rom ->
                // Giữ vị trí ô bằng biến thường (không phải state) để cuộn danh sách không bị recompose liên tục.
                val tileBounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(1) }
                Box(Modifier.onGloballyPositioned { tileBounds[0] = it.boundsInWindow() }) {
                    RomTile(
                        rom = rom,
                        cover = rememberCover(model, rom),
                        showBadge = showBadge,
                        onOpen = { model.open(rom) },
                        onMenu = { onMenu(rom, tileBounds[0]) },
                    )
                }
            }
        }
    }
}

/** Thông báo gọn cho mục Gần đây / Yêu thích khi còn trống. */
@Composable
private fun SimpleEmpty(title: String, hint: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(hint, color = Rc.Muted, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
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
                    .background(Rc.SheetGlass)
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
    showBadge: Boolean,
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
            if (showBadge) {
                val tabId = GameSystem.fromId(rom.system).tab
                Text(
                    GameSystem.TABS.firstOrNull { it.first == tabId }?.second ?: tabId.uppercase(Locale.ROOT),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(Color(0x73000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            if (rom.favorite) {
                RetroIcon("star", size = 16.dp, tint = Color(0xFFFFD60A), modifier = Modifier.align(Alignment.TopEnd).padding(7.dp))
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

/** "vừa xong", "12 phút trước", "3 giờ trước", rồi theo ngày. */
private fun relativeTime(ts: Long): String {
    val m = ((System.currentTimeMillis() - ts) / 60_000L).toInt()
    return when {
        m < 1 -> "vừa xong"
        m < 60 -> "$m phút trước"
        m < 1440 -> "${m / 60} giờ trước"
        else -> relativeDay(ts).lowercase(Locale.ROOT)
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
