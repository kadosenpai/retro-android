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

// Bộ lọc hình, uMode: 0 = pixel thô, 1 = sắc nét (sharp bilinear), 2 = LCD, 3 = CRT.
// Texture luôn để GL_LINEAR; chế độ thô tự lấy mẫu đúng giữa texel nên vẫn ra "gần nhất".
// Sắc nét: giữ phần giữa mỗi texel đặc, chỉ trộn màu trong khoảng một pixel màn hình ở mép texel.
// Phóng nguyên số thì không có vùng trộn nên giống hệt pixel thô.
// uBgr đảo kênh màu cho khung XRGB8888.
private const val FRAGMENT = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vUv;
uniform sampler2D uTex;
uniform float uBgr;
uniform vec2 uTexSize;   // kích thước khung hình trong texture (texel)
uniform vec2 uVr;        // dải dọc của khung được vẽ (0..1)
uniform vec2 uOut;       // kích thước vùng vẽ trên màn hình (pixel)
uniform float uMode;
uniform float uStrength; // 0..1, dùng cho LCD và CRT

void main() {
  // Vùng nguồn đang vẽ (texel) và vị trí hiện tại trong đó.
  vec2 src = vec2(uTexSize.x, (uVr.y - uVr.x) * uTexSize.y);
  vec2 p = clamp(vUv * src, vec2(0.0), src - 0.0001);
  vec2 base = floor(p);
  vec2 fr = p - base;
  vec2 sc = max(uOut / src, vec2(1.0));   // pixel màn hình trên mỗi texel

  vec2 pos = base + 0.5;
  if (uMode > 0.5) {
    vec2 rr = 0.5 - 0.5 / sc;
    vec2 cd = fr - 0.5;
    pos = base + (cd - clamp(cd, -rr, rr)) * sc + 0.5;
  }
  // Giữ điểm lấy mẫu trong vùng nguồn để không lẹm sang màn hình kia của NDS.
  pos = clamp(pos, vec2(0.5), src - 0.5);
  vec3 c = texture2D(uTex, vec2(pos.x / uTexSize.x, uVr.x + pos.y / uTexSize.y)).rgb;
  if (uBgr > 0.5) c = c.bgr;

  if (uMode > 1.5 && uMode < 2.5) {
    // LCD: vạch tối mảnh (khoảng 1 pixel) ở mép trên và mép trái của mỗi texel.
    float fade = smoothstep(1.5, 3.0, min(sc.x, sc.y));
    vec2 e = fr * sc;
    vec2 w = max(vec2(1.0), 0.12 * sc);
    vec2 g = clamp(w - e + 0.5, 0.0, 1.0);
    c *= 1.0 - 0.7 * uStrength * fade * max(g.x, g.y);
    c *= 1.0 + 0.1 * uStrength * fade;
  } else if (uMode > 2.5) {
    // CRT: đường quét ngang (hàng sáng rộng hơn khi điểm ảnh sáng) và chấm màu RGB theo cột pixel màn hình.
    float fadeY = smoothstep(1.5, 3.0, sc.y);
    float lum = dot(c, vec3(0.299, 0.587, 0.114));
    float wd = mix(0.2, 0.36, lum);
    float scan = smoothstep(wd, 0.5, abs(fr.y - 0.5));
    c *= 1.0 - 0.85 * uStrength * fadeY * scan;
    float xm = mod(floor(gl_FragCoord.x), 3.0);
    vec3 tri = vec3(xm < 0.5 ? 1.0 : 0.0, (xm > 0.5 && xm < 1.5) ? 1.0 : 0.0, xm > 1.5 ? 1.0 : 0.0);
    float a = 0.16 * uStrength;
    c *= (1.0 - a) + a * 3.0 * tri;
    c *= 1.0 + 0.12 * uStrength;
  }
  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""

class GameRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var tex = 0
    private var uTex = 0
    private var uBgr = 0
    private var uVr = 0
    private var uTexSize = 0
    private var uOut = 0
    private var uMode = 0
    private var uStrength = 0
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
        uVr = GLES20.glGetUniformLocation(program, "uVr")
        uTexSize = GLES20.glGetUniformLocation(program, "uTexSize")
        uOut = GLES20.glGetUniformLocation(program, "uOut")
        uMode = GLES20.glGetUniformLocation(program, "uMode")
        uStrength = GLES20.glGetUniformLocation(program, "uStrength")

        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)
        tex = t[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        // Luôn lọc tuyến tính: shader tự quyết định mức làm mịn (xem FRAGMENT).
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
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

        val quads = DsLayout.quads
        GLES20.glUseProgram(program)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glUniform1f(uBgr, if (is565) 0f else 1f)
        GLES20.glUniform2f(uTexSize, w.toFloat(), h.toFloat())
        GLES20.glUniform1f(uMode, RenderPrefs.filter.ordinal.toFloat())
        GLES20.glUniform1f(uStrength, RenderPrefs.level)
        quad.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quad)
        if (quads == null) {
            val vp = computeViewport(surfW, surfH, w, h, DisplayAspect.value, RenderPrefs.scaleMode)
            if (vp.width > 0 && vp.height > 0) {
                GLES20.glViewport(vp.x, surfH - vp.y - vp.height, vp.width, vp.height)
                GLES20.glUniform2f(uVr, 0f, 1f)
                GLES20.glUniform2f(uOut, vp.width.toFloat(), vp.height.toFloat())
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
        } else {
            for (q in quads) {
                val x = Math.round(q.x0 * surfW)
                val y = Math.round(q.y0 * surfH)
                val qw = Math.round((q.x1 - q.x0) * surfW)
                val qh = Math.round((q.y1 - q.y0) * surfH)
                if (qw <= 0 || qh <= 0) continue
                GLES20.glViewport(x, surfH - y - qh, qw, qh)
                GLES20.glUniform2f(uVr, q.v0, q.v1)
                GLES20.glUniform2f(uOut, qw.toFloat(), qh.toFloat())
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
        }
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
