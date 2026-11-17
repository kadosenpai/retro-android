package com.retro.app.data

import com.retro.app.core.NativeCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Dịch toàn bộ chữ tiếng Anh trong ROM Pokémon Emerald sang tiếng Việt bằng Gemini. */
class GameTranslator(private val store: RomStore) {

    class Result(val total: Int, val translated: Int, val failed: Int, val error: String?)

    /** Chỉ Pokémon Emerald bản Mỹ (mã game BPEE) được hỗ trợ. */
    fun supports(rom: RomInfo): Boolean = rom.system == "gba" && store.gbaGameCode(rom.id) == "BPEE"

    /**
     * Chạy dịch; có thể dừng rồi chạy lại, các đoạn đã dịch được giữ và bỏ qua.
     * [onProgress] nhận (đã xử lý, tổng, ghi chú hiển thị hoặc null).
     */
    suspend fun run(
        rom: RomInfo,
        key: String,
        model: String,
        onProgress: (done: Int, total: Int, note: String?) -> Unit,
    ): Result {
        val sources = withContext(Dispatchers.IO) { extractSources(rom) }
            ?: return Result(0, 0, 0, "Không đọc được file ROM")
        val existing = withContext(Dispatchers.IO) { store.readTranslations(rom.id) }
        val pending = sources.filter { it !in existing }
        val total = sources.size
        var done = total - pending.size
        var translated = 0
        var failed = 0
        onProgress(done, total, null)
        if (pending.isEmpty()) return Result(total, 0, 0, null)

        val client = GeminiClient(key, model)
        var error: String? = null
        for (batch in batches(pending)) {
            coroutineContext.ensureActive()
            val ok = ArrayList<Pair<String, String>>()
            var left = batch
            try {
                // Lượt 1 cho cả lô; lượt 2 thử lại riêng các đoạn bị hỏng mã điều khiển.
                for (round in 0 until 2) {
                    if (left.isEmpty()) break
                    val out = client.translate(left) { sec ->
                        onProgress(done, total, "Đợi hạn mức ${sec}s…")
                    }
                    val bad = ArrayList<String>()
                    left.forEachIndexed { i, src ->
                        val t = out[i].trim()
                        if (TranslateText.isValid(src, t)) ok.add(src to t) else bad.add(src)
                    }
                    left = bad
                }
            } catch (e: GeminiException) {
                if (ok.isNotEmpty()) withContext(Dispatchers.IO) { store.appendTranslations(rom.id, ok) }
                translated += ok.size
                error = e.message
                break
            }
            withContext(Dispatchers.IO) { store.appendTranslations(rom.id, ok) }
            translated += ok.size
            failed += left.size
            done += batch.size
            onProgress(done, total, null)
        }
        return Result(total, translated, failed, error)
    }

    /** Danh sách chữ gốc đã chuẩn hoá, bỏ trùng, giữ thứ tự xuất hiện trong ROM. */
    private fun extractSources(rom: RomInfo): List<String>? {
        val raw = NativeCore.nativeExtractGen3Text(store.romFile(rom.id).absolutePath) ?: return null
        val seen = LinkedHashSet<String>()
        for (line in raw.lineSequence()) {
            if (line.isEmpty()) continue
            val text = line.split('\t', limit = 3).getOrNull(2) ?: continue
            val n = TranslateText.normalize(text)
            if (n.isNotEmpty() && TranslateText.worthTranslating(n)) seen.add(n)
        }
        return seen.toList()
    }

    /** Chia thành các lô nhỏ (tối đa ~6000 ký tự và 80 đoạn mỗi lô). */
    private fun batches(items: List<String>): List<List<String>> {
        val out = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        var chars = 0
        for (s in items) {
            if (cur.isNotEmpty() && (chars + s.length > MAX_CHARS || cur.size >= MAX_ITEMS)) {
                out.add(cur)
                cur = ArrayList()
                chars = 0
            }
            cur.add(s)
            chars += s.length
        }
        if (cur.isNotEmpty()) out.add(cur)
        return out
    }

    companion object {
        private const val MAX_CHARS = 6000
        private const val MAX_ITEMS = 80
    }
}
