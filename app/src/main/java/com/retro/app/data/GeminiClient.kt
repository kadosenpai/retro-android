package com.retro.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Lỗi từ Gemini. fatal = thử lại cũng vô ích (sai key, sai model, hết hạn mức…). */
class GeminiException(message: String, val fatal: Boolean) : Exception(message)

/** Gọi Gemini API để dịch một lô đoạn chữ game sang tiếng Việt. */
class GeminiClient(private val key: String, private val model: String) {

    /** Dịch [items]; trả về danh sách cùng độ dài, cùng thứ tự. [onWait] báo khi phải chờ hạn mức. */
    suspend fun translate(items: List<String>, onWait: (seconds: Int) -> Unit): List<String> {
        var attempt = 0
        while (true) {
            try {
                return withContext(Dispatchers.IO) { request(items) }
            } catch (e: GeminiException) {
                if (e.fatal) throw e
                attempt++
                if (attempt > MAX_ATTEMPTS) throw GeminiException(e.message ?: "Lỗi Gemini", true)
                val wait = retryDelaySeconds(e.message ?: "") ?: (10 * attempt)
                onWait(wait)
                delay(wait * 1000L)
            }
        }
    }

    private fun retryDelaySeconds(msg: String): Int? =
        Regex("""retryDelay"?\s*:?\s*"?(\d+)""").find(msg)?.groupValues?.get(1)?.toIntOrNull()?.plus(1)

    private fun request(items: List<String>): List<String> {
        val prompt = buildPrompt(items)
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.3)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING"))),
            )
        val conn = try {
            (URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", key)
            }
        } catch (e: Exception) {
            throw GeminiException("Không kết nối được: ${e.message}", false)
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw httpError(code, text)
            return parse(text, items.size)
        } catch (e: GeminiException) {
            throw e
        } catch (e: Exception) {
            // Mất mạng, hết thời gian chờ… thử lại được.
            throw GeminiException("Lỗi mạng: ${e.message}", false)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpError(code: Int, text: String): GeminiException {
        val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrNull() ?: text.take(200)
        return when {
            code == 429 -> GeminiException("Hết hạn mức tạm thời (429). $text", false)
            code in 500..599 -> GeminiException("Máy chủ Gemini lỗi ($code)", false)
            code == 404 -> GeminiException("Không tìm thấy model \"$model\". Vào Cài đặt → Dịch game để đổi tên model.", true)
            code == 400 && msg.contains("API key", ignoreCase = true) -> GeminiException("API key không hợp lệ. Kiểm tra lại key trong Cài đặt → Dịch game.", true)
            code == 401 || code == 403 -> GeminiException("Gemini từ chối key này ($code): $msg", true)
            else -> GeminiException("Lỗi Gemini ($code): $msg", true)
        }
    }

    private fun parse(text: String, expected: Int): List<String> {
        val root = JSONObject(text)
        val cand = root.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw GeminiException("Gemini không trả kết quả (có thể bị chặn nội dung)", false)
        val parts = cand.optJSONObject("content")?.optJSONArray("parts")
            ?: throw GeminiException("Gemini trả về rỗng (${cand.optString("finishReason")})", false)
        val sb = StringBuilder()
        for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
        val arr = try {
            JSONArray(sb.toString())
        } catch (e: Exception) {
            throw GeminiException("Gemini trả về dữ liệu không đọc được", false)
        }
        if (arr.length() != expected) throw GeminiException("Gemini trả sai số lượng (${arr.length()}/$expected)", false)
        return List(expected) { arr.optString(it, "") }
    }

    private fun buildPrompt(items: List<String>): String {
        val input = JSONArray()
        items.forEach { input.put(it) }
        return """
            You are translating text from the Game Boy Advance game Pokémon Emerald (English) into Vietnamese for players.
            Input is a JSON array of strings. Output a JSON array of strings with exactly the same number of items, in the same order; item i is the Vietnamese translation of input item i.
            Rules:
            - Natural, friendly Vietnamese that fits a Pokémon game. Keep short menu labels short.
            - Keep every tag in curly braces exactly as written, for example {P01} or {C08:3C}. They are variables or control codes (e.g. {P01} is the player's name). Place them where they fit naturally.
            - Keep every \p marker. It separates dialogue pages, so the output must contain the same number of \p markers as the input.
            - Do not translate names: Pokémon species, moves, items, abilities, places, trainers and characters stay in English. Keep the word POKéMON as it is.
            - Keep numbers and symbols such as ♂ ♀ … ! ? unchanged.
            - If an item is only a name or has nothing to translate, return it unchanged.
            - Output only the JSON array.

            ${input}
        """.trimIndent()
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
    }
}
