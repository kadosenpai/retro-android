package com.retro.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class RomInfo(
    val id: String,
    val name: String,
    val size: Long,
    val addedAt: Long,
    val lastPlayedAt: Long,
    /** Tên file gốc (không đuôi), dùng để tra ảnh bìa dù bạn đổi tên hiển thị. */
    val fileBase: String = "",
    /** Thời điểm đặt ảnh bìa (0 = chưa có). */
    val coverAt: Long = 0,
    /** Lần gần nhất đã thử tải ảnh bìa tự động (để không thử lại liên tục). */
    val coverTriedAt: Long = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("size", size)
        .put("addedAt", addedAt)
        .put("lastPlayedAt", lastPlayedAt)
        .put("fileBase", fileBase)
        .put("coverAt", coverAt)
        .put("coverTriedAt", coverTriedAt)

    companion object {
        fun fromJson(o: JSONObject) = RomInfo(
            id = o.getString("id"),
            name = o.optString("name", "Game"),
            size = o.optLong("size", 0),
            addedAt = o.optLong("addedAt", 0),
            lastPlayedAt = o.optLong("lastPlayedAt", 0),
            fileBase = o.optString("fileBase", ""),
            coverAt = o.optLong("coverAt", 0),
            coverTriedAt = o.optLong("coverTriedAt", 0),
        )
    }
}

sealed interface ImportResult {
    data class Added(val rom: RomInfo) : ImportResult
    data class Duplicate(val rom: RomInfo) : ImportResult
    data class Rejected(val reason: String) : ImportResult
}

/** Lưu ROM, save và chỉ mục thư viện trong bộ nhớ riêng của ứng dụng. */
class RomStore(private val ctx: Context) {
    private val lock = Any()
    private val romDir = File(ctx.filesDir, "roms").apply { mkdirs() }
    private val saveDir = File(ctx.filesDir, "saves").apply { mkdirs() }
    private val coverDir = File(ctx.filesDir, "covers").apply { mkdirs() }
    private val stateDir = File(ctx.filesDir, "states").apply { mkdirs() }
    private val indexFile = File(ctx.filesDir, "library.json")

    fun romFile(id: String) = File(romDir, "$id.rom")
    fun savFile(id: String) = File(saveDir, "$id.sav")

    // ---------- Ảnh bìa ----------
    private fun coverFile(id: String) = File(coverDir, "$id.jpg")

    fun readCover(id: String): ByteArray? = coverFile(id).takeIf { it.exists() }?.readBytes()

    fun setCover(id: String, jpeg: ByteArray) = synchronized(lock) {
        writeAtomic(coverFile(id), jpeg)
        val now = System.currentTimeMillis()
        writeIndex(readIndex().map { if (it.id == id) it.copy(coverAt = now, coverTriedAt = now) else it })
    }

    fun markCoverTried(id: String) = synchronized(lock) {
        val now = System.currentTimeMillis()
        writeIndex(readIndex().map { if (it.id == id) it.copy(coverTriedAt = now) else it })
    }

    // ---------- Trạng thái (save state) ----------
    private fun stateFile(id: String, slot: Int) = File(stateDir, "$id.$slot.state")
    private fun thumbFile(id: String, slot: Int) = File(stateDir, "$id.$slot.png")

    fun writeState(id: String, slot: Int, data: ByteArray, thumbPng: ByteArray?) {
        writeAtomic(stateFile(id, slot), data)
        if (thumbPng != null) writeAtomic(thumbFile(id, slot), thumbPng) else thumbFile(id, slot).delete()
    }

    fun readState(id: String, slot: Int): ByteArray? = stateFile(id, slot).takeIf { it.exists() }?.readBytes()

    fun readThumb(id: String, slot: Int): ByteArray? = thumbFile(id, slot).takeIf { it.exists() }?.readBytes()

    /** Thời điểm lưu (ms); 0 nếu ô trống. */
    fun stateTime(id: String, slot: Int): Long = stateFile(id, slot).takeIf { it.exists() }?.lastModified() ?: 0L

    fun deleteState(id: String, slot: Int) {
        stateFile(id, slot).delete()
        thumbFile(id, slot).delete()
    }

    fun list(): List<RomInfo> = synchronized(lock) { readIndex() }

    fun get(id: String): RomInfo? = list().firstOrNull { it.id == id }

