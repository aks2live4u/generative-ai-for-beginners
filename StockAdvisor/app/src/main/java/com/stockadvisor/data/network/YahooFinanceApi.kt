package com.stockadvisor.data.network

import com.stockadvisor.data.model.PricePoint
import com.stockadvisor.data.model.StockData
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class YahooFinanceApi(private val client: OkHttpClient) {

    @Throws(IOException::class, IllegalArgumentException::class)
    fun fetchStockData(symbol: String): StockData {
        val (resolvedSymbol, chartData) = fetchChartWithAutoSuffix(symbol)
        // Quote data is supplementary (PE, market cap) — never fail the whole request if it's unavailable
        val quoteData: JsonObject? = try { fetchQuoteData(resolvedSymbol) } catch (_: Exception) { null }
        return mergeData(resolvedSymbol, chartData, quoteData)
    }

    /**
     * Tries the symbol as entered first, then appends .NS (NSE) and .BO (BSE) for Indian stocks.
     * Validates that the chart response actually contains data — a bare Yahoo Finance 200 with
     * null results (e.g. "ZOMATO" with no suffix) is treated the same as a 404.
     */
    private fun fetchChartWithAutoSuffix(symbol: String): Pair<String, JsonObject> {
        // User explicitly provided exchange suffix or index prefix — use exactly as-is
        if ('.' in symbol || symbol.startsWith("^")) {
            return symbol to fetchChartData(symbol)
        }

        val candidates = listOf(symbol, "$symbol.NS", "$symbol.BO")

        for (candidate in candidates) {
            try {
                val json = fetchChartData(candidate)
                val hasData = json.getAsJsonObject("chart")
                    ?.getAsJsonArray("result")
                    ?.let { it.size() > 0 } == true
                if (hasData) return candidate to json
                // 200 OK but null result — symbol doesn't exist under this suffix, try next
            } catch (e: IOException) {
                val msg = e.message ?: ""
                // Auth / network errors mean the service itself is unavailable — stop retrying
                if ("401" in msg || "403" in msg) throw e
                // 404 or any other fetch error → try next suffix
            }
        }

        throw IOException(
            "'$symbol' not found on any exchange.\n" +
            "For Indian NSE stocks add .NS (e.g. ${symbol}.NS).\n" +
            "For Nifty50 index use ^NSEI."
        )
    }

    private fun fetchChartData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1mo&range=5y"
        val response = client.newCall(buildRequest(url)).execute()
        val body = response.body?.string() ?: throw IOException("Empty chart response for $symbol")
        if (!response.isSuccessful) {
            throw IOException("Yahoo Finance chart error ${response.code} for $symbol")
        }
        return JsonParser.parseString(body).asJsonObject
    }

    private fun fetchQuoteData(symbol: String): JsonObject {
        // Try query2 first — it is often less restricted than query1
        for (host in listOf("query2", "query1")) {
            try {
                val url = "https://$host.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
                val response = client.newCall(buildRequest(url)).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    return JsonParser.parseString(body).asJsonObject
                }
            } catch (_: Exception) { }
        }
        throw IOException("Quote data unavailable for $symbol")
    }

    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Accept", "application/json,text/plain,*/*")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Origin", "https://finance.yahoo.com")
        .header("Referer", "https://finance.yahoo.com/")
        .build()

    private fun mergeData(symbol: String, chartJson: JsonObject, quoteJson: JsonObject?): StockData {
        val chartResult = chartJson
            .getAsJsonObject("chart")
            .getAsJsonArray("result")
            ?.takeIf { it.size() > 0 }
            ?.get(0)?.asJsonObject
            ?: throw IllegalArgumentException("No chart data found for $symbol")

        val meta = chartResult.getAsJsonObject("meta")
        val timestamps = chartResult.getAsJsonArray("timestamp")
        val closes = chartResult
            .getAsJsonObject("indicators")
            .getAsJsonArray("quote")
            ?.get(0)?.asJsonObject
            ?.getAsJsonArray("close")

        val priceHistory = mutableListOf<PricePoint>()
        if (timestamps != null && closes != null) {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            for (i in 0 until minOf(timestamps.size(), closes.size())) {
                val ts = timestamps[i]?.asLong ?: continue
                val close = closes[i]?.takeIf { !it.isJsonNull }?.asDouble ?: continue
                priceHistory.add(PricePoint(sdf.format(Date(ts * 1000L)), close))
            }
        }

        val currentPrice = meta.get("regularMarketPrice")?.asDouble
            ?: meta.get("chartPreviousClose")?.asDouble
            ?: 0.0
        val high52 = meta.get("fiftyTwoWeekHigh")?.asDouble ?: 0.0
        val low52 = meta.get("fiftyTwoWeekLow")?.asDouble ?: 0.0

        var peRatio: Double? = null
        var marketCap: Long? = null
        var volume: Long = 0L

        if (quoteJson != null) {
            try {
                val quoteResult = quoteJson
                    .getAsJsonObject("quoteResponse")
                    .getAsJsonArray("result")
                    ?.takeIf { it.size() > 0 }
                    ?.get(0)?.asJsonObject

                if (quoteResult != null) {
                    peRatio = quoteResult.get("trailingPE")?.takeIf { !it.isJsonNull }?.asDouble
                    marketCap = quoteResult.get("marketCap")?.takeIf { !it.isJsonNull }?.asLong
                    volume = quoteResult.get("regularMarketVolume")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
                }
            } catch (_: Exception) { /* supplementary — ignore on failure */ }
        }

        return StockData(
            symbol = symbol.uppercase(),
            currentPrice = currentPrice,
            fiftyTwoWeekHigh = high52,
            fiftyTwoWeekLow = low52,
            peRatio = peRatio,
            volume = volume,
            marketCap = marketCap,
            priceHistory = priceHistory
        )
    }
}
