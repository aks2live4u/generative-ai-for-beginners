package com.braindump.app.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Minimal client for the Claude Messages API (https://docs.anthropic.com/). */
object ClaudeClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val API_VERSION = "2023-06-01"

    /**
     * @param messages JSON array of {"role": "user"|"assistant", "content": ...}
     *                  where content is a string or an array of content blocks.
     */
    fun sendMessage(
        apiKey: String,
        model: String,
        systemPrompt: String?,
        messages: JSONArray,
        maxTokens: Int = 1024
    ): Result<String> {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", maxTokens)
                if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
                put("messages", messages)
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(Exception(extractErrorMessage(responseBody) ?: "HTTP ${response.code}"))
                }
                val json = JSONObject(responseBody)
                val content = json.optJSONArray("content")
                val text = StringBuilder()
                if (content != null) {
                    for (i in 0 until content.length()) {
                        val block = content.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            text.append(block.optString("text"))
                        }
                    }
                }
                Result.success(text.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Builds a user content block array from text plus optional inline images. */
    fun buildUserContent(text: String, images: List<Pair<String, String>>): JSONArray {
        val content = JSONArray()
        for ((mediaType, base64Data) in images) {
            content.put(
                JSONObject().apply {
                    put("type", "image")
                    put(
                        "source",
                        JSONObject().apply {
                            put("type", "base64")
                            put("media_type", mediaType)
                            put("data", base64Data)
                        }
                    )
                }
            )
        }
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", text)
        })
        return content
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            JSONObject(responseBody).optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
    }
}
