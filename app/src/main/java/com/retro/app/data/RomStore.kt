package com.retro.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
    /** Id hệ máy (xem [GameSystem]); các game cũ không có trường này nên mặc định là GBA. */
    val system: String = "gba",
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
        .put("system", system)

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
            system = o.optString("system", "gba"),
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
    private val cheatDir = File(ctx.filesDir, "cheats").apply { mkdirs() }
    private val stateDir = File(ctx.filesDir, "states").apply { mkdirs() }
    private val indexFile = File(ctx.filesDir, "library.json")

    fun romFile(id: String) = File(romDir, "$id.rom")
    fun savFile(id: String) = File(saveDir, "$id.sav")

    // ---------- Cheat ----------
    private fun cheatFile(id: String) = File(cheatDir, "$id.json")

    fun readCheats(id: String): List<CheatItem> {
        val f = cheatFile(id)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val code = o.optString("code", "")
                if (code.isEmpty()) return@mapNotNull null
                CheatItem(o.optString("id", "c$i"), o.optString("name", ""), code, o.optBoolean("enabled", false))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun writeCheats(id: String, list: List<CheatItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("code", it.code).put("enabled", it.enabled))
        }
        writeAtomic(cheatFile(id), arr.toString().toByteArray())
    }

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

    /** Đoán hệ của file .bin theo nội dung: Mega Drive có chữ "SEGA" ở 0x100, đĩa PlayStation có "PLAYSTATION" ở sector 16; còn lại coi là GBA. */
    private fun sniffBin(f: File): GameSystem {
        val head = ByteArray(40000)
        var n = 0
        try {
            f.inputStream().use { input ->
                while (n < head.size) {
                    val r = input.read(head, n, head.size - n)
                    if (r < 0) break
                    n += r
                }
            }
        } catch (_: Exception) {
            return GameSystem.GBA
        }
        fun at(off: Int, text: String) =
            off + text.length <= n && String(head, off, text.length, Charsets.ISO_8859_1) == text
        if (at(0x100, "SEGA")) return GameSystem.MD
        if (at(16 * 2048 + 8, "PLAYSTATION")) return GameSystem.PSX // ảnh 2048 byte/sector
        // Ảnh thô 2352 byte/sector (bắt đầu bằng mẫu đồng bộ 00 FF*10 00): sector 16 nằm quanh byte 37632.
        val raw = n > 12 && head[0] == 0.toByte() && head[11] == 0.toByte() && (1..10).all { head[it] == 0xFF.toByte() }
        if (raw && n >= 37700 && String(head, 37600, 100, Charsets.ISO_8859_1).contains("PLAYSTATION")) return GameSystem.PSX
        return GameSystem.GBA
    }

    /** Sao chép ROM từ Uri người dùng chọn vào bộ nhớ riêng; id = SHA-256 của nội dung. */
    fun import(uri: Uri): ImportResult {
        val displayName = queryName(uri) ?: "Game"
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (ext == "cue") {
            return ImportResult.Rejected("\"$displayName\": file .cue chỉ là bảng mục lục của đĩa. Hãy chọn file .bin (đĩa một track) hoặc đổi cả đĩa sang .chd")
        }
        // .bin dùng chung cho GBA, Mega Drive và PlayStation: phải xem nội dung mới biết hệ nào (làm sau khi sao chép).
        var sys = GameSystem.fromExt(ext)
            ?: return ImportResult.Rejected("\"$displayName\": chỉ hỗ trợ file .gba, .gb, .gbc, .nes, .sfc, .smc, .nds, .md, .gen, .bin, .chd, .pbp, .iso (không nén .zip)")
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
            if (ext == "bin") sys = sniffBin(tmp)
            if (total < sys.minRomSize) return ImportResult.Rejected("\"$displayName\" quá nhỏ, không phải ROM ${sys.label}")
            val id = md.digest().joinToString("") { "%02x".format(it) }.take(32)
            synchronized(lock) {
                val list = readIndex()
                list.firstOrNull { it.id == id }?.let { return ImportResult.Duplicate(it) }
                if (!tmp.renameTo(romFile(id))) {
                    tmp.copyTo(romFile(id), overwrite = true)
                }
                val now = System.currentTimeMillis()
                val base = displayName.substringBeforeLast('.')
                val info = RomInfo(id, base, total, now, 0L, fileBase = base, system = sys.id)
                writeIndex(list + info)
                return ImportResult.Added(info)
            }
        } catch (e: Exception) {
            return ImportResult.Rejected("Lỗi khi thêm \"$displayName\": ${e.message}")
        } finally {
            tmp.delete()
        }
    }

    /**
     * Quét thư mục game (cây tài liệu do người dùng chọn), thêm mọi ROM mới tìm thấy (có quét cả thư mục con).
     * Mỗi file đã xử lý được nhớ theo (id tài liệu, kích thước, ngày sửa) nên lần quét sau không đọc lại file cũ,
     * trừ khi game đó đã bị xoá khỏi thư viện.
     */
    fun scanTree(tree: Uri): List<RomInfo> {
        val prefs = ctx.getSharedPreferences("retro-scan", Context.MODE_PRIVATE)
        // "khoá tệp\tid game"
        val known = HashMap<String, String>()
        for (e in prefs.getStringSet("known", emptySet()).orEmpty()) {
            val i = e.lastIndexOf('\t')
            if (i > 0) known[e.substring(0, i)] = e.substring(i + 1)
        }
        val liveIds = synchronized(lock) { readIndex().map { it.id }.toHashSet() }
        val added = ArrayList<RomInfo>()
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val stack = ArrayDeque<String>()
        stack.add(DocumentsContract.getTreeDocumentId(tree))
        while (stack.isNotEmpty()) {
            val dirId = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
            val files = ArrayList<Array<String>>()
            try {
                ctx.contentResolver.query(children, cols, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val docId = c.getString(0) ?: continue
                        val name = c.getString(1) ?: continue
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            stack.add(docId)
                        } else {
                            files.add(arrayOf(docId, name, c.getLong(3).toString(), c.getLong(4).toString()))
                        }
                    }
                }
            } catch (_: Exception) {
                continue
            }
            for (f in files) {
                val ext = f[1].substringAfterLast('.', "").lowercase()
                if (GameSystem.fromExt(ext) == null) continue
                val key = "${f[0]}|${f[2]}|${f[3]}"
                val old = known[key]
                if (old != null && old in liveIds) continue
                when (val r = import(DocumentsContract.buildDocumentUriUsingTree(tree, f[0]))) {
                    is ImportResult.Added -> {
                        known[key] = r.rom.id
                        added.add(r.rom)
                    }
                    is ImportResult.Duplicate -> known[key] = r.rom.id
                    is ImportResult.Rejected -> {}
                }
            }
        }
        prefs.edit().putStringSet("known", known.map { "${it.key}\t${it.value}" }.toSet()).apply()
        return added
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
        cheatFile(id).delete()
        for (slot in 0..SLOT_COUNT) deleteState(id, slot)
    }

    fun readSram(id: String): ByteArray? = savFile(id).takeIf { it.exists() }?.readBytes()

    fun writeSram(id: String, data: ByteArray) = writeAtomic(savFile(id), data)

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
     * Nhập file .sav từ Uri thành save của game. Kích thước hợp lệ (GBA, GB, GBC): 512 B đến 128 KB
     * (cho phép dư tối đa 64 byte vì vài trình giả lập ghi thêm dữ liệu đồng hồ).
     * Xoá luôn trạng thái tự động của game, nếu không lần mở sau sẽ nạp lại trạng thái cũ đè lên save vừa nhập.
     */
    fun importSram(id: String, uri: Uri): String? {
        return try {
            val data = ctx.contentResolver.openInputStream(uri).use { it?.readBytes() }
                ?: return "Không đọc được file"
            if (data.size < 512 || data.size > 131072 + 64) {
                return "File không phải save hợp lệ (${data.size} byte)"
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
        /** Ô 0 = tự động; ô 1..SLOT_COUNT = người chơi tự lưu. */
        const val AUTO_SLOT = 0
        const val SLOT_COUNT = 4
    }
}
