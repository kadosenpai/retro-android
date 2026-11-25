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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.retro.app.AppModel
import com.retro.app.data.GameSystem
import com.retro.app.data.RomInfo
import com.retro.app.data.SortKey
import androidx.compose.ui.geometry.Offset
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
    // Màn ngang (thấp): bỏ thanh dưới, gộp thanh trên thành một hàng để game hiện to, đủ.
    val compact = LocalConfiguration.current.screenHeightDp < 480
    var view by remember { mutableStateOf(LibView.LIBRARY) }
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
    androidx.activity.compose.BackHandler(enabled = view != LibView.LIBRARY) { view = LibView.LIBRARY }

    // Màu nền lấy từ bìa game nổi bật của mục đang xem, đổi màu mượt khi chuyển tab.
    val tintRom = remember(model.roms, view, tab) {
        val pool = when {
            view == LibView.FAVORITES -> model.roms.filter { it.favorite }
            view == LibView.LIBRARY && tab != ALL_TAB -> model.roms.filter { GameSystem.fromId(it.system).tab == tab }
            else -> model.roms
        }
        pool.filter { it.coverAt > 0L }.maxByOrNull { maxOf(it.lastPlayedAt, it.addedAt) }
    }
    val tintCover = if (tintRom != null) rememberCover(model, tintRom) else null
    val tintTarget = remember(tintCover) { if (tintCover != null) dominantColor(tintCover) else NeutralTint }
    val tint by animateColorAsState(tintTarget, tween(500), label = "tint")

    // Làm mờ phần nền phía sau khi có bảng mở (Android 12 trở lên).
    val blurRadius = if (settingsOpen) 16.dp else 0.dp
    val onMenu: (RomInfo, Bounds?) -> Unit = { rom, rect ->
        hideKeyboard()
        menuAnchor = rect
        gameMenu = rom
    }
    val pickRoms = { picker.launch(arrayOf("*/*")) }
    val openSettings = {
        hideKeyboard()
        settingsOpen = true
    }

    val navBarInset = with(androidx.compose.ui.platform.LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val navPad = if (compact) 0.dp else 52.dp + navBarInset
    Box(
        Modifier
            .fillMaxSize()
            .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            .tintBackground(tint),
    ) {
    androidx.compose.runtime.CompositionLocalProvider(LocalNavPad provides navPad) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    if (compact) WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    else WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            ),
    ) {
        // Hàng đầu: ô tìm kiếm, sắp xếp, quét lại (màn ngang thêm nút cài đặt ở đầu).
        if (view != LibView.SETTINGS) Row(
            Modifier.fillMaxWidth().padding(start = Ds.Pad, end = Ds.Pad, top = if (compact) 4.dp else Ds.GapSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (compact) {
                CircleBtn("settings", "Cài đặt", onClick = openSettings)
                Spacer(Modifier.width(Ds.Gap))
            }
            SearchPill(query, { query = it }, Modifier.weight(1f))
            Spacer(Modifier.width(Ds.Gap))
            SortButton(sort) { k -> model.update { it.copy(sort = k) } }
            Spacer(Modifier.width(Ds.Gap))
            CircleBtn(
                if (hasAnyRom) "refresh" else "plus",
                if (hasAnyRom) "Quét lại thư mục game" else "Thêm game",
            ) { addOrScan() }
        }

        if (view == LibView.SETTINGS) {
            // tab Cài đặt: không cần thanh tab
        } else if (view == LibView.LIBRARY && tabs.isNotEmpty()) {
            LibTabs(tabs, pagerState.currentPage, compact) { id ->
                if (id != model.settings.tab) model.update { it.copy(tab = id) }
            }
        } else {
            Spacer(Modifier.height(Ds.Gap))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                view == LibView.SETTINGS -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Ds.Pad)
                        .padding(bottom = LocalNavPad.current + Ds.SectionGap),
                ) {
                    SettingsContent(
                        model = model,
                        onClose = null,
                        onDeleteAll = { deleteAllConfirm = true },
                        onPickFolder = {
                            scanAfterPick = false
                            folderPicker.launch(null)
                        },
                    )
                }
                tabs.isEmpty() -> EmptyState(hasAny = false, onAdd = pickRoms)
                view == LibView.LIBRARY -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 0,
                ) { page ->
                    val pageTab = tabs.getOrNull(page)?.first ?: tab
                    LibraryPage(
                        model = model,
                        pageTab = pageTab,
                        query = query,
                        sort = sort,
                        compact = compact,
                        tint = tint,
                        onAdd = pickRoms,
                        onMenu = onMenu,
                    )
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
                        if (query.isNotBlank()) {
                            EmptyState(hasAny = true, onAdd = pickRoms)
                        } else if (recent) {
                            SimpleEmpty("Chưa có game nào gần đây", "Chơi một game, nó sẽ hiện ở đây.")
                        } else {
                            SimpleEmpty("Chưa có game yêu thích", "Giữ lâu vào một game rồi chọn \"Thêm vào Yêu thích\".")
                        }
                    } else if (compact) {
                        GamesGridCompact(list, model, showBadge = true, onMenu = onMenu)
                    } else {
                        GamesList(
                            title = if (recent) "Gần đây" else "Yêu thích",
                            list = list,
                            model = model,
                            showBadge = true,
                            onMenu = onMenu,
                        )
                    }
                }
            }
        }

    }
    }
    if (!compact) {
        LibBottomBar(
            selected = view,
            tint = tint,
            onSelect = { v ->
                hideKeyboard()
                view = v
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    }

    // Giữ lâu vào một game: game phóng to ở giữa, bảng thao tác nằm dưới, phần còn lại mờ đi.
    gameMenu?.let { rom ->
        GameMenuOverlay(
            rom = rom,
            anchor = menuAnchor,
            cover = rememberCover(model, rom),
            tint = tint,
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
            containerColor = Rc.Sheet,
            contentColor = Rc.Text,
            scrimColor = Color(0x8C000000),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = null,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .sheetMaxHeight(full = true)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Ds.Pad)
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 24.dp),
                ) {
                SettingsContent(
                    model = model,
                    onClose = { settingsOpen = false },
                    onDeleteAll = { deleteAllConfirm = true },
                    onPickFolder = {
                        scanAfterPick = false
                        folderPicker.launch(null)
                    },
                )
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


private enum class LibView { LIBRARY, RECENT, FAVORITES, SETTINGS }

/** Id của tab "Tất cả" (các tab hệ máy dùng id trong [GameSystem.TABS]). */
private const val ALL_TAB = "all"

/** Khoảng đệm dưới cùng để nội dung cuộn không bị thanh điều hướng che. */
private val LocalNavPad = androidx.compose.runtime.compositionLocalOf { 0.dp }

private typealias Bounds = androidx.compose.ui.geometry.Rect

private fun matchQuery(list: List<RomInfo>, query: String): List<RomInfo> {
    val q = query.trim().lowercase()
    return if (q.isEmpty()) list else list.filter { it.name.lowercase().contains(q) || it.id.startsWith(q) }
}

private fun sortRoms(list: List<RomInfo>, sort: SortKey): List<RomInfo> = when (sort) {
    SortKey.RECENT -> list.sortedByDescending { maxOf(it.lastPlayedAt, it.addedAt) }
    SortKey.NAME -> list.sortedBy { it.name.lowercase() }
    SortKey.ADDED -> list.sortedByDescending { it.addedAt }
}

/** Nhãn ngắn của hệ máy (GBA, NDS, PS1...). */
private fun sysLabel(rom: RomInfo): String {
    val t = GameSystem.fromId(rom.system).tab
    return GameSystem.TABS.firstOrNull { it.first == t }?.second ?: t.uppercase(Locale.ROOT)
}

/** Nút tròn có biểu tượng (sắp xếp, quét lại, cài đặt ở màn ngang). */
@Composable
private fun CircleBtn(icon: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(Ds.BtnSize)
            .clip(CircleShape)
            .background(Ds.CardFill)
            .border(BorderStroke(1.dp, Ds.CardStroke), CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RetroIcon(icon, size = Ds.IconBtn, tint = Rc.Text)
    }
}

/** Nút sắp xếp kèm menu chọn kiểu sắp xếp. */
@Composable
private fun SortButton(sort: SortKey, onPick: (SortKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        CircleBtn("sort", "Sắp xếp") { open = true }
        SortMenu(open, { open = false }, sort, onPick)
    }
}

/** Ô tìm kiếm dạng viên thuốc, gõ là lọc ngay. */
@Composable
private fun SearchPill(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Ds.BtnSize / 2)
    Row(
        modifier
            .height(Ds.BtnSize)
            .clip(shape)
            .background(Ds.CardFill)
            .border(BorderStroke(1.dp, Ds.CardStroke), shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroIcon("search", size = Ds.IconRow, tint = Rc.Muted)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Rc.Text, fontSize = 15.sp, fontFamily = InterFamily),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Tìm tên ROM hoặc mã ID...", color = Rc.Faint, fontSize = 15.sp, maxLines = 1)
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(Ds.IconBtn).clip(CircleShape).clickableNoRipple { onChange("") }, contentAlignment = Alignment.Center) {
                RetroIcon("x", size = 18.dp, tint = Rc.Muted)
            }
        }
    }
}

