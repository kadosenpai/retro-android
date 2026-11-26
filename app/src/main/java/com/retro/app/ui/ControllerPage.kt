package com.retro.app.ui

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.app.AppModel
import com.retro.app.data.GbaButton
import com.retro.app.data.SCAN_BASE
import com.retro.app.data.hardwareKeyCode

/** Tên các tay cầm đang kết nối. */
private fun connectedPads(): List<String> =
    InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
        .filter { d ->
            !d.isVirtual &&
                ((d.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (d.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
        }
        .map { it.name }

private fun keyName(code: Int): String = if (code >= SCAN_BASE) "Nút phần cứng ${code - SCAN_BASE}" else when (code) {
    KeyEvent.KEYCODE_DPAD_UP -> "Mũi tên lên"
    KeyEvent.KEYCODE_DPAD_DOWN -> "Mũi tên xuống"
    KeyEvent.KEYCODE_DPAD_LEFT -> "Mũi tên trái"
    KeyEvent.KEYCODE_DPAD_RIGHT -> "Mũi tên phải"
    KeyEvent.KEYCODE_DPAD_CENTER -> "Giữa"
    KeyEvent.KEYCODE_BUTTON_A -> "Nút A"
    KeyEvent.KEYCODE_BUTTON_B -> "Nút B"
    KeyEvent.KEYCODE_BUTTON_X -> "Nút X"
    KeyEvent.KEYCODE_BUTTON_Y -> "Nút Y"
    KeyEvent.KEYCODE_BUTTON_C -> "Nút C"
    KeyEvent.KEYCODE_BUTTON_Z -> "Nút Z"
    KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
    KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
    KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
    KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
    KeyEvent.KEYCODE_BUTTON_START -> "Start"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    KeyEvent.KEYCODE_BUTTON_THUMBL -> "Nhấn cần trái"
    KeyEvent.KEYCODE_BUTTON_THUMBR -> "Nhấn cần phải"
    else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
}

/** Trang Tay cầm: xem tay cầm đang kết nối, gán lại từng nút GBA, đặt lại mặc định. */
@Composable
fun ControllerPage(model: AppModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var pads by remember { mutableStateOf(connectedPads()) }
    DisposableEffect(Unit) {
        val im = ctx.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                pads = connectedPads()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                pads = connectedPads()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                pads = connectedPads()
            }
        }
        im.registerInputDeviceListener(listener, null)
        onDispose { im.unregisterInputDeviceListener(listener) }
    }
    var capturing by remember { mutableStateOf<GbaButton?>(null) }

    PageHeader("Tay cầm", onBack)
    Spacer(Modifier.height(Ds.SectionGap))

    GroupCard {
        Text(
            if (pads.isEmpty()) "Chưa thấy tay cầm. Kết nối Bluetooth hoặc cáp, rồi bấm một nút."
            else "Đang kết nối: " + pads.joinToString(", "),
            color = if (pads.isEmpty()) Rc.Muted else Rc.Text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }

    SectionTitle("Gán nút")
    GroupCard {
        GbaButton.values().forEachIndexed { i, b ->
            if (i > 0) RowDivider(inset = 16.dp)
            val keys = model.controllerMap.keys[b].orEmpty().sorted()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { capturing = b }
                    .heightIn(min = Ds.RowHeight)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(b.label, color = Rc.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    if (keys.isEmpty()) "Chưa gán" else keys.joinToString(", ") { keyName(it) },
                    color = Rc.Muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .background(Rc.Chip, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text("Gán", color = Rc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    GroupCard(Modifier.padding(top = 12.dp)) { ActionRow("Đặt lại mặc định") { model.resetControllerMap() } }
    Text(
        "Cần analog trái luôn là hướng. Nút Mode/Home mở menu, không gán lại được.",
        color = Rc.Faint,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
    )

    capturing?.let { b ->
        val focus = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { capturing = null },
            title = { Text("Gán nút ${b.label}") },
            text = {
                LaunchedEffect(Unit) { focus.requestFocus() }
                // D-pad kiểu hat (trục) không phát phím nên MainActivity chuyển giúp qua hatCapture.
                DisposableEffect(b) {
                    model.hatCapture = { code ->
                        model.assignKey(b, code)
                        capturing = null
                    }
                    onDispose { model.hatCapture = null }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .focusRequester(focus)
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            val rawCode = ev.nativeKeyEvent.keyCode
                            // Phím không tên (keyCode 0) như D-pad Joy-Con trái được phân biệt bằng scanCode.
                            val code = hardwareKeyCode(rawCode, ev.nativeKeyEvent.scanCode)
                            when {
                                ev.type != KeyEventType.KeyDown -> true
                                rawCode == KeyEvent.KEYCODE_BACK -> {
                                    capturing = null
                                    true
                                }
                                rawCode == KeyEvent.KEYCODE_BUTTON_MODE -> {
                                    model.showToast("Nút này dùng để mở menu")
                                    true
                                }
                                // Nhận mọi phím phát ra từ tay cầm (kể cả mã lạ), không chỉ các nút chuẩn.
                                KeyEvent.isGamepadButton(rawCode) || code >= SCAN_BASE ||
                                    rawCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER ||
                                    ev.nativeKeyEvent.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD or InputDevice.SOURCE_JOYSTICK) != 0 -> {
                                    model.assignKey(b, code)
                                    capturing = null
                                    true
                                }
                                else -> {
                                    model.showToast("Không nhận nút này (mã ${KeyEvent.keyCodeToString(rawCode)})")
                                    true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nhấn một nút trên tay cầm…",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { capturing = null }) { Text("Huỷ") } },
        )
    }
}
