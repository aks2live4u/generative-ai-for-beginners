package com.nexus.launcher.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An error returned by the Gemini API, or a network failure (code 0). */
class GeminiException(val code: Int, val status: String, message: String) : Exception(message)

/**
 * Minimal client for the Gemini API (Google AI Studio keys) over REST.
 *
 * Uses OkHttp and Android's built-in org.json, so it adds almost nothing to the APK.
 * Docs: https://ai.google.dev/api/generate-content
 */
class GeminiClient(
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    data class Result(
        /** Visible answer text (thought parts excluded). */
        val text: String,
        /** The model's content object, to append to the conversation history. */
        val content: JSONObject?,
        val finishReason: String?,
        val blockReason: String?,
    )

    /** One generateContent call. [contents] is the conversation, oldest first. */
    fun generate(
        apiKey: String,
        model: String,
        system: String,
        contents: List<JSONObject>,
        googleSearch: Boolean,
        maxOutputTokens: Int = 4096,
    ): Result {
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", JSONArray(contents))
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", maxOutputTokens)
                    .put("temperature", 0.7)
                    .apply {
                        // Gemini 2.5 Flash thinks by default; switch it off so spoken replies come back fast.
                        if (model.contains("2.5-flash")) put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                    },
            )
        if (googleSearch) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))

        val json = post("models/${modelId(model)}:generateContent", apiKey, body)
        val feedback = json.optJSONObject("promptFeedback")
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val text = buildString {
            val parts = content?.optJSONArray("parts") ?: JSONArray()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                append(part.optString("text", ""))
            }
        }
        return Result(
            text = text.trim(),
            content = content?.let { if (!it.has("role")) it.put("role", "model") else it },
            finishReason = candidate?.optString("finishReason")?.takeIf { it.isNotEmpty() },
            blockReason = feedback?.optString("blockReason")?.takeIf { it.isNotEmpty() },
        )
    }

    /** Model IDs (without the "models/" prefix) that support generateContent. */
    fun listModels(apiKey: String): List<String> {
        val out = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val path = "models?pageSize=200" + (pageToken?.let { "&pageToken=$it" } ?: "")
            val json = get(path, apiKey)
            val models = json.optJSONArray("models") ?: JSONArray()
            for (i in 0 until models.length()) {
                val m = models.optJSONObject(i) ?: continue
                val methods = m.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                val canGenerate = (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                if (canGenerate) out += m.optString("name").removePrefix("models/")
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return out
    }

    private fun post(path: String, apiKey: String, body: JSONObject): JSONObject =
        execute(
            Request.Builder()
                .url("$baseUrl/$path")
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody(JSON))
                .build(),
        )

    private fun get(path: String, apiKey: String): JSONObject =
        execute(Request.Builder().url("$baseUrl/$path").header("x-goog-api-key", apiKey).get().build())

    private fun execute(request: Request): JSONObject {
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) return if (raw.isBlank()) JSONObject() else JSONObject(raw)
                val err = runCatching { JSONObject(raw).optJSONObject("error") }.getOrNull()
                throw GeminiException(
                    code = response.code,
                    status = err?.optString("status").orEmpty(),
                    message = err?.optString("message")?.takeIf { it.isNotEmpty() } ?: "HTTP ${response.code}",
                )
            }
        } catch (e: IOException) {
            throw GeminiException(0, "NETWORK", e.message ?: "Network error")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun modelId(model: String) = model.trim().removePrefix("models/")

        /** Builds one conversation turn: role is "user" or "model". */
        fun turn(role: String, text: String): JSONObject =
            JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", text)))

        /** Picks a fast, general-purpose chat model from what the key can use. */
        fun pickFallbackModel(available: List<String>): String? {
            val preferred = listOf("gemini-2.5-flash", "gemini-flash-latest", "gemini-2.0-flash")
            preferred.firstOrNull { it in available }?.let { return it }
            val skip = listOf("lite", "image", "tts", "audio", "live", "embedding", "vision", "thinking", "exp", "learnlm", "gemma")
            return available
                .filter { it.startsWith("gemini") && "flash" in it && skip.none { s -> s in it } }
                .maxOrNull()
                ?: available.firstOrNull { it.startsWith("gemini") }
        }
    }
}
