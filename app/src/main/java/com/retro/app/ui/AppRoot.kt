package com.retro.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.Screen
import kotlinx.coroutines.delay

@Composable
fun AppRoot(model: AppModel) {
    Box(Modifier.fillMaxSize().background(Rc.Bg)) {
        when (model.screen) {
            Screen.LIBRARY -> LibraryScreen(model)
            Screen.PLAY -> PlayScreen(model)
        }

        if (model.loading) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        val msg = model.toast
        if (msg != null) {
            LaunchedEffect(msg) {
                delay(3200)
                if (model.toast == msg) model.toast = null
            }
            ToastBubble(msg, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Thông báo nổi ngắn ở đáy màn hình. Bảng Cài đặt là cửa sổ riêng nên tự vẽ thêm một bản trong bảng. */
@Composable
fun ToastBubble(msg: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .background(Color(0xE61E1F24), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(msg, color = Color.White, fontSize = 15.sp)
    }
}
