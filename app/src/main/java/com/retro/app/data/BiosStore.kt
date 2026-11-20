package com.retro.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.CRC32

/** Các file BIOS/firmware mà lõi có thể dùng (đều tuỳ chọn; thiếu thì lõi dùng bản giả lập). */
enum class BiosKind(val title: String, val hint: String) {
    PSX("BIOS PlayStation", "File 512 KB (SCPH-xxxx). Có thể thêm nhiều vùng (US, EU, JP); không có thì dùng BIOS giả lập, một số game không chạy"),
    NDS7("BIOS ARM7 (bios7.bin)", "File 16 KB"),
    NDS9("BIOS ARM9 (bios9.bin)", "File 4 KB"),
    NDS_FW("Firmware (firmware.bin)", "File 128, 256 hoặc 512 KB. Cần đủ cả ba file NDS thì mới dùng BIOS thật, thiếu thì dùng BIOS giả lập"),
    DSI7("DSi BIOS ARM7 (dsi_bios7.bin)", "File 64 KB, lấy từ máy DSi của bạn"),
    DSI9("DSi BIOS ARM9 (dsi_bios9.bin)", "File 64 KB, lấy từ máy DSi của bạn"),
    DSI_FW("DSi Firmware (dsi_firmware.bin)", "File 128 KB trở lên, lấy từ máy DSi của bạn"),
    DSI_NAND("DSi NAND (dsi_nand.bin)", "File bộ nhớ trong của DSi, khoảng 240 MB. Chép mất vài giây"),
    GBA("BIOS Game Boy Advance (gba_bios.bin)", "File 16 KB. Có thì game GBA hiện logo khởi động và chạy đúng hơn"),
    ;

    /** Hệ máy dùng BIOS này (id của [GameSystem]). */
    val systemId: String
        get() = when (this) {
            PSX -> GameSystem.PSX.id
            NDS7, NDS9, NDS_FW, DSI7, DSI9, DSI_FW, DSI_NAND -> GameSystem.NDS.id
            GBA -> GameSystem.GBA.id
        }
}