    private fun readIndex(): List<RomInfo> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).map { RomInfo.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeIndex(list: List<RomInfo>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        writeAtomic(indexFile, arr.toString().toByteArray())
    }

    private fun writeAtomic(file: File, data: ByteArray) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(data)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    /** Sao chép ROM từ Uri người dùng chọn vào bộ nhớ riêng; id = SHA-256 của nội dung. */
    fun import(uri: Uri): ImportResult {
        val displayName = queryName(uri) ?: "Game"
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (ext !in ALLOWED_EXT) {
            return ImportResult.Rejected("\"$displayName\": chỉ hỗ trợ file .gba (không nén .zip)")
        }
        val tmp = File(romDir, "import-${System.nanoTime()}.tmp")
        try {
            val md = MessageDigest.getInstance("SHA-256")
            var total = 0L
            ctx.contentResolver.openInputStream(uri).use { input ->
                if (input == null) return ImportResult.Rejected("Không đọc được \"$displayName\"")
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        md.update(buf, 0, n)
                        out.write(buf, 0, n)
                        total += n
                    }
                }
            }
            if (total < 0x200) return ImportResult.Rejected("\"$displayName\" quá nhỏ, không phải ROM GBA")
            val id = md.digest().joinToString("") { "%02x".format(it) }.take(32)
            synchronized(lock) {
                val list = readIndex()
                list.firstOrNull { it.id == id }?.let { return ImportResult.Duplicate(it) }
                if (!tmp.renameTo(romFile(id))) {
                    tmp.copyTo(romFile(id), overwrite = true)
                }
                val now = System.currentTimeMillis()
                val base = displayName.substringBeforeLast('.')
                val info = RomInfo(id, base, total, now, 0L, fileBase = base)
                writeIndex(list + info)
                return ImportResult.Added(info)
            }
        } catch (e: Exception) {
            return ImportResult.Rejected("Lỗi khi thêm \"$displayName\": ${e.message}")
        } finally {
            tmp.delete()
        }
    }

    fun rename(id: String, name: String) = synchronized(lock) {
        writeIndex(readIndex().map { if (it.id == id) it.copy(name = name) else it })
    }

    fun touchPlayed(id: String) = synchronized(lock) {
        val now = System.currentTimeMillis()
        writeIndex(readIndex().map { if (it.id == id) it.copy(lastPlayedAt = now) else it })
    }

    fun delete(id: String) = synchronized(lock) {
        writeIndex(readIndex().filter { it.id != id })
        romFile(id).delete()
        savFile(id).delete()
        coverFile(id).delete()
        for (slot in 0..SLOT_COUNT) deleteState(id, slot)
    }

    fun readSram(id: String): ByteArray? = savFile(id).takeIf { it.exists() }?.readBytes()

    fun writeSram(id: String, data: ByteArray) = writeAtomic(savFile(id), data)

    fun hasSram(id: String): Boolean = savFile(id).let { it.exists() && it.length() > 0 }

    /** Ghi file .sav của game ra Uri người dùng chọn. Trả về thông báo lỗi, hoặc null nếu thành công. */
    fun exportSram(id: String, uri: Uri): String? {
        val src = savFile(id)
        if (!src.exists() || src.length() == 0L) return "Game này chưa có save để xuất"
        return try {
            ctx.contentResolver.openOutputStream(uri, "wt").use { out ->
                if (out == null) return "Không ghi được file"
                out.write(src.readBytes())
            }
            null
        } catch (e: Exception) {
            "Lỗi khi xuất save: ${e.message}"
        }
    }

    /**
     * Nhập file .sav từ Uri thành save của game. Kích thước hợp lệ của GBA: 512 B đến 128 KB
     * (cho phép dư tối đa 64 byte vì vài trình giả lập ghi thêm dữ liệu đồng hồ).
     * Xoá luôn trạng thái tự động của game, nếu không lần mở sau sẽ nạp lại trạng thái cũ đè lên save vừa nhập.
     */
    fun importSram(id: String, uri: Uri): String? {
        return try {
            val data = ctx.contentResolver.openInputStream(uri).use { it?.readBytes() }
                ?: return "Không đọc được file"
            if (data.size < 512 || data.size > 131072 + 64) {
                return "File không phải save GBA hợp lệ (${data.size} byte)"
            }
            synchronized(lock) {
                writeAtomic(savFile(id), data)
                deleteState(id, AUTO_SLOT)
            }
            null
        } catch (e: Exception) {
            "Lỗi khi nhập save: ${e.message}"
        }
    }

    private fun queryName(uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: uri.lastPathSegment

    companion object {
        val ALLOWED_EXT = setOf("gba", "agb", "bin")

        /** Ô 0 = tự động; ô 1..SLOT_COUNT = người chơi tự lưu. */
        const val AUTO_SLOT = 0
        const val SLOT_COUNT = 4
    }
}