/** Hàng tab chữ: tab đang chọn đậm, trắng và có gạch chân; tự cuộn vào tầm nhìn. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibTabs(tabs: List<Pair<String, String>>, selected: Int, compact: Boolean, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Ds.Pad)
            .padding(top = if (compact) 6.dp else 20.dp, bottom = if (compact) 4.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        tabs.forEachIndexed { i, (id, label) ->
            val on = i == selected
            val requester = remember { BringIntoViewRequester() }
            LaunchedEffect(on) { if (on) requester.bringIntoView() }
            Column(
                Modifier
                    .bringIntoViewRequester(requester)
                    .clickableNoRipple { onSelect(id) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    color = if (on) Rc.Text else Rc.Faint,
                    fontSize = if (compact) 16.sp else 19.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(20.dp).height(3.dp).clip(CircleShape).background(if (on) Color.White else Color.Transparent))
            }
        }
    }
}

/** Tiêu đề mục (cỡ 20) kèm nút "Xem thêm ›" tuỳ chọn. */
@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = Ds.Pad, end = Ds.Pad, top = Ds.SectionGap, bottom = Ds.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Rc.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.weight(1f))
        if (action != null) {
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .clip(shape)
                    .background(Ds.CardFill)
                    .border(BorderStroke(1.dp, Ds.CardStroke), shape)
                    .clickable(onClick = onAction)
                    .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, color = Rc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                RetroIcon("chevron-right", size = 16.dp, tint = Rc.Text)
            }
        }
    }
}

