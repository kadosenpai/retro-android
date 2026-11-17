package com.retro.app.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.retro.app.core.EmulatorEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val VERTEX = """
attribute vec2 aPos;
varying vec2 vUv;
void main() {
  vUv = vec2(aPos.x * 0.5 + 0.5, 0.5 - aPos.y * 0.5);
  gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

// Hiển thị thô (lấy mẫu gần nhất); uBgr đảo kênh màu cho khung XRGB8888.
private const val FRAGMENT = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vUv;
uniform sampler2D uTex;
uniform float uBgr;

void main() {
  vec3 c = texture2D(uTex, vUv).rgb;
  gl_FragColor = vec4(uBgr > 0.5 ? c.bgr : c, 1.0);
}
"""

class GameRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var tex = 0
    private var uTex = 0
    private var uBgr = 0
    private var aPos = 0

    private val quad: FloatBuffer =
        ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

    private val frame: ByteBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 4).order(ByteOrder.nativeOrder())
    private val info = IntArray(3)

    private var surfW = 0
    private var surfH = 0
    private var texW = 0
    private var texH = 0
    private var texFmt = 0
    private var lastSerial = -1L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = buildProgram()
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uTex = GLES20.glGetUniformLocation(program, "uTex")
        uBgr = GLES20.glGetUniformLocation(program, "uBgr")

        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)
        tex = t[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        texW = 0
        texH = 0
        texFmt = 0
        lastSerial = -1L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfW = width
        surfH = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glViewport(0, 0, surfW, surfH)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        frame.clear()
        val serial = EmulatorEngine.copyFrame(frame, info)
        val w = info[0]
        val h = info[1]
        val fmt = info[2]
        if (serial < 0 || w <= 0 || h <= 0 || fmt == 0) return

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)

        val is565 = fmt == 2
        val format = if (is565) GLES20.GL_RGB else GLES20.GL_RGBA
        val type = if (is565) GLES20.GL_UNSIGNED_SHORT_5_6_5 else GLES20.GL_UNSIGNED_BYTE
        val resized = w != texW || h != texH || fmt != texFmt
        if (resized || serial != lastSerial) {
            frame.position(0)
            if (resized) {
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, w, h, 0, format, type, frame)
                texW = w
                texH = h
                texFmt = fmt
            } else {
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, w, h, format, type, frame)
            }
            lastSerial = serial
        }

        val vp = computeViewport(surfW, surfH, w, h)
        if (vp.width <= 0 || vp.height <= 0) return
        GLES20.glViewport(vp.x, surfH - vp.y - vp.height, vp.width, vp.height)
        GLES20.glUseProgram(program)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glUniform1f(uBgr, if (is565) 0f else 1f)
        quad.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun buildProgram(): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        check(ok[0] != 0) { "Liên kết shader lỗi: " + GLES20.glGetProgramInfoLog(p) }
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] != 0) { "Biên dịch shader lỗi: " + GLES20.glGetShaderInfoLog(s) }
        return s
    }
}

/** View hiển thị game bằng OpenGL ES 2. */
class GameGLView(context: android.content.Context) : GLSurfaceView(context) {
    val renderer = GameRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
