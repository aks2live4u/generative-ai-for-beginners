package com.braindump.app.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/** Fetches a random quote from ZenQuotes (https://zenquotes.io) for the home screen. */
object QuoteClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://zenquotes.io/api/random"

    fun fetchRandom(): Result<Pair<String, String>> {
        return try {
            val request = Request.Builder().url(API_URL).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}"))
                }
                val arr = JSONArray(response.body?.string().orEmpty())
                if (arr.length() == 0) return Result.failure(Exception("Empty response"))
                val obj = arr.getJSONObject(0)
                val text = obj.optString("q").trim()
                val author = obj.optString("a").trim()
                if (text.isEmpty()) return Result.failure(Exception("Empty quote"))
                Result.success(text to author.ifEmpty { "Unknown" })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