/** Thanh dưới gọn như GameHub: nền trong suốt, icon 24dp + chữ 11sp; mục đang chọn trắng, icon đặc, có hiệu ứng nảy. */
@Composable
private fun LibBottomBar(selected: LibView, tint: Color, onSelect: (LibView) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth().background(lerp(tint, Color.Black, 0.88f).copy(alpha = 0.92f))) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ds.Divider))
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Ds.GapSm)) {
        NavItem("grid", "Thư viện", selected == LibView.LIBRARY, Modifier.weight(1f)) { onSelect(LibView.LIBRARY) }
        NavItem("clock", "Gần đây", selected == LibView.RECENT, Modifier.weight(1f)) { onSelect(LibView.RECENT) }
        NavItem("star", "Yêu thích", selected == LibView.FAVORITES, Modifier.weight(1f)) { onSelect(LibView.FAVORITES) }
        NavItem("settings", "Cài đặt", selected == LibView.SETTINGS, Modifier.weight(1f)) { onSelect(LibView.SETTINGS) }
    }
  }
}

@Composable
private fun NavItem(icon: String, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (on) Color.White else Color(0xFFA9ABB3), tween(220), label = "navColor")
    val pop by androidx.compose.animation.core.animateFloatAsState(
        if (on) 1.14f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "navPop",
    )
    val fillAmt by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, tween(220), label = "navFill")
    Column(
        modifier.clickableNoRipple(onClick).padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(22.dp).graphicsLayer { scaleX = pop; scaleY = pop }, contentAlignment = Alignment.Center) {
            if (icon == "grid") {
                // Bốn ô bo tròn: viền khi tắt, đặc khi chọn (như icon Library của GameHub)
                androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
                    val s = size.minDimension
                    val gap = s * 0.10f
                    val cell = (s - gap) / 2f
                    val r = androidx.compose.ui.geometry.CornerRadius(cell * 0.30f)
                    val sw = s * 0.085f
                    for (i in 0..1) for (j in 0..1) {
                        val tl = Offset(i * (cell + gap), j * (cell + gap))
                        drawRoundRect(color.copy(alpha = fillAmt), tl, androidx.compose.ui.geometry.Size(cell, cell), r)
                        drawRoundRect(color.copy(alpha = 1f - fillAmt), tl + Offset(sw / 2, sw / 2), androidx.compose.ui.geometry.Size(cell - sw, cell - sw), r, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                    }
                }
            } else {
                RetroIcon(icon, size = 22.dp, tint = color, fill = on && icon == "star")
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** Một trang của thư viện (một tab). Trang "Tất cả" có Chơi tiếp, thẻ nổi bật, Thêm gần đây rồi toàn bộ game. */
@Composable
private fun LibraryPage(
    model: AppModel,
    pageTab: String,
    query: String,
    sort: SortKey,
    compact: Boolean,
    tint: Color,
    onAdd: () -> Unit,
    onMenu: (RomInfo, Bounds?) -> Unit,
) {
    val inTab = remember(model.roms, pageTab) {
        if (pageTab == ALL_TAB) model.roms else model.roms.filter { GameSystem.fromId(it.system).tab == pageTab }
    }
    val shown = remember(inTab, query, sort) { sortRoms(matchQuery(inTab, query), sort) }
    if (shown.isEmpty()) {
        EmptyState(hasAny = inTab.isNotEmpty(), onAdd = onAdd)
        return
    }
    val showBadge = pageTab == ALL_TAB
    if (compact) {
        GamesGridCompact(shown, model, showBadge, onMenu)
        return
    }
    val home = pageTab == ALL_TAB && query.isBlank()
    val recent = remember(model.roms) {
        model.roms.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(1)
    }
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = ((maxWidth - Ds.Pad * 2) / 170.dp).toInt().coerceAtLeast(2)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 0.dp, bottom = Ds.SectionGap + LocalNavPad.current),
        ) {
            if (home) {
                if (recent.isNotEmpty()) {
                    item(key = "h-recent") { SectionHeader("Chơi tiếp") }
                    item(key = "c-recent") {
                        val top = recent.first()
                        ContinueCard(top, rememberCover(model, top), { model.open(top) }) { b -> onMenu(top, b) }
                    }
                }
                item(key = "h-all") { SectionHeader("Tất cả game") }
            } else {
                item(key = "top") { Spacer(Modifier.height(Ds.Gap)) }
            }
            gameRows(shown, cols, model, showBadge, onMenu)
        }
    }
}

