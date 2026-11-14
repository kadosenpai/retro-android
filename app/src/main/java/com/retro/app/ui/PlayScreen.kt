package com.retro.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.retro.app.data.PadControl
import com.retro.app.data.PadLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.retro.app.AppModel
import com.retro.app.core.EmulatorEngine
import com.retro.app.render.GameGLView

@Composable
fun PlayScreen(model: AppModel) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    val s = model.settings
    val editing = model.editingLayout
    val layout = if (landscape) model.layoutLandscape else model.layoutPortrait
    var selected by remember { mutableStateOf<PadControl?>(null) }

    // Toàn màn hình khi chơi.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowInsetsControllerCompat(it, view) }
        ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Mở menu hoặc đang chỉnh bố cục thì tạm dừng game; xong thì chạy tiếp.
    DisposableEffect(model.menuOpen || editing) {
        if (model.menuOpen || editing) EmulatorEngine.pause() else EmulatorEngine.resume()
        onDispose { }
    }
    BackHandler(enabled = editing) {
        model.editingLayout = false
        selected = null
    }
    BackHandler(enabled = !model.menuOpen && !editing) { model.menuOpen = true }

    val padWeightHeight = (cfg.screenHeightDp * 0.45f).dp

    Box(Modifier.fillMaxSize().background(Rc.Bg)) {
        if (landscape) {
            GameSurface(model, Modifier.fillMaxSize())
            if (s.showPad || editing) {
                TouchOverlay(
                    haptics = s.haptics,
                    layout = layout,
                    editing = editing,
                    selected = selected,
                    onSelect = { selected = it },
                    onLayoutChange = { model.setLayout(landscape, it) },
                    onMask = { EmulatorEngine.setTouchMask(it) },
                    onMenu = { model.menuOpen = true },
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                GameSurface(
                    model,
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )
                if (s.showPad || editing) {
                    TouchOverlay(
                        haptics = s.haptics,
                    layout = layout,
                    editing = editing,
                    selected = selected,
                    onSelect = { selected = it },
                    onLayoutChange = { model.setLayout(landscape, it) },
                        onMask = { EmulatorEngine.setTouchMask(it) },
                        onMenu = { model.menuOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(padWeightHeight)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(bottom = 6.dp),
                    )
                }
            }
        }

        if (!s.showPad && !editing) {
            // Không có nút ▲ khi ẩn phím ảo: chạm vào góc trên phải để mở menu.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
                    .background(Rc.Card.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .clickableNoRipple { model.menuOpen = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("▲", color = Rc.Text, fontSize = 16.sp)
            }
        }

        if (s.showStats) {
            Text(
                "%.1f fps · lõi %.1f ms".format(model.fps, model.coreMs),
                color = Rc.Muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            )
        }

        if (editing) {
            LayoutEditBar(
                landscape = landscape,
                selected = selected,
                layout = layout,
                onScale = { c, d -> model.setLayout(landscape, layout.scaled(c, d)) },
                onReset = {
                    model.setLayout(landscape, PadLayout())
                    selected = null
                },
                onDone = {
                    model.editingLayout = false
                    selected = null
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 8.dp),
            )
        }

        if (model.menuOpen) PlayMenuSheet(model) { model.menuOpen = false }
    }
}

@Composable
private fun GameSurface(model: AppModel, modifier: Modifier) {
    val s = model.settings
    AndroidView(
        factory = { ctx -> GameGLView(ctx) },
        modifier = modifier,
        update = { v ->
            v.renderer.scale = s.scale
            v.renderer.filter = s.filter
        },
    )
}


@Composable
private fun EditChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(Rc.Chip, RoundedCornerShape(50))
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Rc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Thanh công cụ khi chỉnh bố cục: đổi kích thước nút đang chọn, đặt lại, xong. */
@Composable
private fun LayoutEditBar(
    landscape: Boolean,
    selected: PadControl?,
    layout: PadLayout,
    onScale: (PadControl, Float) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Rc.Card.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (landscape) "Chỉnh nút ảo (màn ngang)" else "Chỉnh nút ảo (màn dọc)",
            color = Rc.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        if (selected == null) {
            Text("Chạm một nút rồi kéo để di chuyển", color = Rc.Muted, fontSize = 13.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditChip("−") { onScale(selected, -0.1f) }
                Text(
                    "${selected.label} · %.1f×".format(layout.scaleOf(selected)),
                    color = Rc.Text,
                    fontSize = 14.sp,
                )
                EditChip("+") { onScale(selected, 0.1f) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditChip("Đặt lại", onReset)
            EditChip("Xong", onDone)
        }
    }
}
