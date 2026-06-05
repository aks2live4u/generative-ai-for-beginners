package com.stockadvisor.data.network

import com.stockadvisor.data.model.PricePoint
import com.stockadvisor.data.model.StockData
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class YahooFinanceApi(private val client: OkHttpClient) {

    @Throws(IOException::class)
    fun fetchStockData(symbol: String): StockData {
        // Strip market-prefix encoding before resolving (prefix is only routing metadata)
        val cleanInput = symbol.trim()
        val (resolvedSymbol, chartData) = resolveAndFetchChart(cleanInput)
        val quoteData: JsonObject? = try { fetchQuoteData(resolvedSymbol) } catch (_: Exception) { null }
        return mergeData(resolvedSymbol, chartData, quoteData)
    }

    /**
     * Resolution strategy:
     *  IN:<sym>  → India mode  : .NS → .BO → search (NSE/BSE only)
     *  US:<sym>  → US/Global   : bare → search (skip Indian exchanges)
     *  <sym>     → Auto        : .NS → .BO → bare → search
     *  <sym.XX>  → Explicit    : try directly → search same exchange on rename
     *  ^<sym>    → Index       : try directly
     */
    private fun resolveAndFetchChart(input: String): Pair<String, JsonObject> {

        // ── India mode ─────────────────────────────────────────────────────
        if (input.startsWith("IN:")) {
            val sym = input.removePrefix("IN:")
            for (candidate in listOf("$sym.NS", "$sym.BO")) {
                tryChartData(candidate)?.let { return candidate to it }
            }
            // Renamed ticker (e.g. ZOMATO.NS → ETERNAL.NS) — search NSE/BSE only
            searchForSymbol(sym, indiaOnly = true)?.let { found ->
                tryChartData(found)?.let { return found to it }
            }
            throw IllegalArgumentException(
                "'$sym' not found on NSE or BSE.\nTry the exact NSE ticker (e.g. ETERNAL for Zomato)."
            )
        }

        // ── US / Global mode ───────────────────────────────────────────────
        if (input.startsWith("US:")) {
            val sym = input.removePrefix("US:")
            tryChartData(sym)?.let { return sym to it }
            searchForSymbol(sym, indiaOnly = false, globalOnly = true)?.let { found ->
                tryChartData(found)?.let { return found to it }
            }
            throw IllegalArgumentException("'$sym' not found on US/global markets.")
        }

        // ── Explicit suffix (.NS, .BO) or index (^) ────────────────────────
        if ('.' in input || input.startsWith("^")) {
            tryChartData(input)?.let { return input to it }
            // Handle renamed tickers — search constrained to same exchange
            val suffix = when {
                input.endsWith(".NS") -> ".NS"
                input.endsWith(".BO") -> ".BO"
                else -> null
            }
            if (suffix != null) {
                val base = input.substringBeforeLast(".")
                searchForSymbol(base, indiaOnly = true)?.let { found ->
                    if (found.endsWith(suffix)) tryChartData(found)?.let { return found to it }
                }
            }
            throw IllegalArgumentException("Could not find '$input'.")
        }

        // ── Auto mode — prefer Indian exchanges ────────────────────────────
        // so "ZOMATO" / "ITC" / "RELIANCE" resolve to NSE/BSE before bare
        for (candidate in listOf("$input.NS", "$input.BO", input)) {
            tryChartData(candidate)?.let { return candidate to it }
        }

        // Search fallback (handles renamed tickers, company names, ETF names)
        searchForSymbol(input)?.let { found ->
            tryChartData(found)?.let { return found to it }
        }

        throw IllegalArgumentException(
            "Could not find '$input'.\n" +
            "Try selecting a specific market (India / US) or use the exact ticker with suffix (e.g. ETERNAL.NS)."
        )
    }

    private fun tryChartData(symbol: String): JsonObject? {
        return try {
            val json = fetchChartData(symbol)
            val hasData = json.getAsJsonObject("chart")
                ?.getAsJsonArray("result")
                ?.let { it.size() > 0 } == true
            if (hasData) json else null
        } catch (e: IOException) {
            val msg = e.message ?: ""
            if ("401" in msg || "403" in msg) throw e
            null
        }
    }

    private fun searchForSymbol(
        query: String,
        indiaOnly: Boolean = false,
        globalOnly: Boolean = false
    ): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v1/finance/search" +
                    "?q=$encoded&quotesCount=10&newsCount=0&enableFuzzyQuery=true"
            val response = client.newCall(buildRequest(url)).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null

            val quotes = JsonParser.parseString(body).asJsonObject
                .getAsJsonArray("quotes") ?: return null

            val validTypes = listOf("EQUITY", "ETF", "MUTUALFUND", "INDEX")

            when {
                indiaOnly -> {
                    // Only return NSE (.NS) or BSE (.BO) results
                    for (quote in quotes) {
                        val obj = quote.asJsonObject
                        val sym = obj.get("symbol")?.takeIf { !it.isJsonNull }?.asString ?: continue
                        val type = obj.get("quoteType")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if ((sym.endsWith(".NS") || sym.endsWith(".BO")) && type in validTypes) return sym
                    }
                    null
                }
                globalOnly -> {
                    // Prefer non-Indian results; skip .NS and .BO
                    for (quote in quotes) {
                        val obj = quote.asJsonObject
                        val sym = obj.get("symbol")?.takeIf { !it.isJsonNull }?.asString ?: continue
                        val type = obj.get("quoteType")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if (!sym.endsWith(".NS") && !sym.endsWith(".BO") && type in validTypes) return sym
                    }
                    quotes.firstOrNull()?.asJsonObject?.get("symbol")?.takeIf { !it.isJsonNull }?.asString
                }
                else -> {
                    // Auto: prefer Indian exchanges first, then global
                    for (quote in quotes) {
                        val obj = quote.asJsonObject
                        val sym = obj.get("symbol")?.takeIf { !it.isJsonNull }?.asString ?: continue
                        val type = obj.get("quoteType")?.takeIf { !it.isJsonNull }?.asString ?: ""
                        if ((sym.endsWith(".NS") || sym.endsWith(".BO")) && type in validTypes) return sym
                    }
                    quotes.firstOrNull()?.asJsonObject?.get("symbol")?.takeIf { !it.isJsonNull }?.asString
                }
            }
        } catch (_: Exception) { null }
    }

    private fun fetchChartData(symbol: String): JsonObject {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1mo&range=5y"
        val response = client.newCall(buildRequest(url)).execute()
        val body = response.body?.string() ?: throw IOException("Empty chart response for $symbol")
        if (!response.isSuccessful) throw IOException("Yahoo Finance chart error ${response.code} for $symbol")
        return JsonParser.parseString(body).asJsonObject
    }

    private fun fetchQuoteData(symbol: String): JsonObject {
        for (host in listOf("query2", "query1")) {
            try {
                val url = "https://$host.finance.yahoo.com/v7/finance/quote?symbols=$symbol"
                val response = client.newCall(buildRequest(url)).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) return JsonParser.parseString(body).asJsonObject
            } catch (_: Exception) { }
        }
        throw IOException("Quote data unavailable")
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
            ?: throw IOException("No chart data returned for $symbol")

        val meta = chartResult.getAsJsonObject("meta")

        // takeIf { !it.isJsonNull } guards against JSON null values that Gson represents
        // as JsonNull objects (non-null in Kotlin) — calling .asString on them throws.
        val name = meta.get("longName")?.takeIf { !it.isJsonNull }?.asString
            ?: meta.get("shortName")?.takeIf { !it.isJsonNull }?.asString
            ?: symbol

        val instrumentType = meta.get("instrumentType")?.takeIf { !it.isJsonNull }?.asString
            ?: meta.get("quoteType")?.takeIf { !it.isJsonNull }?.asString
            ?: "EQUITY"

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
        val currency = meta.get("currency")?.asString ?: "USD"

        var peRatio: Double? = null
        var pbRatio: Double? = null
        var eps: Double? = null
        var debtToEquity: Double? = null
        var dividendYield: Double? = null
        var roe: Double? = null
        var revenueGrowth: Double? = null
        var earningsGrowth: Double? = null
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
                    fun d(key: String) = quoteResult.get(key)?.takeIf { !it.isJsonNull }?.asDouble
                    peRatio       = d("trailingPE")
                    pbRatio       = d("priceToBook")
                    eps           = d("trailingEps")
                    debtToEquity  = d("debtToEquity")
                    dividendYield = d("dividendYield")?.let { it * 100 }  // convert 0.02 → 2.0%
                    roe           = d("returnOnEquity")?.let { it * 100 } // convert 0.15 → 15.0%
                    revenueGrowth = d("revenueGrowth")?.let { it * 100 }
                    earningsGrowth= d("earningsGrowth")?.let { it * 100 }
                    marketCap     = quoteResult.get("marketCap")?.takeIf { !it.isJsonNull }?.asLong
                    volume        = quoteResult.get("regularMarketVolume")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
                }
            } catch (_: Exception) { }
        }

        return StockData(
            symbol = symbol.uppercase(),
            name = name,
            assetType = instrumentType,
            currentPrice = currentPrice,
            fiftyTwoWeekHigh = high52,
            fiftyTwoWeekLow = low52,
            peRatio = peRatio,
            pbRatio = pbRatio,
            eps = eps,
            debtToEquity = debtToEquity,
            dividendYield = dividendYield,
            roe = roe,
            revenueGrowth = revenueGrowth,
            earningsGrowth = earningsGrowth,
            volume = volume,
            marketCap = marketCap,
            priceHistory = priceHistory
        )
    }
}