/** Danh sách game theo hàng (cols cột) cho LazyColumn. */
private fun androidx.compose.foundation.lazy.LazyListScope.gameRows(
    games: List<RomInfo>,
    cols: Int,
    model: AppModel,
    showBadge: Boolean,
    onMenu: (RomInfo, Bounds?) -> Unit,
) {
    items(games.chunked(cols), key = { row -> row.first().id }) { row ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Ds.Pad).padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(Ds.Gap),
        ) {
            row.forEach { rom -> Box(Modifier.weight(1f)) { GameTile(rom, model, showBadge, onMenu) } }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Danh sách có tiêu đề (cho mục Gần đây và Yêu thích). */
@Composable
private fun GamesList(title: String, list: List<RomInfo>, model: AppModel, showBadge: Boolean, onMenu: (RomInfo, Bounds?) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = ((maxWidth - Ds.Pad * 2) / 170.dp).toInt().coerceAtLeast(2)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Ds.SectionGap + LocalNavPad.current)) {
            item(key = "title") { SectionHeader(title) }
            gameRows(list, cols, model, showBadge, onMenu)
        }
    }
}

/** Màn ngang: lưới cỡ cố định theo chiều cao còn lại để bìa, tên 2 dòng và ngày luôn nằm gọn trong một hàng. */
@Composable
private fun GamesGridCompact(list: List<RomInfo>, model: AppModel, showBadge: Boolean, onMenu: (RomInfo, Bounds?) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val coverSize = maxOf(90.dp, minOf(170.dp, maxHeight - 84.dp))
        LazyVerticalGrid(
            columns = GridCells.FixedSize(coverSize),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Ds.Pad, end = Ds.Pad, top = Ds.GapSm, bottom = Ds.GapSm + LocalNavPad.current),
            horizontalArrangement = Arrangement.spacedBy(Ds.Gap, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(list, key = { it.id }) { rom -> GameTile(rom, model, showBadge, onMenu) }
        }
    }
}

