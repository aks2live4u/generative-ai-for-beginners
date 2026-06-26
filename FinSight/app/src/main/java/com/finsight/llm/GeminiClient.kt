package com.finsight.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

sealed class GeminiResult {
    data class Success(val text: String) : GeminiResult()
    data class Failure(val message: String) : GeminiResult()
}

/**
 * Calls the Gemini API's generateContent REST endpoint directly over HTTPS (same
 * HttpURLConnection + org.json approach as [com.finsight.gmail.GmailScanner] - no new HTTP
 * library needed for a handful of simple POST requests). Uses the cheapest "flash" tier model,
 * which is plenty for summarizing/answering over the user's own transaction data.
 *
 * The API key and every byte of [prompt]/[systemInstruction] are supplied by the caller - this
 * class has no knowledge of whether the user has actually opted in; that gate lives in
 * [GeminiSettingsManager] and must be checked before calling [generateContent].
 */
class GeminiClient {

    suspend fun generateContent(
        apiKey: String,
        systemInstruction: String,
        prompt: String,
        model: String = "gemini-2.5-flash"
    ): GeminiResult = withContext(Dispatchers.IO) {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val requestBody = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            put("contents", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
        }

        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            // Smart Scan sends up to ~500 transaction lines (with redacted raw SMS/email text) for
            // the model to cross-reference for duplicates/fraud/insurance - that's enough reasoning
            // work that the model routinely takes longer than 20s to respond, which was surfacing
            // to the user as a generic "timeout" on every single run.
            connection.connectTimeout = 60_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode !in 200..299) {
                val errorMessage = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                    .getOrDefault("HTTP $responseCode")
                return@withContext GeminiResult.Failure(errorMessage)
            }

            val text = JSONObject(body)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            GeminiResult.Success(text)
        } catch (e: Exception) {
            GeminiResult.Failure(e.message ?: "Unknown error calling Gemini")
        } finally {
            connection.disconnect()
        }
    }
}
