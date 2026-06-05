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
            200 -> { }
            401 -> throw IOException("Invalid API key. Update it via the settings icon.")
            429 -> throw RateLimitException("Rate limit reached. Please wait and retry.")
            else -> {
                val errMsg = try {
                    JsonParser.parseString(responseBody).asJsonObject
                        .getAsJsonObject("error")?.get("message")
                        ?.takeIf { !it.isJsonNull }?.asString ?: responseBody
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
            if (!item.isJsonObject) continue           // guard against JsonNull elements
            val obj = item.asJsonObject
            val type = obj.get("type")?.takeIf { !it.isJsonNull }?.asString
            if (type == "text") {
                obj.get("text")?.takeIf { !it.isJsonNull }?.asString?.let { sb.append(it) }
            }
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
        val vsLow = if (data.fiftyTwoWeekLow > 0)
            ((data.currentPrice - data.fiftyTwoWeekLow) / data.fiftyTwoWeekLow * 100).roundToInt() else 100
        val rangePosition = if (data.fiftyTwoWeekHigh > data.fiftyTwoWeekLow)
            ((data.currentPrice - data.fiftyTwoWeekLow) / (data.fiftyTwoWeekHigh - data.fiftyTwoWeekLow) * 100).roundToInt()
            else 50

        // 3-month price trend from history
        val recentTrend3M = run {
            val n = data.priceHistory.size
            if (n >= 4) {
                val p3m = data.priceHistory[n - 4].closePrice
                val pNow = data.priceHistory.last().closePrice
                if (p3m > 0) ((pNow - p3m) / p3m * 100).roundToInt() else 0
            } else 0
        }
        val trendDesc = when {
            recentTrend3M >= 10  -> "↑ UPTREND (+$recentTrend3M% in 3 months)"
            recentTrend3M <= -10 -> "↓ DOWNTREND ($recentTrend3M% in 3 months)"
            else                 -> "→ SIDEWAYS ($recentTrend3M% in 3 months)"
        }

        val currencySymbol = if ("NS" in data.symbol || "BO" in data.symbol || data.symbol.startsWith("MF:")) "₹" else "$"
        val isIndian = currencySymbol == "₹"
        val isMutualFund = data.assetType.contains("FUND", ignoreCase = true) || data.symbol.startsWith("MF:")
        val priceLabel = if (isMutualFund) "NAV" else "Price"

        fun fmt1(v: Double?) = v?.let { String.format("%.1f", it) } ?: "N/A"
        fun fmt2(v: Double?) = v?.let { String.format("%.2f", it) } ?: "N/A"
        val peStr  = fmt1(data.peRatio)
        val pbStr  = fmt1(data.pbRatio)
        val epsStr = fmt2(data.eps)
        val deStr  = fmt1(data.debtToEquity)
        val roeStr = data.roe?.let { "${String.format("%.2f", it)}%" } ?: "N/A"
        val divStr = data.dividendYield?.let { "${String.format("%.2f", it)}%" } ?: "N/A"
        val revGStr = data.revenueGrowth?.let { "${if (it >= 0) "+" else ""}${String.format("%.1f", it)}% YoY" } ?: "N/A"
        val epsGStr = data.earningsGrowth?.let { "${if (it >= 0) "+" else ""}${String.format("%.1f", it)}% YoY" } ?: "N/A"
        val marketCapStr = data.marketCap?.let { formatLargeNumber(it, currencySymbol) } ?: "N/A"

        val instrumentTypeLabel = when {
            isMutualFund -> "Indian Mutual Fund"
            "ETF" in data.assetType.uppercase() -> "ETF"
            "INDEX" in data.assetType.uppercase() -> "Index"
            else -> "Equity"
        }

        val pricePositionDesc = when {
            rangePosition <= 10 -> "VERY NEAR 52W LOW ($rangePosition% of range) — near yearly bottom"
            rangePosition <= 25 -> "In lower range ($rangePosition% of range) — below yearly average"
            rangePosition >= 90 -> "VERY NEAR 52W HIGH ($rangePosition% of range) — near yearly top"
            rangePosition >= 75 -> "In upper range ($rangePosition% of range) — above yearly average"
            else                -> "Mid-range ($rangePosition% of range)"
        }

        val pnlPct: Double?
        val positionSection = if ((decision == "SELL" || decision == "HOLD") && (quantity != null || avgBuyPrice != null)) {
            val qtyStr = quantity?.toString() ?: "?"
            val buyStr = avgBuyPrice?.let { "$currencySymbol${String.format("%.2f", it)}" } ?: "?"
            val pnl = if (avgBuyPrice != null && quantity != null) (data.currentPrice - avgBuyPrice) * quantity else null
            pnlPct = if (avgBuyPrice != null && avgBuyPrice > 0) ((data.currentPrice - avgBuyPrice) / avgBuyPrice * 100) else null
            val pnlStr = if (pnl != null) {
                val sign = if (pnl >= 0) "+" else ""
                "$sign$currencySymbol${String.format("%.0f", abs(pnl))} (${if (pnlPct!! >= 0) "+" else ""}${String.format("%.1f", pnlPct)}%)"
            } else "N/A"
            """
USER'S CURRENT POSITION:
- Quantity: $qtyStr units  |  Avg buy price: $buyStr
- Unrealized P&L: $pnlStr
"""
        } else { pnlPct = null; "" }

        val sellContextNote = when {
            decision == "SELL" && pnlPct != null && pnlPct < -15 ->
                "⚠️ USER IS IN A LOSS OF ${String.format("%.1f", pnlPct)}%. $pricePositionDesc. Selling NOW permanently locks in this loss."
            decision == "SELL" && rangePosition <= 25 ->
                "⚠️ $pricePositionDesc. Selling near the yearly low is almost always bad timing."
            decision == "SELL" && pnlPct != null && pnlPct >= 20 ->
                "ℹ️ USER IS IN PROFIT ${String.format("%.1f", pnlPct)}%. $pricePositionDesc."
            else -> pricePositionDesc
        }

        return """
You are a senior Indian stock market advisor who gives advice like a smart, experienced friend — honest, direct, and caring about the investor's money. Think like Rakesh Jhunjhunwala for long-term calls and a seasoned technical analyst for short-term calls.

The user wants to $decision: ${data.name.ifBlank { data.symbol }}

═══ MARKET DATA (Yahoo Finance, $today) ═══
Name: ${data.name.ifBlank { data.symbol }}  |  Symbol: ${data.symbol}  |  Type: $instrumentTypeLabel
Current $priceLabel: $currencySymbol${String.format("%.2f", data.currentPrice)}
52W High: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekHigh)}  |  52W Low: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekLow)}
vs 52W High: $vsHigh%  |  vs 52W Low: +$vsLow%  |  Range position: $pricePositionDesc
Recent 3M Trend: $trendDesc
5-Year Return: $pctChange%  |  Max Drawdown (5Y): ${maxDrawdown.roundToInt()}%

FUNDAMENTALS:
P/E Ratio (TTM): $peStr  |  P/B Ratio: $pbStr  |  EPS (TTM): $epsStr
ROE: $roeStr  |  Debt/Equity: $deStr  |  Dividend Yield: $divStr
Revenue Growth: $revGStr  |  Earnings Growth: $epsGStr
Market Cap: $marketCapStr
$positionSection
CONTEXT: $sellContextNote

═══ INVESTMENT RULES — APPLY STRICTLY ═══

RULE 1 — NEVER SELL AT THE BOTTOM:
If price is within 25% of its 52W Low (range position ≤ 25%), recommending SELL is terrible advice — the investor sells at maximum pain. Correct advice: HOLD for recovery. Only exception: company is going bankrupt.

RULE 2 — DON'T LOCK IN PAPER LOSSES:
A loss is only real when you sell. If the company is fundamentally sound, hold. The stock will return to fair value. A -33% loss fully recovers once the price rebounds.

RULE 3 — P/E CONTEXT MATTERS:
• P/E > 80: Very expensive, future growth already priced in. Be cautious on BUY.
• P/E 20-60: Normal for growth stocks, especially Indian tech/consumer.
• P/E < 20: Cheap, but ask why. Could be a bargain or a value trap.
• P/E N/A: Either loss-making or data unavailable. Check earnings trend.

RULE 4 — 3-MONTH TREND IS KEY FOR SHORT-TERM:
If the recent trend is downward (↓), it means momentum is negative. Short-term traders should wait for stabilization before entering.

RULE 5 — VERDICT GUIDELINES:
${when (decision) {
    "SELL" -> "• WISE: near 52W high, in profit >15%, OR fundamental breakdown\n• NEUTRAL: mid-range, mixed signals\n• RISKY: near 52W low, in loss — worst time to sell"
    "BUY"  -> "• WISE: near 52W low, positive 5Y return, reasonable P/E, uptrend forming\n• NEUTRAL: mid-range, fair entry but not ideal\n• RISKY: near 52W high, very high P/E, strong downtrend"
    else   -> "• WISE: fundamentally sound, temporary dip, wait for recovery\n• NEUTRAL: mixed signals, hold but monitor\n• RISKY: structural business decline, negative 5Y returns"
}}
RULE 6 — MENTION SIP FOR INDIAN STOCKS:
For Indian equity with uncertain short-term outlook, always mention the SIP (Systematic Investment Plan) option — buying fixed amounts monthly instead of a lump sum. This averages the cost and reduces timing risk.

═══ OUTPUT FORMAT — FOLLOW EXACTLY ═══

VERDICT: [WISE/RISKY/NEUTRAL]
CONFIDENCE: [HIGH/MEDIUM/LOW]

INSTRUMENT: ${data.name.ifBlank { data.symbol }} | ${data.symbol} | $instrumentTypeLabel

SUMMARY:
• [most important insight — plain English, max 14 words]
• [second key point — plain English, max 14 words]
• [third key point — plain English, max 14 words]

STRENGTHS:
• [specific strength with a number — max 14 words]
• [specific strength — max 14 words]
• [specific strength — max 14 words]

RISKS:
• [specific risk with context — max 14 words]
• [specific risk — max 14 words]
• [specific risk — max 14 words]

EXPERT ADVICE:

FOR BUYERS:
[2-3 sentences. Should I buy a lump sum today? Reference the P/E, 3M trend, and 52W range position with actual numbers. Be direct and honest.]

FOR LONG TERM:
[2-3 sentences. Is this a good long-term hold or SIP candidate? ${if (isIndian) "Mention whether monthly SIP is better than a lump sum, and why." else "Discuss long-term investment strategy."} Use actual numbers.]

FOR TRADERS:
[1-2 sentences. Is the short-term momentum up or down? Reference the 3M trend. When to enter or stay away?]

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
