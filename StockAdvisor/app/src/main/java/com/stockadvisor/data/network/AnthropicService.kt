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
        // rangePosition: 0% = at 52W low, 100% = at 52W high
        val rangePosition = if (data.fiftyTwoWeekHigh > data.fiftyTwoWeekLow)
            ((data.currentPrice - data.fiftyTwoWeekLow) / (data.fiftyTwoWeekHigh - data.fiftyTwoWeekLow) * 100).roundToInt()
            else 50

        val currencySymbol = if ("NS" in data.symbol || "BO" in data.symbol || data.symbol.startsWith("MF:")) "₹" else "$"
        val peStr = data.peRatio?.let { String.format("%.1f", it) } ?: "N/A"
        val marketCapStr = data.marketCap?.let { formatLargeNumber(it, currencySymbol) } ?: "N/A"
        val isMutualFund = data.assetType.contains("FUND", ignoreCase = true) || data.symbol.startsWith("MF:")
        val priceLabel = if (isMutualFund) "NAV" else "Price"

        val pnlPct: Double?
        val positionSection = if ((decision == "SELL" || decision == "HOLD") && (quantity != null || avgBuyPrice != null)) {
            val qtyStr = quantity?.toString() ?: "?"
            val buyStr = avgBuyPrice?.let { "$currencySymbol${String.format("%.2f", it)}" } ?: "?"
            val pnl = if (avgBuyPrice != null && quantity != null)
                (data.currentPrice - avgBuyPrice) * quantity else null
            pnlPct = if (avgBuyPrice != null && avgBuyPrice > 0)
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
        } else {
            pnlPct = null
            ""
        }

        val instrumentTypeLabel = when {
            isMutualFund -> "Indian Mutual Fund"
            "ETF" in data.assetType.uppercase() -> "ETF"
            "INDEX" in data.assetType.uppercase() -> "Index"
            else -> "Equity"
        }

        // Context clues for the prompt so Claude can reason correctly
        val pricePositionDesc = when {
            rangePosition <= 10 -> "VERY NEAR 52-WEEK LOW ($rangePosition% of yearly range) — stock is at/near its lowest point this year"
            rangePosition <= 25 -> "Near 52-week low ($rangePosition% of yearly range) — stock is in the lower quarter of its yearly range"
            rangePosition >= 90 -> "VERY NEAR 52-WEEK HIGH ($rangePosition% of yearly range) — stock is near its highest point this year"
            rangePosition >= 75 -> "Near 52-week high ($rangePosition% of yearly range) — stock is in the upper quarter of its yearly range"
            else -> "Mid-range ($rangePosition% of yearly range)"
        }

        val sellContextNote = if (decision == "SELL") {
            val lossNote = if (pnlPct != null && pnlPct < -15)
                "\n- USER IS IN A LOSS OF ${String.format("%.1f", pnlPct)}% — selling now permanently locks in this loss."
            else if (pnlPct != null && pnlPct >= 20)
                "\n- USER IS IN A PROFIT OF ${String.format("%.1f", pnlPct)}% — selling takes a meaningful gain off the table."
            else ""
            "SELL CONTEXT: $pricePositionDesc$lossNote"
        } else if (decision == "BUY") {
            "BUY CONTEXT: $pricePositionDesc"
        } else {
            "HOLD CONTEXT: $pricePositionDesc"
        }

        return """
You are a senior stock advisor at a reputable Indian investment firm. You think like Warren Buffett and Rakesh Jhunjhunwala — long-term, patient, fundamentals-first. A user is asking whether to $decision ${data.name.ifBlank { data.symbol }}.

MARKET DATA (${if (isMutualFund) "AMFI/mfapi.in" else "Yahoo Finance"}, $today):
- Name: ${data.name.ifBlank { data.symbol }}
- Symbol: ${data.symbol} | Type: $instrumentTypeLabel
- Current $priceLabel: $currencySymbol${String.format("%.2f", data.currentPrice)}
- 52-Week High: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekHigh)}  |  52-Week Low: $currencySymbol${String.format("%.2f", data.fiftyTwoWeekLow)}
- Price vs 52W High: $vsHigh%  |  Price vs 52W Low: +$vsLow%
- Position in yearly range: $pricePositionDesc
- 5-Year Return: $pctChange%
- Max Single Drawdown (5Y): ${maxDrawdown.roundToInt()}%
- P/E Ratio: $peStr  |  Market Cap: $marketCapStr
$positionSection
$sellContextNote

═══════════════════════════════════════════════════════════
INVESTMENT LOGIC — APPLY THESE RULES STRICTLY:
═══════════════════════════════════════════════════════════

RULE 1 — NEVER RECOMMEND SELLING AT THE BOTTOM:
If the stock is within 20% of its 52-week LOW (range position ≤ 25%), recommending SELL is almost always BAD advice. The investor would be selling at maximum pain, locking in peak losses. The stock has already fallen hard — the damage is done. The correct advice is usually HOLD and wait for recovery.
Exception: only recommend selling near the low if there is strong evidence the company is going bankrupt or fundamentally broken.

RULE 2 — DON'T CRYSTALLIZE LOSSES:
If the user is in an unrealized loss (negative P&L), selling converts a "paper loss" into a permanent, real loss. Unless the company's future is genuinely broken, always lean toward HOLD. Market cycles recover. A -33% loss recovers fully once the stock goes back to the buy price.

RULE 3 — A FALLING STOCK ≠ A BAD COMPANY:
Stock prices go up and down with market sentiment, economic cycles, and sector rotations. A stock down 30-40% from its high but with a positive 5-year return is showing normal market volatility, NOT permanent value destruction. Treat it like a sale at a store — the business may still be good.

RULE 4 — THE 52-WEEK RANGE TELLS THE STORY:
• Price in bottom 0-25% of range: stock is beaten down — HOLD if fundamentals okay, RISKY to SELL, potential BUY opportunity
• Price in middle 25-75% of range: neutral zone — use other metrics to decide
• Price in top 75-100% of range: stock is extended — CAUTION on BUY, might be good time to take profits on SELL

RULE 5 — 5-YEAR RETURN IS THE HEALTH CHECK:
• Positive 5Y return (>0%) = business is still growing over time. Temporary dip is likely recoverable.
• Negative 5Y return = business might be structurally declining. More caution warranted.

RULE 6 — FOR BUY DECISIONS:
• Near 52W Low + positive 5Y return = potentially good entry point (buying when others are fearful)
• Near 52W High = paying a premium, higher risk of short-term pullback
• High P/E (>40) = expensive stock, lower margin of safety

RULE 7 — FOR SELL/HOLD WITH A LOSS:
• Ask: "Has the reason I bought this stock changed?" If not, holding is almost always better than selling at a loss.
• The recommendation should acknowledge the loss honestly but guide the user correctly: do NOT sell at the bottom.
• If the stock has strong 5Y returns and is near its low → clearly say: "Hold and wait for recovery."

RULE 8 — VERDICT GUIDELINES FOR $decision:
${when (decision) {
    "SELL" -> """• WISE to sell: stock is near 52W HIGH and user is in profit (>15%), OR company has major fundamental problems
