package com.retro.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.RenderEffect as AndroidRenderEffect

/*
 * Hiệu ứng "kính lỏng" cho thư viện.
 *
 * Cách làm: vùng nội dung nằm phía sau (nền + lưới game) được ghi vào một lớp đồ hoạ dùng chung ([GlassState]).
 * Mỗi phần tử kính ([glass]) vẽ lại đúng phần lớp đó nằm sau nó, qua một RenderEffect:
 * khúc xạ ở mép (shader AGSL, Android 13+), làm mờ, tăng bão hoà màu; rồi phủ lớp tint và viền sáng.
 * Android 12 chỉ có làm mờ (không khúc xạ); Android 11 trở xuống và khi tắt kính thì dùng nền mờ đặc.
 *
 * Lưu ý: phần tử kính KHÔNG được nằm bên trong vùng nguồn (nó sẽ vẽ lại chính mình). Trong vùng nguồn dùng [glassSurface].
 */

/** Nguồn nền dùng chung cho các phần tử kính. */
@Stable
class GlassState internal constructor(internal val layer: GraphicsLayer) {
    /** Vị trí góc trên trái của vùng nguồn trong cửa sổ. */
    var origin: Offset by mutableStateOf(Offset.Zero)
        internal set

    private var counter = 0

    /** Tăng mỗi khi nguồn được vẽ lại, để các phần tử kính vẽ lại theo (nội dung phía sau đã đổi). */
    internal val tick = mutableIntStateOf(0)

    internal fun bump() {
        counter++
        tick.intValue = counter
    }
}

@Composable
fun rememberGlassState(): GlassState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassState(layer) }
}

/** Nguồn kính hiện có (null = không dùng hiệu ứng nền, các phần tử kính tự chuyển sang nền mờ đặc). */
val LocalGlass = compositionLocalOf<GlassState?> { null }

/** Đặt lên vùng chứa nội dung nằm sau lớp kính (nền, lưới game). */
fun Modifier.glassSource(state: GlassState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInWindow() }
    .drawWithContent {
        state.layer.record { this@drawWithContent.drawContent() }
        drawLayer(state.layer)
        state.bump()
    }

/** Tính lại RenderEffect khi kích thước hoặc thông số đổi; còn lại dùng bản đã tạo. */
private class FxCache {
    var key: String = ""
}

/**
 * Phần tử kính: vẽ lại nền phía sau (khúc xạ + mờ), phủ [tint], viền sáng. Con của nó vẽ lên trên.
 * [blur] độ mờ; [bevel] bề rộng vùng mép bị khúc xạ; [refraction] độ lệch tối đa ở mép.
 */
fun Modifier.glass(
    radius: Dp,
    tint: Color = Rc.GlassTint,
    fallback: Color = Rc.GlassFallback,
    blur: Dp = 18.dp,
    bevel: Dp = 12.dp,
    refraction: Dp = 4.dp,
): Modifier = composed {
    val src = LocalGlass.current
    val density = LocalDensity.current
    val layer = rememberGraphicsLayer()
    val fx = remember { FxCache() }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val live = src != null && Build.VERSION.SDK_INT >= 31
    this
        .shadow(10.dp, RoundedCornerShape(radius), clip = false, ambientColor = Color(0x59000000), spotColor = Color(0x59000000))
        .onGloballyPositioned { pos = it.positionInWindow() }
        .drawWithContent {
            val w = size.width
            val h = size.height
            val r = with(density) { radius.toPx() }.coerceAtMost(minOf(w, h) / 2f)
            val path = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r, r))) }
            if (live && src != null && w > 0f && h > 0f && Build.VERSION.SDK_INT >= 31) {
                @Suppress("UNUSED_VARIABLE")
                val t = src.tick.intValue // đăng ký theo dõi: nền đổi thì vẽ lại
                val o = src.origin
                val key = "$w|$h|$r|$blur|$bevel|$refraction|${density.density}"
                if (fx.key != key) {
                    fx.key = key
                    layer.renderEffect = buildGlassEffect(
                        w, h, r,
                        with(density) { blur.toPx() },
                        with(density) { bevel.toPx() },
                        with(density) { refraction.toPx() },
                    )
                }
                layer.record {
                    translate(left = o.x - pos.x, top = o.y - pos.y) { drawLayer(src.layer) }
                }
                clipPath(path) { drawLayer(layer) }
                drawPath(path, tint)
            } else {
                drawPath(path, fallback)
            }
            drawGlassEdges(path, w, h)
            drawContent()
        }
}

