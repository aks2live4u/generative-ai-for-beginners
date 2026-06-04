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
        val prompt = buildPrompt(stockData, decision)

        val toolsArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "web_search_20250305")
                addProperty("name", "web_search")
            })
        }

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "user")
                add("content", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", prompt)
                    })
                })
            })
        }

        val requestBody = JsonObject().apply {
            addProperty("model", "claude-opus-4-5")
            addProperty("max_tokens", 2000)
            add("tools", toolsArray)
            add("messages", messagesArray)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(requestBody)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("anthropic-beta", "web-search-2025-03-05")
            .header("content-type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response from Anthropic API")

        when (response.code) {
            429 -> throw RateLimitException("Rate limit reached. Please wait a moment and retry.")
            401 -> throw IOException("Invalid API key. Please check your ANTHROPIC_API_KEY in local.properties.")
            else -> if (!response.isSuccessful) {
                throw IOException("Anthropic API error ${response.code}: $responseBody")
            }
        }

        return parseResponseText(responseBody)
    }

    private fun parseResponseText(responseBody: String): String {
        val json = JsonParser.parseString(responseBody).asJsonObject
        val contentArray = json.getAsJsonArray("content") ?: return "No analysis available."
        val sb = StringBuilder()
        for (item in contentArray) {
            val obj = item.asJsonObject
            if (obj.get("type")?.asString == "text") {
                sb.append(obj.get("text")?.asString ?: "")
            }
        }
        return sb.toString().trim().ifEmpty { "Analysis completed but no text was returned." }
    }

    private fun buildPrompt(data: StockData, decision: String): String {
        val startPrice = data.priceHistory.firstOrNull()?.closePrice ?: data.currentPrice
        val pctChange = if (startPrice > 0) {
            ((data.currentPrice - startPrice) / startPrice * 100).roundToInt()
        } else 0
        val maxDrawdown = calculateMaxDrawdown(data.priceHistory)
        val marketCapStr = data.marketCap?.let { formatLargeNumber(it) } ?: "N/A"
        val peStr = data.peRatio?.let { String.format("%.1f", it) } ?: "N/A"
        val volumeStr = formatLargeNumber(data.volume)

        return """
You are a professional financial research analyst with access to real-time web search. A user wants to $decision ${data.symbol}.

Current Market Data:
- Symbol: ${data.symbol}
- Current Price: ${"$"}${String.format("%.2f", data.currentPrice)}
- 52-Week High: ${"$"}${String.format("%.2f", data.fiftyTwoWeekHigh)}
- 52-Week Low: ${"$"}${String.format("%.2f", data.fiftyTwoWeekLow)}
- P/E Ratio: $peStr
- Market Cap: $marketCapStr
- Volume: $volumeStr

5-Year Price History:
- Price ~5 years ago: ${"$"}${String.format("%.2f", startPrice)}
- Current Price: ${"$"}${String.format("%.2f", data.currentPrice)}
- Total Return: $pctChange%
- Maximum Drawdown: ${maxDrawdown.roundToInt()}%
- Monthly data points: ${data.priceHistory.size}

Using your web search capability, research the latest news, analyst opinions, earnings reports, and market trends for ${data.symbol}.

Provide a comprehensive analysis of whether the user's decision to $decision ${data.symbol} is WISE, RISKY, or NEUTRAL.

Format your response EXACTLY as follows:

VERDICT: [WISE/RISKY/NEUTRAL]

MARKET ANALYSIS:
[2-3 paragraphs on current market conditions, recent news, and sector trends]

TECHNICAL ANALYSIS:
[Analysis of the 5-year price trend, momentum, support/resistance levels]

FUNDAMENTAL ANALYSIS:
[PE ratio context, revenue/earnings growth, competitive position, valuation]

RISK FACTORS:
[3-5 key risks to this investment decision]

RECOMMENDATION:
[Final recommendation with clear reasoning for the $decision decision]

Disclaimer: This analysis is for educational purposes only and does not constitute financial advice.
        """.trimIndent()
    }

    private fun calculateMaxDrawdown(prices: List<com.stockadvisor.data.model.PricePoint>): Double {
        if (prices.isEmpty()) return 0.0
        var maxDrawdown = 0.0
        var peak = prices.first().closePrice
        for (point in prices) {
            if (point.closePrice > peak) peak = point.closePrice
            val drawdown = if (peak > 0) (peak - point.closePrice) / peak * 100 else 0.0
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }
        return maxDrawdown
    }

    private fun formatLargeNumber(n: Long): String = when {
        n >= 1_000_000_000_000L -> String.format("$%.2fT", n / 1_000_000_000_000.0)
        n >= 1_000_000_000L    -> String.format("$%.2fB", n / 1_000_000_000.0)
        n >= 1_000_000L        -> String.format("$%.2fM", n / 1_000_000.0)
        else                   -> "$$n"
    }
}
