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
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("size", size)
        .put("addedAt", addedAt)
        .put("lastPlayedAt", lastPlayedAt)

    companion object {
        fun fromJson(o: JSONObject) = RomInfo(
            id = o.getString("id"),
            name = o.optString("name", "Game"),
            size = o.optLong("size", 0),
            addedAt = o.optLong("addedAt", 0),
            lastPlayedAt = o.optLong("lastPlayedAt", 0),
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
    private val indexFile = File(ctx.filesDir, "library.json")

    fun romFile(id: String) = File(romDir, "$id.rom")
    fun savFile(id: String) = File(saveDir, "$id.sav")

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
                val info = RomInfo(id, displayName.substringBeforeLast('.'), total, now, 0L)
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
    }

    fun readSram(id: String): ByteArray? = savFile(id).takeIf { it.exists() }?.readBytes()

    fun writeSram(id: String, data: ByteArray) = writeAtomic(savFile(id), data)

    private fun queryName(uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: uri.lastPathSegment

    companion object {
        val ALLOWED_EXT = setOf("gba", "agb", "bin")
    }
}