/** Mặt kính nhẹ (không đọc nền): tint + viền sáng. Dùng cho phần tử nằm TRONG vùng nguồn, ví dụ thẻ trong lưới. */
fun Modifier.glassSurface(radius: Dp, tint: Color = Color(0x1AFFFFFF)): Modifier = composed {
    val density = LocalDensity.current
    this.drawWithContent {
        val w = size.width
        val h = size.height
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(w, h) / 2f)
        val path = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r, r))) }
        drawPath(path, tint)
        drawGlassEdges(path, w, h)
        drawContent()
    }
}

/** Viền sáng chéo (sáng ở góc trên trái và dưới phải) và vệt phản chiếu mảnh dọc mép trên. */
private fun DrawScope.drawGlassEdges(path: Path, w: Float, h: Float) {
    val sw = 1.dp.toPx()
    drawPath(
        path,
        Brush.linearGradient(
            0f to Color(0x59FFFFFF),
            0.4f to Color(0x0DFFFFFF),
            0.6f to Color(0x0DFFFFFF),
            1f to Color(0x33FFFFFF),
            start = Offset.Zero,
            end = Offset(w, h),
        ),
        style = Stroke(sw),
    )
    drawPath(
        path,
        Brush.verticalGradient(
            0f to Color(0x1FFFFFFF),
            0.25f to Color.Transparent,
            startY = 0f,
            endY = h,
        ),
        style = Stroke(sw * 2f),
    )
}

/**
 * Chuỗi hiệu ứng: khúc xạ (Android 13+) → tăng bão hoà và sáng → làm mờ.
 * Làm mờ đặt cuối nên mép khúc xạ mượt, không bị răng cưa.
 */
@RequiresApi(31)
private fun buildGlassEffect(w: Float, h: Float, radius: Float, blurPx: Float, bevelPx: Float, strengthPx: Float): RenderEffect {
    var e: AndroidRenderEffect? = null
    if (Build.VERSION.SDK_INT >= 33 && strengthPx > 0f) {
        val shader = RuntimeShader(REFRACT_AGSL)
        shader.setFloatUniform("size", w, h)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("bevel", maxOf(bevelPx, 1f))
        shader.setFloatUniform("strength", strengthPx)
        e = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
    }
    val cm = ColorMatrix()
    cm.setSaturation(1.25f)
    cm.postConcat(
        ColorMatrix(
            floatArrayOf(
                1.05f, 0f, 0f, 0f, 0f,
                0f, 1.05f, 0f, 0f, 0f,
                0f, 0f, 1.05f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    val filter = ColorMatrixColorFilter(cm)
    var chain: AndroidRenderEffect = if (e != null) {
        AndroidRenderEffect.createColorFilterEffect(filter, e)
    } else {
        AndroidRenderEffect.createColorFilterEffect(filter)
    }
    if (blurPx > 0.5f) {
        chain = AndroidRenderEffect.createBlurEffect(blurPx, blurPx, chain, Shader.TileMode.CLAMP)
    }
    return chain.asComposeRenderEffect()
}

/**
 * Khúc xạ ở mép hình chữ nhật bo góc: điểm gần mép lấy màu từ vị trí lùi vào trong,
 * càng sát mép càng lệch nhiều (bình phương), ở giữa không lệch.
 */
private const val REFRACT_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bevel;
uniform float strength;

half4 main(float2 p) {
    float2 c = p - size * 0.5;
    float2 hs = size * 0.5;
    float2 q = abs(c) - (hs - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float depth = clamp(-d / bevel, 0.0, 1.0);
    float k = 1.0 - depth;
    k = k * k;
    float2 n;
    if (q.x > 0.0 || q.y > 0.0) {
        n = normalize(max(q, float2(0.0))) * sign(c);
    } else if (q.x > q.y) {
        n = float2(sign(c.x), 0.0);
    } else {
        n = float2(0.0, sign(c.y));
    }
    return content.eval(p - n * k * strength);
}
"""
