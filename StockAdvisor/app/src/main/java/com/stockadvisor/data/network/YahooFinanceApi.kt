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
        // Resolve the actual Yahoo Finance symbol (may auto-append .NS for Indian stocks)
        val resolvedSymbol = resolveSymbol(symbol)
        val chartData = fetchChartData(resolvedSymbol)
        val quoteData = fetchQuoteData(resolvedSymbol)
        return mergeData(resolvedSymbol, chartData, quoteData)
    }

    /**
     * Yahoo Finance requires exchange suffixes for non-US stocks.
     * Indian stocks: RELIANCE.NS (NSE), RELIANCE.BO (BSE)
     * Indices: ^NSEI (Nifty50), ^BSESN (Sensex), ^GSPC (S&P500)
     * US stocks/ETFs: AAPL, VOO — no suffix needed
     *
     * Strategy: if the symbol has no dot or caret, try as-is first.
     * On 404, retry with .NS suffix (catches most Indian NSE stocks).
     */
    private fun resolveSymbol(symbol: String): String {
        // Already has exchange suffix or is an index — use directly
        if ('.' in symbol || symbol.startsWith("^")) return symbol

        // Try the bare symbol first (works for US stocks)
        val bareOk = try { checkSymbolExists(symbol) } catch (_: IOException) { false }
        if (bareOk) return symbol

        // Try NSE suffix (most Indian stocks)
        val nsSymbol = "$symbol.NS"
        val nsOk = try { checkSymbolExists(nsSymbol) } catch (_: IOException) { false }
        if (nsOk) return nsSymbol

        // Try BSE suffix as last resort
        val boSymbol = "$symbol.BO"
        val boOk = try { checkSymbolExists(boSymbol) } catch (_: IOException) { false }
        if (boOk) return boSymbol

        // Fall through — fetchChartData will return a proper error
        return symbol
    }

    private fun checkSymbolExists(symbol: String): Boolean {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1mo&range=1mo"
        val request = buildRequest(url)
        val response = client.newCall(request).execute()
        response.body?.string() // consume body
        return response.isSuccessful
    }

    private fun fetchChartData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1mo&range=5y"
        val response = client.newCall(buildRequest(url)).execute()
        val body = response.body?.string() ?: throw IOException("Empty chart response")
        if (!response.isSuccessful) {
            val hint = if (response.code == 404)
                "Symbol '$symbol' not found. For Indian stocks use format: RELIANCE.NS or TCS.NS"
            else
                "Yahoo Finance error ${response.code} for '$symbol'"
            throw IOException(hint)
        }
        return JsonParser.parseString(body).asJsonObject
    }

    private fun fetchQuoteData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
        val response = client.newCall(buildRequest(url)).execute()
        val body = response.body?.string() ?: throw IOException("Empty quote response")
        if (!response.isSuccessful) throw IOException("Yahoo Finance quote error ${response.code}")
        return JsonParser.parseString(body).asJsonObject
    }

    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Accept", "application/json,text/plain,*/*")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Origin", "https://finance.yahoo.com")
        .header("Referer", "https://finance.yahoo.com/")
        .build()

    private fun mergeData(symbol: String, chartJson: JsonObject, quoteJson: JsonObject): StockData {
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
        } catch (_: Exception) { /* quote supplementary — ignore on failure */ }

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
