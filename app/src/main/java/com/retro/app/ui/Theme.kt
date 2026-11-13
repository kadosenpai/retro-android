package com.retro.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Bảng màu bám theo bản web. */
object Rc {
    val Bg = Color(0xFF08090C)
    val Card = Color(0xFF17181C)
    val Chip = Color(0xFF27272B)
    val Sheet = Color(0xFF101114)
    val Stroke = Color(0x2EFFFFFF)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0x99FFFFFF)
    val Faint = Color(0x66FFFFFF)
    val Danger = Color(0xFFFF6B6B)
}

@Composable
fun RetroTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Rc.Text,
        onPrimary = Color.Black,
        background = Rc.Bg,
        onBackground = Rc.Text,
        surface = Rc.Card,
        onSurface = Rc.Text,
        surfaceVariant = Rc.Chip,
        onSurfaceVariant = Rc.Muted,
    )
    MaterialTheme(colorScheme = colors, content = content)
}
