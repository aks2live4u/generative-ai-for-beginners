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
        val chartData = fetchChartData(symbol)
        val quoteData = fetchQuoteData(symbol)
        return mergeData(symbol, chartData, quoteData)
    }

    private fun fetchChartData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1mo&range=5y"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; StockAdvisor/1.0)")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty chart response")
        if (!response.isSuccessful) throw IOException("Chart API error: ${response.code}")
        return JsonParser.parseString(body).asJsonObject
    }

    private fun fetchQuoteData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; StockAdvisor/1.0)")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty quote response")
        if (!response.isSuccessful) throw IOException("Quote API error: ${response.code}")
        return JsonParser.parseString(body).asJsonObject
    }

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
