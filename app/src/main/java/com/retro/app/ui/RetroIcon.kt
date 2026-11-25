package com.retro.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Vẽ biểu tượng nét mảnh theo tên (xem [RetroIconData]); khung gốc 24x24, nét 2. */
@Composable
fun RetroIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Rc.Muted,
) {
    val paths = remember(name) {
        (RetroIconData.paths[name] ?: emptyList()).map { PathParser().parsePathString(it).toPath() }
    }
    val strokeWidth = RetroIconData.strokeWidth[name] ?: 2f
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(scaleX = k, scaleY = k, pivot = Offset.Zero) {
            for (p in paths) {
                drawPath(
                    p,
                    color = tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}
