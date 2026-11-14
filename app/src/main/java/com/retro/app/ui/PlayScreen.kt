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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    sticky = s.sticky,
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
                        sticky = s.sticky,
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
                    .background(Rc.Solid.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
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


/** Bảng chỉnh bố cục gọn: tiêu đề + nút Mặc định/Xong cùng hàng, gợi ý một dòng, thanh trượt kích thước. */
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
    val shape = RoundedCornerShape(24.dp)
    val current = if (selected != null) layout.scaleOf(selected) else 1f
    if (landscape) {
        // Màn ngang thấp: gộp thành một hàng mỏng để không che các nút đang chỉnh.
        Row(
            modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Rc.Solid.copy(alpha = 0.94f))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Rc.Stroke), RoundedCornerShape(26.dp))
                .padding(start = 18.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(150.dp)) {
                Text("Chỉnh nút ảo", color = Rc.Text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(
                    if (selected == null) "Chạm một nút để chọn" else "${selected.label} · %.1f×".format(current),
                    color = Rc.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Slider(
                value = current,
                onValueChange = { v -> if (selected != null) onScale(selected, v - current) },
                valueRange = PadLayout.MIN_SCALE..PadLayout.MAX_SCALE,
                steps = 14,
                enabled = selected != null,
                modifier = Modifier.weight(1f).height(36.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Rc.GlassStrong,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = Rc.Faint,
                    disabledActiveTrackColor = Rc.Faint,
                    disabledInactiveTrackColor = Rc.GlassStrong,
                    disabledActiveTickColor = Color.Transparent,
                    disabledInactiveTickColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(10.dp))
            PillButton("Mặc định", primary = false, onClick = onReset, modifier = Modifier.heightIn(min = 40.dp))
            Spacer(Modifier.width(8.dp))
            PillButton("Xong", onClick = onDone, modifier = Modifier.heightIn(min = 40.dp))
        }
        return
    }
    Column(
        modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Rc.Solid.copy(alpha = 0.94f))
            .border(androidx.compose.foundation.BorderStroke(1.dp, Rc.Stroke), shape)
            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (landscape) "Chỉnh nút ảo (ngang)" else "Chỉnh nút ảo (dọc)",
                color = Rc.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            PillButton("Mặc định", primary = false, onClick = onReset, modifier = Modifier.heightIn(min = 40.dp))
            Spacer(Modifier.width(8.dp))
            PillButton("Xong", onClick = onDone, modifier = Modifier.heightIn(min = 40.dp))
        }
        Text(
            if (selected == null) "Chạm một nút để chọn, kéo để di chuyển."
            else "${selected.label} · %.1f×".format(current),
            color = Rc.Muted,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Slider(
            value = current,
            onValueChange = { v -> if (selected != null) onScale(selected, v - current) },
            valueRange = PadLayout.MIN_SCALE..PadLayout.MAX_SCALE,
            steps = 14,
            enabled = selected != null,
            modifier = Modifier.height(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Rc.GlassStrong,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = Rc.Faint,
                disabledActiveTrackColor = Rc.Faint,
                disabledInactiveTrackColor = Rc.GlassStrong,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
        )
    }
}
