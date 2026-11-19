package com.retro.app.ui

import android.app.Activity
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import com.retro.app.render.computeViewport
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.retro.app.AppModel
import com.retro.app.core.EmulatorEngine
import com.retro.app.data.GameSystem
import com.retro.app.render.DisplayAspect
import com.retro.app.render.GameGLView

@Composable
fun PlayScreen(model: AppModel) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.screenWidthDp > cfg.screenHeightDp
    val s = model.settings
    val isNds = model.current?.system == "nds"

    // Toàn màn hình khi chơi.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowInsetsControllerCompat(it, view) }
        ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctrl?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Hệ có tỉ lệ khung riêng (PlayStation, Mega Drive) thì ép tỉ lệ khi vẽ.
    val aspect = GameSystem.fromId(model.current?.system).aspect
    DisposableEffect(aspect) {
        DisplayAspect.value = aspect
        onDispose { DisplayAspect.value = 0f }
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
                onExit = { model.exitToLibrary() },
                onQuickSave = { model.saveSlot(1) },
                onQuickLoad = { model.loadSlot(1) },
                skin = when (model.current?.system) { "snes" -> "snes"; "nds" -> "nds"; else -> "smoke" },
                onTouch = { x, y, down -> EmulatorEngine.setTouch(x, y, down) },
                game = { m -> GameSurface(m) },
                hud = { m, c, sp -> if (s.showStats) StatusHud(model.fps, model.coreMs, m, c, sp) },
                modifier = Modifier.background(androidx.compose.ui.graphics.Color.Black),
            )
        } else if (landscape) {
            Box(Modifier.fillMaxSize().dsTouch(isNds)) { GameSurface(Modifier.fillMaxSize()) }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).windowInsetsPadding(WindowInsets.safeDrawing).dsTouch(isNds)) {
                    GameSurface(Modifier.fillMaxSize())
                }
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
                    .pointerInput(Unit) {
                        // Chạm nhanh mở menu; giữ đủ lâu thì thoát thẳng về thư viện.
                        awaitEachGesture {
                            awaitFirstDown()
                            val t0 = SystemClock.uptimeMillis()
                            val up = withTimeoutOrNull(EXIT_HOLD_MS) { waitForUpOrCancellation() }
                            if (up != null) {
                                model.menuOpen = true
                            } else if (SystemClock.uptimeMillis() - t0 >= EXIT_HOLD_MS - 20) {
                                if (s.haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                model.exitToLibrary()
                                waitForUpOrCancellation()
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("▲", color = Rc.Text, fontSize = 16.sp)
            }
        }

        if (s.showStats && !s.showPad) {
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

/**
 * NDS khi ẩn phím ảo: khung hình (256x384) hiển thị nguyên cả hai màn; chạm vào nửa dưới thì gửi cảm ứng cho lõi.
 */
private fun Modifier.dsTouch(enabled: Boolean): Modifier = if (!enabled) this else pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val vp = computeViewport(size.width, size.height, 256, 384)

        fun send(p: Offset, pressed: Boolean) {
            if (vp.width <= 0 || vp.height <= 0) return
            val fx = ((p.x - vp.x) / vp.width).coerceIn(0f, 1f)
            val fy = ((p.y - vp.y) / vp.height).coerceIn(0.5f, 1f)
            EmulatorEngine.setTouch(
                (fx * 65535f - 32768f).toInt().coerceIn(-32768, 32767),
                (fy * 65535f - 32768f).toInt().coerceIn(-32768, 32767),
                pressed,
            )
        }

        // Chỉ nhận khi ngón đặt xuống ở nửa dưới của khung hình.
        val startsOnBottom = vp.height > 0 && down.position.y >= vp.y + vp.height / 2f
        if (!startsOnBottom) return@awaitEachGesture
        send(down.position, true)
        while (true) {
            val ev = awaitPointerEvent()
            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (!c.pressed) {
                send(c.position, false)
                return@awaitEachGesture
            }
            send(c.position, true)
            c.consume()
        }
        EmulatorEngine.setTouch(0, 0, false)
    }
}
