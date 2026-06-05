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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
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
    fun analyzeStock(
        stockData: StockData,
        decision: String,
        quantity: Int? = null,
        avgBuyPrice: Double? = null
    ): String {
        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            throw IOException("API key not set. Tap the settings icon to add your key.")
        }

        val prompt = buildPrompt(stockData, decision, quantity, avgBuyPrice)

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", prompt)
            })
        }

        val requestBody = JsonObject().apply {
            addProperty("model", "claude-sonnet-4-6")
            addProperty("max_tokens", 1500)
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
            200 -> { }
            401 -> throw IOException("Invalid API key. Update it via the settings icon.")
            429 -> throw RateLimitException("Rate limit reached. Please wait and retry.")
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
            if (obj.get("type")?.asString == "text") sb.append(obj.get("text")?.asString ?: "")
        }
        return sb.toString().trim().ifEmpty { "Analysis completed but no text returned." }
    }

    private fun buildPrompt(
        data: StockData,
        decision: String,
        quantity: Int?,
        avgBuyPrice: Double?
    ): String {
        val today = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        val startPrice = data.priceHistory.firstOrNull()?.closePrice ?: data.currentPrice
        val pctChange = if (startPrice > 0) ((data.currentPrice - startPrice) / startPrice * 100).roundToInt() else 0
        val maxDrawdown = calculateMaxDrawdown(data.priceHistory)
        val vsHigh = if (data.fiftyTwoWeekHigh > 0)
            ((data.currentPrice - data.fiftyTwoWeekHigh) / data.fiftyTwoWeekHigh * 100).roundToInt() else 0
        val currencySymbol = if ("NS" in data.symbol || "BO" in data.symbol || data.symbol.startsWith("MF:")) "₹" else "$"
        val peStr = data.peRatio?.let { String.format("%.1f", it) } ?: "N/A"
        val marketCapStr = data.marketCap?.let { formatLargeNumber(it, currencySymbol) } ?: "N/A"
        val isMutualFund = data.assetType.contains("FUND", ignoreCase = true) || data.symbol.startsWith("MF:")
        val priceLabel = if (isMutualFund) "NAV" else "Price"

        // Position section for SELL/HOLD
        val positionSection = if ((decision == "SELL" || decision == "HOLD") && (quantity != null || avgBuyPrice != null)) {
            val qtyStr = quantity?.toString() ?: "?"
            val buyStr = avgBuyPrice?.let { "$currencySymbol${String.format("%.2f", it)}" } ?: "?"
            val pnl = if (avgBuyPrice != null && quantity != null)
                (data.currentPrice - avgBuyPrice) * quantity else null
            val pnlPct = if (avgBuyPrice != null && avgBuyPrice > 0)
                ((data.currentPrice - avgBuyPrice) / avgBuyPrice * 100) else null
            val pnlStr = if (pnl != null) {
                val sign = if (pnl >= 0) "+" else ""
                "$sign$currencySymbol${String.format("%.0f", abs(pnl))} (${if (pnlPct!! >= 0) "+" else ""}${String.format("%.1f", pnlPct)}%)"
            } else "N/A"

            """
USER'S CURRENT POSITION:
- Quantity held: $qtyStr units
- Average buy price: $buyStr
- Current $priceLabel: $currencySymbol${String.format("%.2f", data.currentPrice)}
- Unrealized P&L: $pnlStr
"""
        } else ""

        val instrumentTypeLabel = when {
            isMutualFund -> "Indian Mutual Fund"
            "ETF" in data.assetType.uppercase() -> "ETF"
            "INDEX" in data.assetType.uppercase() -> "Index"
            else -> "Equity"
        }

        return """
You are a financial research assistant. A user wants to $decision the following instrument.

INSTRUMENT DATA (source: ${if (isMutualFund) "AMFI/mfapi.in" else "Yahoo Finance"}, retrieved $today):
- Symbol: ${data.symbol}
- Full Name: ${data.name.ifBlank { data.symbol }}
- Type: $instrumentTypeLabel
- Current $priceLabel: $currencySymbol${String.format("%.2f", data.currentPrice)}
- 52-Week High: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekHigh)}
- 52-Week Low: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekLow)}
- vs 52W High: $vsHigh%
- P/E Ratio: $peStr
- Market Cap / AUM: $marketCapStr
- 5Y Return: $pctChange%
- Max Drawdown (5Y): ${maxDrawdown.roundToInt()}%
$positionSection
CRITICAL INSTRUCTIONS:
1. You are analyzing ONLY the instrument listed above (${data.name.ifBlank { data.symbol }}). Do NOT invent data not provided here.
2. If the instrument name does not match a typical $decision candidate, state this at the top.
3. If data seems insufficient (e.g., a newly listed fund with little history), say so clearly.
4. Keep every section SHORT — use bullet points, not paragraphs.
5. Follow the EXACT output format below. Do not add or rename sections.

OUTPUT FORMAT — follow this EXACTLY:

VERDICT: [WISE/RISKY/NEUTRAL]
CONFIDENCE: [HIGH/MEDIUM/LOW]

INSTRUMENT: ${data.name.ifBlank { data.symbol }} | ${data.symbol} | $instrumentTypeLabel

SUMMARY:
• [key insight 1 — max 15 words]
• [key insight 2 — max 15 words]
• [key insight 3 — max 15 words]

METRICS:
Current $priceLabel | $currencySymbol${String.format("%.2f", data.currentPrice)} | -
52W High | $currencySymbol${String.format("%.2f", data.fiftyTwoWeekHigh)} |
52W Low | $currencySymbol${String.format("%.2f", data.fiftyTwoWeekLow)} |
vs 52W High | $vsHigh% | [↑ GOOD if > -5%, ↓ CAUTION otherwise]
5Y Return | $pctChange% | [↑ GOOD if > 50%, → NEUTRAL 10-50%, ↓ WEAK if < 10%]
Max Drawdown | ${maxDrawdown.roundToInt()}% | [↓ HIGH RISK if > 40%, → MODERATE 20-40%, ↑ LOW if < 20%]
P/E Ratio | $peStr | [↓ HIGH if > 40, → FAIR 15-40, ↑ VALUE if < 15, or N/A]
Market Cap | $marketCapStr | -
${if (positionSection.isNotBlank()) """
YOUR POSITION:
Avg Buy Price | [from position data] | -
Unrealized P&L | [calculate from position data] | [↑ PROFIT or ↓ LOSS]
P&L % | [calculate] | [↑ or ↓]""" else ""}
STRENGTHS:
• [positive factor — max 12 words]
• [positive factor — max 12 words]
• [positive factor — max 12 words]

RISKS:
• [risk factor — max 12 words]
• [risk factor — max 12 words]
• [risk factor — max 12 words]

RECOMMENDATION:
[2 sentences max. Direct, actionable advice for the $decision decision. If SELL/HOLD, factor in their position P&L.]

DATA: ${if (isMutualFund) "AMFI via mfapi.in" else "Yahoo Finance"} | Retrieved $today
DISCLAIMER: Educational only. Not financial advice.
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

    private fun formatLargeNumber(n: Long, currency: String): String = when {
        n >= 1_000_000_000_000L -> "$currency%.2fT".format(n / 1_000_000_000_000.0)
        n >= 1_000_000_000L    -> "$currency%.2fB".format(n / 1_000_000_000.0)
        n >= 1_000_000L        -> "$currency%.2fM".format(n / 1_000_000.0)
        else                   -> "$currency$n"
    }
}
