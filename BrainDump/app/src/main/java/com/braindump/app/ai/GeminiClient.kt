package com.braindump.app.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the Gemini "generateContent" API, used only for voice
 * note transcription since the Claude API does not accept raw audio.
 */
object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun transcribeAudio(apiKey: String, model: String, base64Audio: String, mimeType: String): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val body = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray()
                                    .put(
                                        JSONObject().apply {
                                            put(
                                                "text",
                                                "Transcribe this audio recording. Return only the spoken words, with no extra commentary."
                                            )
                                        }
                                    )
                                    .put(
                                        JSONObject().apply {
                                            put(
                                                "inline_data",
                                                JSONObject().apply {
                                                    put("mime_type", mimeType)
                                                    put("data", base64Audio)
                                                }
                                            )
                                        }
                                    )
                            )
                        }
                    )
                )
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(Exception(extractErrorMessage(responseBody) ?: "HTTP ${response.code}"))
                }
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val text = StringBuilder()
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            text.append(parts.getJSONObject(i).optString("text"))
                        }
                    }
                }
                Result.success(text.toString().trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            JSONObject(responseBody).optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
    }
}
