package com.stockadvisor.data.network

import com.stockadvisor.data.model.StockData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class RateLimitException(message: String) : IOException(message)

class AnthropicService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Throws(IOException::class)
    fun analyzeStock(stockData: StockData, decision: String): String {
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            throw IOException("Invalid API key: open local.properties and set ANTHROPIC_API_KEY=sk-ant-...")
        }

        val prompt = buildPrompt(stockData, decision)

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", prompt)
            })
        }

        val requestBody = JsonObject().apply {
            addProperty("model", "claude-sonnet-4-6")
            addProperty("max_tokens", 2000)
            add("messages", messagesArray)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(requestBody)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response from Anthropic API")

        when (response.code) {
            200 -> { /* success, fall through */ }
            401 -> throw IOException("Invalid API key. Please check ANTHROPIC_API_KEY in local.properties.")
            429 -> throw RateLimitException("Rate limit reached. Please wait a moment and retry.")
            else -> {
                val errMsg = try {
                    JsonParser.parseString(responseBody).asJsonObject
                        .getAsJsonObject("error")?.get("message")?.asString ?: responseBody
                } catch (_: Exception) { responseBody.take(200) }
                throw IOException("API error ${response.code}: $errMsg")
            }
        }

        return parseResponseText(responseBody)
    }

    private fun parseResponseText(body: String): String {
        val content = JsonParser.parseString(body).asJsonObject
            .getAsJsonArray("content") ?: return "No analysis available."
        val sb = StringBuilder()
        for (item in content) {
            val obj = item.asJsonObject
            if (obj.get("type")?.asString == "text") {
                sb.append(obj.get("text")?.asString ?: "")
            }
        }
        return sb.toString().trim().ifEmpty { "Analysis completed but no text returned." }
    }

    private fun buildPrompt(data: StockData, decision: String): String {
        val startPrice = data.priceHistory.firstOrNull()?.closePrice ?: data.currentPrice
        val pctChange = if (startPrice > 0) ((data.currentPrice - startPrice) / startPrice * 100).roundToInt() else 0
        val maxDrawdown = calculateMaxDrawdown(data.priceHistory)
        val peStr = data.peRatio?.let { String.format("%.1f", it) } ?: "N/A"
        val marketCapStr = data.marketCap?.let { formatLargeNumber(it) } ?: "N/A"

        return """
You are a professional financial research analyst. A user wants to $decision ${data.symbol}.

Current Market Data:
- Symbol: ${data.symbol}
- Current Price: ${"$"}${String.format("%.2f", data.currentPrice)}
- 52-Week High: ${"$"}${String.format("%.2f", data.fiftyTwoWeekHigh)}
- 52-Week Low: ${"$"}${String.format("%.2f", data.fiftyTwoWeekLow)}
- P/E Ratio: $peStr
- Market Cap: $marketCapStr
- Volume: ${formatLargeNumber(data.volume)}

5-Year History:
- 5 years ago: ${"$"}${String.format("%.2f", startPrice)}
- Today: ${"$"}${String.format("%.2f", data.currentPrice)}
- Total Return: $pctChange%
- Max Drawdown: ${maxDrawdown.roundToInt()}%

Provide a comprehensive analysis of whether $decision of ${data.symbol} is WISE, RISKY, or NEUTRAL.

Respond in this EXACT format:

VERDICT: [WISE/RISKY/NEUTRAL]

MARKET ANALYSIS:
[2–3 paragraphs on current market conditions and sector trends]

TECHNICAL ANALYSIS:
[Price trend, support/resistance, momentum over 5 years]

FUNDAMENTAL ANALYSIS:
[PE ratio context, growth, competitive position, valuation]

RISK FACTORS:
[3–5 key risks bullet points]

RECOMMENDATION:
[Final clear reasoning for the $decision decision]

Disclaimer: Educational purposes only. Not financial advice.
        """.trimIndent()
    }

    private fun calculateMaxDrawdown(prices: List<com.stockadvisor.data.model.PricePoint>): Double {
        if (prices.isEmpty()) return 0.0
        var maxDrawdown = 0.0
        var peak = prices.first().closePrice
        for (p in prices) {
            if (p.closePrice > peak) peak = p.closePrice
            val dd = if (peak > 0) (peak - p.closePrice) / peak * 100 else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }
        return maxDrawdown
    }

    private fun formatLargeNumber(n: Long): String = when {
        n >= 1_000_000_000_000L -> "$%.2fT".format(n / 1_000_000_000_000.0)
        n >= 1_000_000_000L    -> "$%.2fB".format(n / 1_000_000_000.0)
        n >= 1_000_000L        -> "$%.2fM".format(n / 1_000_000.0)
        else                   -> "$$n"
    }
}
