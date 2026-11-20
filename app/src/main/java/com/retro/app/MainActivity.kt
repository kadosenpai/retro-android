package com.retro.app

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.retro.app.core.EmulatorEngine
import com.retro.app.core.Pad
import com.retro.app.ui.AppRoot
import com.retro.app.ui.RetroTheme

class MainActivity : ComponentActivity() {
    private val model: AppModel get() = (application as RetroApp).model

    private var keyMask = 0
    private var axisMask = 0
    private var hatMask = 0
    private var lastHatKey = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            RetroTheme { AppRoot(model) }
        }
    }

    override fun onPause() {
        super.onPause()
        model.autoSave()
        model.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        model.resumeGame()
    }

    private fun playing() = model.screen == Screen.PLAY && !model.menuOpen

    private fun bitFor(code: Int): Int = model.controllerMap.bitFor(code)

    private fun pushHardware() = EmulatorEngine.setHardwareMask(keyMask or axisMask or hatMask)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (model.screen == Screen.PLAY) {
            if (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE && event.action == KeyEvent.ACTION_DOWN) {
                model.menuOpen = true
                return true
            }
            val bit = bitFor(event.keyCode)
            if (bit != 0) {
                if (playing()) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> keyMask = keyMask or bit
                        KeyEvent.ACTION_UP -> keyMask = keyMask and bit.inv()
                    }
                    pushHardware()
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    keyMask = keyMask and bit.inv()
                    pushHardware()
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** D-pad kiểu "hat" (trục) đổi thành mã phím D-pad ảo để dùng chung bảng gán nút; 0 = không bấm. */
    private fun hatKey(event: MotionEvent): Int {
        val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        return when {
            hx < -0.5f -> KeyEvent.KEYCODE_DPAD_LEFT
            hx > 0.5f -> KeyEvent.KEYCODE_DPAD_RIGHT
            hy < -0.5f -> KeyEvent.KEYCODE_DPAD_UP
            hy > 0.5f -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> 0
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val joystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (joystick && event.action == MotionEvent.ACTION_MOVE) {
            // Đang chờ gán nút: hướng D-pad kiểu hat (Joy-Con trái...) cũng được nhận như một nút.
            val key = hatKey(event)
            val capture = model.hatCapture
            if (capture != null) {
                if (key != 0 && key != lastHatKey) capture(key)
                lastHatKey = key
                return true
            }
            lastHatKey = key
            if (model.screen == Screen.PLAY) {
                // Cần analog trái luôn là hướng; D-pad hat đi qua bảng gán nút như phím D-pad thường.
                val x = event.getAxisValue(MotionEvent.AXIS_X)
                val y = event.getAxisValue(MotionEvent.AXIS_Y)
                var m = 0
                if (x > 0.5f) m = m or Pad.RIGHT
                if (x < -0.5f) m = m or Pad.LEFT
                if (y > 0.5f) m = m or Pad.DOWN
                if (y < -0.5f) m = m or Pad.UP
                axisMask = if (playing()) m else 0
                hatMask = if (playing() && key != 0) bitFor(key) else 0
                pushHardware()
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }
}