/** Ô game kèm việc ghi lại vị trí trên màn hình (để menu giữ lâu đặt đúng chỗ). */
@Composable
private fun GameTile(rom: RomInfo, model: AppModel, showBadge: Boolean, onMenu: (RomInfo, Bounds?) -> Unit) {
    // Giữ vị trí ô bằng biến thường (không phải state) để cuộn danh sách không bị recompose liên tục.
    val bounds = remember { arrayOfNulls<Bounds>(1) }
    Box(Modifier.onGloballyPositioned { bounds[0] = it.boundsInWindow() }) {
        RomTile(
            rom = rom,
            cover = rememberCover(model, rom),
            showBadge = showBadge,
            onOpen = { model.open(rom) },
            onMenu = { onMenu(rom, bounds[0]) },
        )
    }
}

/** Thẻ "Chơi tiếp": một game vừa chơi, bìa bên trái, tên + hệ máy, nút play tròn trắng bên phải. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(rom: RomInfo, cover: ImageBitmap?, onOpen: () -> Unit, onMenu: (Bounds?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bounds = remember { arrayOfNulls<Bounds>(1) }
    val shape = RoundedCornerShape(Ds.CardRadius)
    Row(
        Modifier
            .padding(horizontal = Ds.Pad)
            .fillMaxWidth()
            .clip(shape)
            .background(Ds.CardFill)
            .border(BorderStroke(1.dp, Ds.CardStroke), shape)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu(bounds[0])
                },
            )
            .padding(Ds.GapSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverBox(rom, cover, Modifier.size(88.dp).onGloballyPositioned { bounds[0] = it.boundsInWindow() }, Ds.CoverRadius - 4.dp, 32.sp)
        Spacer(Modifier.width(Ds.Gap))
        Column(Modifier.weight(1f)) {
            Text(rom.name, color = Rc.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                sysLabel(rom) + " · " + relativeTime(rom.lastPlayedAt),
                color = Rc.Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.width(Ds.Gap))
        Box(Modifier.size(Ds.BtnSize).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            RetroIcon("play", size = Ds.IconBtn, tint = Color.Black)
        }
        Spacer(Modifier.width(Ds.GapSm))
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
    tint: Color,
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
            val a0 = anchor ?: androidx.compose.ui.geometry.Rect(sw / 2 - 90f, sh / 3, sw / 2 + 90f, sh / 3 + 180f)
            val coverW = minOf(a0.width, a0.height)
            // Game ở sát mép dưới/trên màn hình hay bị thanh điều hướng che: đẩy bìa vào vùng nhìn thấy để không mất khúc nào.
            val grow = coverW * 0.08f
            val ay = a0.top.coerceIn(insetTop + grow / 2, (sh - insetBottom - coverW - grow / 2).coerceAtLeast(insetTop + grow / 2))
            val ax = a0.left.coerceIn(margin + grow / 2, (sw - margin - coverW - grow / 2).coerceAtLeast(margin))
            val a = androidx.compose.ui.geometry.Rect(ax, ay, ax + coverW, ay + coverW)
            val menuW = with(density) { 260.dp.toPx() }
            val dens = density.density
            val coverBottom = a.top + coverW + coverW * 0.04f
            val titleDp = 30f
            val n = actions.size
            // Bảng không bao giờ đè lên bìa: đặt dưới bìa nếu đủ chỗ, không thì đặt trên bìa;
            // cả hai đều thiếu thì chọn phía rộng hơn và thu nhỏ chiều cao dòng.
            val lift = coverW * 0.04f
            val spaceBelow = sh - insetBottom - (coverBottom + gap)
            val spaceAbove = (a.top - lift - gap) - insetTop
            val fullH = (titleDp + n * 48f) * dens
            val below = spaceBelow >= fullH || (spaceAbove < fullH && spaceBelow >= spaceAbove)
            val space = if (below) spaceBelow else spaceAbove
            val rowDp = (((space / dens) - titleDp) / n).coerceIn(34f, 48f)
            val panelH = minOf((titleDp + n * rowDp) * dens, space.coerceAtLeast(with(density) { 120.dp.toPx() }))
            val my = (if (below) coverBottom + gap else a.top - lift - gap - panelH)
                .coerceIn(insetTop.coerceAtMost(sh - panelH), (sh - insetBottom - panelH).coerceAtLeast(insetTop))
            // Game ở cột trái: bảng thẳng hàng cạnh trái bìa; game ở cột phải: thẳng hàng cạnh phải.
            val mx = (if (a.left + menuW + margin <= sw) a.left else a.right - menuW)
                .coerceIn(margin, (sw - menuW - margin).coerceAtLeast(margin))
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
                    .height(with(density) { panelH.toDp() })
                    .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(lerp(tint, Color.White, 0.10f), lerp(tint, Color.Black, 0.12f))))
                    .border(BorderStroke(1.dp, Ds.CardStroke), RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    rom.name,
                    color = Rc.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(titleDp.dp).padding(horizontal = 20.dp).wrapContentHeight(Alignment.CenterVertically),
                )
                actions.forEachIndexed { idx, act ->
                    if (idx > 0) Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(1.dp).background(Ds.Divider))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = act.onClick)
                            .height(rowDp.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            act.label,
                            color = if (act.danger) Rc.Danger else Rc.Text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        RetroIcon(act.icon, size = Ds.IconRow, tint = if (act.danger) Rc.Danger else Rc.Muted)
                    }
                }
            }
        }
    }
}

/**
 * Nội dung Cài đặt dùng chung cho tab Cài đặt (onClose = null) và bảng cài đặt ở màn ngang (onClose != null).
 * Có nhiều trang con (Tay cầm, Âm thanh, ...); nút Back của hệ thống quay về trang chính.
 */
