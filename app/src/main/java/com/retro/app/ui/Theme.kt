package com.retro.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.retro.app.R

/** Bảng màu bám theo bản web (kính trắng đen). */
object Rc {
    val Bg = Color(0xFF08090C)
    /** Kính: trắng 7% (như --glass của web). */
    val Card = Color(0x12FFFFFF)
    val Glass = Color(0x12FFFFFF)
    /** Kính đậm: trắng 14% (như --glass-strong). */
    val GlassStrong = Color(0x24FFFFFF)
    /** Nền đặc cho những chỗ không được trong suốt (hộp thoại, menu thả xuống, thanh nổi trên game). */
    val Solid = Color(0xFF17181C)
    val Chip = Color(0xFF27272B)
    /** Bảng (sheet): rgba(18,18,21,0.9). */
    val Sheet = Color(0xE6121215)
    val Stroke = Color(0x24FFFFFF)
    /** Đường kẻ mảnh giữa các hàng (--faint của web). */
    val Line = Color(0x14FFFFFF)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0x8CFFFFFF)
    val Faint = Color(0x66FFFFFF)
    val Danger = Color(0xFFFF6B6B)
}

/** Phông Inter, giống bản web. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun Typography.withFont(f: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

/**
 * Nền chuyển sắc của khung ứng dụng web:
 * radial-gradient(120% 80% at 0 0, #1f2026 0%, #0d0e12 55%, #08090c 100%).
 */
fun Modifier.retroBackground(): Modifier = this.drawBehind {
    drawRect(Rc.Bg)
    val w = size.width
    val h = size.height
    if (w > 0f && h > 0f) {
        val radius = 1.2f * w
        val sy = (0.8f * h) / radius
        scale(scaleX = 1f, scaleY = sy, pivot = Offset.Zero) {
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color(0xFF1F2026),
                    0.55f to Color(0xFF0D0E12),
                    1f to Color(0xFF08090C),
                    center = Offset.Zero,
                    radius = radius,
                ),
                size = Size(w, h / sy),
            )
        }
    }
}

@Composable
fun RetroTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Rc.Text,
        onPrimary = Color.Black,
        background = Rc.Bg,
        onBackground = Rc.Text,
        surface = Rc.Solid,
        onSurface = Rc.Text,
        surfaceVariant = Rc.Chip,
        onSurfaceVariant = Rc.Muted,
        surfaceContainer = Rc.Solid,
        surfaceContainerHigh = Rc.Solid,
        surfaceContainerHighest = Rc.Solid,
    )
    MaterialTheme(colorScheme = colors, typography = Typography().withFont(InterFamily)) {
        ProvideTextStyle(TextStyle(fontFamily = InterFamily)) {
            content()
        }
    }
}