/** Lưu BIOS vào thư mục system của lõi (`filesDir/system`); lõi tự tìm file ở đó khi mở game. */
class BiosStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "system").apply { mkdirs() }

    private fun fileOf(kind: BiosKind): String? = when (kind) {
        BiosKind.NDS7 -> "bios7.bin"
        BiosKind.NDS9 -> "bios9.bin"
        BiosKind.NDS_FW -> "firmware.bin"
        BiosKind.DSI7 -> "dsi_bios7.bin"
        BiosKind.DSI9 -> "dsi_bios9.bin"
        BiosKind.DSI_FW -> "dsi_firmware.bin"
        BiosKind.DSI_NAND -> "dsi_nand.bin"
        BiosKind.GBA -> "gba_bios.bin"
        BiosKind.PSX -> null
    }

    /** Các vùng BIOS PlayStation đã cài (US, EU, JP). */
    fun psxRegions(): List<String> =
        listOf("US", "EU", "JP").filter { File(dir, "psx_bios_$it.bin").length() > 0 }

    /** Trạng thái hiển thị: null = chưa cài. */
    fun status(kind: BiosKind): String? = when (kind) {
        BiosKind.PSX -> psxRegions().takeIf { it.isNotEmpty() }?.joinToString(", ")
        else -> if (File(dir, fileOf(kind)!!).length() > 0) "Đã cài" else null
    }

    /** Chế độ DSi cần BIOS ARM7/ARM9 của DS thường cộng với bốn file DSi (lõi đòi như vậy). */
    fun dsiReady(): Boolean =
        listOf(BiosKind.NDS7, BiosKind.NDS9, BiosKind.DSI7, BiosKind.DSI9, BiosKind.DSI_FW, BiosKind.DSI_NAND)
            .all { status(it) != null }

    /** NAND rất lớn nên chép thẳng từ Uri sang file tạm rồi đổi tên, không đọc vào bộ nhớ. */
    private fun installNand(ctx: Context, uri: Uri): String? {
        val tmp = File(dir, "dsi_nand.bin.tmp")
        return try {
            val copied = ctx.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> input.copyTo(out, 256 * 1024) }
            } ?: return "Không đọc được file"
            // Lõi chỉ nhận NAND đúng 240 MB hoặc 245,5 MB, có hoặc không có phần đuôi 64 byte của no$gba.
            val sizes = listOf(0xF000000L, 0xF580000L).flatMap { listOf(it, it + 0x40L) }
            if (copied !in sizes) {
                tmp.delete()
                return "File không đúng kích thước NAND DSi (cần 240 MB hoặc 245,5 MB)"
            }
            val dest = File(dir, "dsi_nand.bin")
            dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return "Không lưu được file NAND"
            }
            null
        } catch (e: Exception) {
            tmp.delete()
            "Không chép được file NAND (${e.message ?: "lỗi"}). Kiểm tra dung lượng trống"
        }
    }

    /** Sao chép BIOS từ Uri vào thư mục system sau khi kiểm tra kích thước/nội dung. Trả về thông báo lỗi, hoặc null nếu xong. */
    fun install(ctx: Context, kind: BiosKind, uri: Uri): String? {
        if (kind == BiosKind.DSI_NAND) return installNand(ctx, uri)
        val data = try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val max = 8 * 1024 * 1024
                val buf = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                while (buf.size() <= max) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buf.write(chunk, 0, n)
                }
                buf.toByteArray().takeIf { it.size <= max }
            }
        } catch (e: Exception) {
            null
        } ?: return "Không đọc được file"
        return when (kind) {
            BiosKind.PSX -> installPsx(data)
            BiosKind.NDS7 -> installFixed(kind, data, setOf(16 * 1024))
            BiosKind.NDS9 -> installFixed(kind, data, setOf(4 * 1024))
            BiosKind.NDS_FW -> installFixed(kind, data, setOf(128 * 1024, 256 * 1024, 512 * 1024))
            BiosKind.DSI7, BiosKind.DSI9 -> installFixed(kind, data, setOf(64 * 1024))
            BiosKind.DSI_FW -> installFixed(kind, data, setOf(128 * 1024, 256 * 1024, 512 * 1024))
            BiosKind.DSI_NAND -> null
            BiosKind.GBA -> installFixed(kind, data, setOf(16 * 1024))
        }
    }

    private fun installFixed(kind: BiosKind, data: ByteArray, sizes: Set<Int>): String? {
        if (data.size !in sizes) {
            return "File không đúng kích thước của ${kind.title} (cần ${sizes.sorted().joinToString(" hoặc ") { "${it / 1024} KB" }})"
        }
        File(dir, fileOf(kind)!!).writeBytes(data)
        return null
    }

    private fun installPsx(data: ByteArray): String? {
        if (data.size != 512 * 1024) return "BIOS PlayStation phải là file 512 KB"
        // Cùng cách kiểm tra như lõi: mã máy mở đầu hoặc chuỗi "PS compatible".
        val head = byteArrayOf(0x00, 0x08, 0x3c, 0x3f)
        val okHead = (0 until 4).all { data[1 + it] == head[it] }
        val okText = String(data, 0x12c, 13, Charsets.ISO_8859_1) == "PS compatible"
        if (!okHead && !okText) return "Đây không phải file BIOS PlayStation"
        val crc = CRC32().apply { update(data) }.value
        fun at(off: Int, s: String) = String(data, off, 2, Charsets.ISO_8859_1) == s
        val region = when {
            at(0x7ff51, " E") -> "EU"
            at(0x7ff51, " J") || crc == 0x18D0F7D8L || crc == 0x3B601FC8L || crc == 0x3539DEF6L -> "JP"
            else -> "US"
        }
        File(dir, "psx_bios_$region.bin").writeBytes(data)
        return null
    }
}
