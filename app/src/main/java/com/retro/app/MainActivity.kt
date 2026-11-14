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

    private fun pushHardware() = EmulatorEngine.setHardwareMask(keyMask or axisMask)

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

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val joystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (joystick && event.action == MotionEvent.ACTION_MOVE && model.screen == Screen.PLAY) {
            val x = if (event.getAxisValue(MotionEvent.AXIS_HAT_X) != 0f) event.getAxisValue(MotionEvent.AXIS_HAT_X) else event.getAxisValue(MotionEvent.AXIS_X)
            val y = if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) != 0f) event.getAxisValue(MotionEvent.AXIS_HAT_Y) else event.getAxisValue(MotionEvent.AXIS_Y)
            var m = 0
            if (x > 0.5f) m = m or Pad.RIGHT
            if (x < -0.5f) m = m or Pad.LEFT
            if (y > 0.5f) m = m or Pad.DOWN
            if (y < -0.5f) m = m or Pad.UP
            axisMask = if (playing()) m else 0
            pushHardware()
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