• NEUTRAL: stock is mid-range, user is in small profit/loss, unclear direction
• RISKY to sell: stock is near 52W LOW, user is in loss — selling locks in maximum loss at the worst time"""
    "BUY" -> """• WISE to buy: stock near 52W LOW with strong 5Y returns, reasonable P/E, established company
• NEUTRAL: mid-range stock, moderate metrics, reasonable entry but not exceptional
• RISKY to buy: stock near 52W HIGH, high P/E (>40), or major drawdown with negative 5Y returns"""
    else -> """• WISE to hold: stock is fundamentally sound, temporary dip, user should wait for recovery
• NEUTRAL: mixed signals, holding is reasonable but monitor closely
• RISKY to hold: persistent negative 5Y returns, fundamental business problems"""
}}
═══════════════════════════════════════════════════════════

FORMATTING INSTRUCTIONS:
1. Use the rules above to reason about this specific stock's situation.
2. Write in simple, plain English — imagine explaining to a friend.
3. Be direct and honest. If the user is making a mistake (e.g., wanting to sell at the bottom), say so clearly and explain why.
4. Max 12 words per bullet. No jargon. If technical term needed, explain in brackets.
5. Follow the EXACT output format below.

OUTPUT FORMAT — follow this EXACTLY:

VERDICT: [WISE/RISKY/NEUTRAL]
CONFIDENCE: [HIGH/MEDIUM/LOW]

INSTRUMENT: ${data.name.ifBlank { data.symbol }} | ${data.symbol} | $instrumentTypeLabel

SUMMARY:
• [key takeaway — max 12 words, plain English]
• [key takeaway — max 12 words, plain English]
• [key takeaway — max 12 words, plain English]

METRICS:
Current $priceLabel | $currencySymbol${String.format("%.2f", data.currentPrice)} | -
52W High | $currencySymbol${String.format("%.2f", data.fiftyTwoWeekHigh)} | -
52W Low | $currencySymbol${String.format("%.2f", data.fiftyTwoWeekLow)} | -
vs 52W High | $vsHigh% | [↑ GOOD if > -5%, ↓ CAUTION otherwise]
vs 52W Low | +$vsLow% | [stock is ${if (vsLow <= 10) "↓ AT/NEAR BOTTOM" else if (vsLow <= 30) "→ RECOVERING" else "↑ WELL ABOVE LOW"}]
5Y Return | $pctChange% | [↑ GOOD if > 50%, → NEUTRAL 10-50%, ↓ WEAK if < 10%]
Max Drawdown | ${maxDrawdown.roundToInt()}% | [↓ HIGH RISK if > 40%, → MODERATE 20-40%, ↑ LOW if < 20%]
P/E Ratio | $peStr | [↓ EXPENSIVE if > 40, → FAIR 15-40, ↑ CHEAP if < 15]
Market Cap | $marketCapStr | -
${if (positionSection.isNotBlank()) """
YOUR POSITION:
Avg Buy Price | [from position data] | -
Unrealized P&L | [calculate from position data] | [↑ PROFIT or ↓ LOSS]
P&L % | [calculate] | [note: selling locks this in permanently]""" else ""}
STRENGTHS:
• [positive factor — max 12 words]
• [positive factor — max 12 words]
• [positive factor — max 12 words]

RISKS:
• [risk factor — max 12 words]
• [risk factor — max 12 words]
• [risk factor — max 12 words]

RECOMMENDATION:
[2-3 sentences. Be a real advisor. Be honest. If the user is asking to sell at the bottom with a big loss, tell them clearly that is bad advice and why. Use the investment rules above. Plain English only.]

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
