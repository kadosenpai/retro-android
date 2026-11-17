package com.retro.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HudColor = Color.White.copy(alpha = 0.8f)

private fun batteryPercent(i: Intent?): Int {
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (level < 0 || scale <= 0) -1 else (level * 100 / scale).coerceIn(0, 100)
}

/**
 * Hàng thông tin nằm trong vùng đen phía trên game của skin:
 * bên trái là fps và thời gian lõi, bên phải là giờ và phần trăm pin.
 */
@Composable
fun StatusHud(fps: Float, coreMs: Float, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var time by remember { mutableStateOf(fmt.format(Date())) }
    var pct by remember { mutableStateOf(-1) }

    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> pct = batteryPercent(intent)
                    else -> time = fmt.format(Date())
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // Battery là broadcast dính: lần đăng ký đầu trả về giá trị hiện tại.
        val sticky = ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        pct = batteryPercent(sticky)
        time = fmt.format(Date())
        onDispose { ctx.unregisterReceiver(receiver) }
    }

    Row(
        modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("%.1f fps · lõi %.1f ms".format(fps, coreMs), color = HudColor, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, color = HudColor, fontSize = 12.sp)
            if (pct >= 0) {
                Spacer(Modifier.width(10.dp))
                BatteryIcon(pct)
                Spacer(Modifier.width(4.dp))
                Text("$pct%", color = HudColor, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BatteryIcon(pct: Int) {
    Canvas(Modifier.size(width = 20.dp, height = 10.dp)) {
        val stroke = 1.2.dp.toPx()
        val nub = 2.dp.toPx()
        val bodyW = size.width - nub
        val r = CornerRadius(2.5.dp.toPx())
        drawRoundRect(HudColor, Offset(stroke / 2, stroke / 2), Size(bodyW - stroke, size.height - stroke), r, Stroke(stroke))
        drawRoundRect(
            HudColor,
            Offset(bodyW, size.height * 0.3f),
            Size(nub, size.height * 0.4f),
            CornerRadius(nub / 2),
        )
        val pad = stroke + 1.dp.toPx()
        val fw = (bodyW - 2 * pad) * pct / 100f
        if (fw > 0f) drawRoundRect(HudColor, Offset(pad, pad), Size(fw, size.height - 2 * pad), CornerRadius(1.dp.toPx()))
    }
}
