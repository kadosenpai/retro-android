package com.retro.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // Toàn màn hình khi chơi.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowInsetsControllerCompat(it, view) }
        ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Mở menu thì tạm dừng game; xong thì chạy tiếp.
    DisposableEffect(model.menuOpen) {
        if (model.menuOpen) EmulatorEngine.pause() else EmulatorEngine.resume()
        onDispose { }
    }
    BackHandler(enabled = !model.menuOpen) { model.menuOpen = true }

    Box(Modifier.fillMaxSize().background(Rc.Bg)) {
        if (s.showPad) {
            // Skin phủ toàn màn hình, màn game nằm trong khung của skin.
            SkinPad(
                haptics = s.haptics,
                sticky = s.sticky,
                onMask = { EmulatorEngine.setTouchMask(it) },
                onMenu = { model.menuOpen = true },
                onQuickSave = { model.saveSlot(1) },
                onQuickLoad = { model.loadSlot(1) },
                game = { m -> GameSurface(m) },
                modifier = Modifier.background(androidx.compose.ui.graphics.Color.Black),
            )
        } else if (landscape) {
            GameSurface(Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize()) {
                GameSurface(Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(WindowInsets.safeDrawing))
            }
        }

        if (!s.showPad) {
            // Không có nút nhà khi ẩn phím ảo: chạm vào góc trên phải để mở menu.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
                    .background(Rc.Solid.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
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

        if (model.menuOpen) PlayMenuSheet(model) { model.menuOpen = false }
    }
}

@Composable
private fun GameSurface(modifier: Modifier) {
    AndroidView(
        factory = { ctx -> GameGLView(ctx) },
        modifier = modifier,
    )
}