@Composable
private fun SettingsContent(
    model: AppModel,
    onClose: (() -> Unit)?,
    onDeleteAll: () -> Unit,
    onPickFolder: () -> Unit,
) {
    // 0 = chính, 1 = Tay cầm, 2 = Giới thiệu, 3 = Âm thanh, 4 = Cảm ứng & rung, 5 = Dữ liệu, 6 = Game Boy, 7 = BIOS, 8 = Hình ảnh
    var settingsPage by remember { mutableStateOf(0) }
    androidx.activity.compose.BackHandler(enabled = settingsPage != 0) { settingsPage = 0 }
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
            if (onClose != null) SheetHeader("Cài đặt", onClose) else TabTitle("Cài đặt")
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
                    onPickFolder()
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

/** Tiêu đề lớn của một tab (cùng cỡ với SheetHeader / PageHeader). */
@Composable
private fun TabTitle(text: String) {
    Text(
        text,
        color = Rc.Text,
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Ô bìa: ảnh bìa cắt đầy ô, chưa có bìa thì hiện chữ cái đầu của tên game. */
@Composable
private fun CoverBox(rom: RomInfo, cover: ImageBitmap?, modifier: Modifier, radius: Dp, letterSize: androidx.compose.ui.unit.TextUnit) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF101114))
            .border(BorderStroke(1.dp, Ds.CardStroke), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(bitmap = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                rom.name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" },
                color = Rc.Muted,
                fontSize = letterSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Ô game trong lưới: bìa vuông (nhãn hệ máy ở góc, dấu sao nếu yêu thích), tên bên dưới. Chạm để chơi, giữ lâu để mở menu. */
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
            .clip(RoundedCornerShape(Ds.CoverRadius))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenu()
                },
            ),
    ) {
        Box {
            CoverBox(rom, cover, Modifier.fillMaxWidth().aspectRatio(1f), Ds.CoverRadius, 40.sp)
            if (showBadge) {
                Text(
                    sysLabel(rom),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color(0x73000000))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (rom.favorite) {
                RetroIcon("star", size = 18.dp, tint = Color(0xFFFFD60A), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
        Text(
            rom.name,
            color = Rc.Text,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
        Text(
            relativeDay(maxOf(rom.lastPlayedAt, rom.addedAt)),
            color = Rc.Muted,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp),
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
