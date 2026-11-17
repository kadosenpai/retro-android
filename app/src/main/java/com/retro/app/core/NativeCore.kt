package com.retro.app.core

import java.nio.ByteBuffer

/** Cầu nối tới lõi C++ (libretro_host). Mọi hàm chỉ nên gọi từ luồng giả lập, trừ [nativeCopyFrame]. */
object NativeCore {
    init {
        System.loadLibrary("retro_host")
    }

    /** Trả về null nếu thành công, ngược lại là thông báo lỗi. */
    external fun nativeInit(corePath: String, systemDir: String, saveDir: String): String?
    external fun nativeLoadGame(romPath: String): String?
    external fun nativeUnloadGame()
    external fun nativeShutdown()
    external fun nativeRunFrame()
    external fun nativeReset()
    external fun nativeSetInput(mask: Int)

    /** [fps, sampleRate, width, height, pixelFormat] */
    external fun nativeAvInfo(): DoubleArray

    /** Chép khung hình vào [buffer] (direct). info = [w, h, fmt]. Trả về số thứ tự khung, -1 nếu lỗi. */
    external fun nativeCopyFrame(buffer: ByteBuffer, info: IntArray): Long

    /** Điền mẫu stereo xen kẽ vào [out], trả về số cặp L/R. */
    external fun nativeDrainAudio(out: ShortArray): Int

    external fun nativeGetSram(): ByteArray?
    external fun nativeSetSram(data: ByteArray)
    external fun nativeSerialize(): ByteArray?
    external fun nativeUnserialize(data: ByteArray): Boolean
    /** Lấy chữ từ ROM Pokémon gen 3: mỗi dòng "vị_trí\tđộ_dài\tchữ". */
    external fun nativeExtractGen3Text(path: String): String?
    external fun nativeSetOption(key: String, value: String)
    external fun nativeCheatReset()
    /** false nếu lõi không đọc được mã (chỉ báo được với lõi đã vá). */
    external fun nativeCheatSet(index: Int, enabled: Boolean, code: String): Boolean
    external fun nativeCheatsPatched(): Boolean
}

/** Số bit của tay cầm libretro (RetroPad). */
object Pad {
    const val B = 1 shl 0
    const val SELECT = 1 shl 2
    const val START = 1 shl 3
    const val UP = 1 shl 4
    const val DOWN = 1 shl 5
    const val LEFT = 1 shl 6
    const val RIGHT = 1 shl 7
    const val A = 1 shl 8
    const val L = 1 shl 10
    const val R = 1 shl 11
    const val DPAD = UP or DOWN or LEFT or RIGHT
}
