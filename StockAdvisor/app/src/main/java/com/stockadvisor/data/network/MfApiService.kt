package com.stockadvisor.data.network

import com.stockadvisor.data.model.PricePoint
import com.stockadvisor.data.model.StockData
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fetches Indian mutual fund NAV data from mfapi.in — a free, no-auth AMFI data source.
 * Used as a fallback when Yahoo Finance cannot provide data for mutual fund queries.
 */
class MfApiService(private val client: OkHttpClient) {

    fun isMutualFundQuery(query: String): Boolean {
        val lower = query.lowercase()
        return listOf("fund", "scheme", "direct", "regular", "growth", "dividend",
            "balanced", "hybrid", "liquid", "debt", "equity scheme", "plan",
            "idcw", "reinvest", "nippon", "icici pru", "hdfc mf", "sbi mf",
            "axis mf", "kotak mf", "mirae", "parag parikh", "quant", "pgim",
            "aditya birla", "uti mf", "tata mf", "franklin").any { it in lower }
    }

    @Throws(IOException::class)
    fun fetchMutualFundData(query: String): StockData? {
        val schemeCode = searchScheme(query) ?: return null
        return fetchNavData(schemeCode)
    }

    private fun searchScheme(query: String): Int? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.mfapi.in/mf/search?q=$encoded"
            val response = client.newCall(buildRequest(url)).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null
            val arr = JsonParser.parseString(body).asJsonArray
            if (arr.size() == 0) return null
            arr.get(0).asJsonObject.get("schemeCode")?.asInt
        } catch (_: Exception) { null }
    }

    private fun fetchNavData(schemeCode: Int): StockData? {
        return try {
            val url = "https://api.mfapi.in/mf/$schemeCode"
            val response = client.newCall(buildRequest(url)).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null

            val root = JsonParser.parseString(body).asJsonObject
            if (root.get("status")?.asString != "SUCCESS") return null

            val meta = root.getAsJsonObject("meta")
            val schemeName = meta.get("scheme_name")?.asString ?: "Unknown Fund"
            val fundHouse = meta.get("fund_house")?.asString ?: ""
            val schemeCategory = meta.get("scheme_category")?.asString ?: "Mutual Fund"
            val dataArr = root.getAsJsonArray("data")

            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val outSdf = SimpleDateFormat("yyyy-MM", Locale.US)

            val navPoints = mutableListOf<PricePoint>()
            // Data is newest-first; we want oldest-first for the chart
            for (i in dataArr.size() - 1 downTo 0) {
                val entry = dataArr.get(i).asJsonObject
                val dateStr = entry.get("date")?.asString ?: continue
                val nav = entry.get("nav")?.asString?.toDoubleOrNull() ?: continue
                try {
                    val date = sdf.parse(dateStr) ?: continue
                    navPoints.add(PricePoint(outSdf.format(date), nav))
                } catch (_: Exception) { }
            }

            // Subsample to monthly (keep last entry per yyyy-MM)
            val monthly = navPoints
                .groupBy { it.date }
                .map { (month, points) -> PricePoint(month, points.last().closePrice) }
                .sortedBy { it.date }
                .takeLast(60)  // max 5 years

            val currentNav = monthly.lastOrNull()?.closePrice ?: 0.0
            val high52 = monthly.takeLast(12).maxOfOrNull { it.closePrice } ?: currentNav
            val low52 = monthly.takeLast(12).minOfOrNull { it.closePrice } ?: currentNav

            StockData(
                symbol = "MF:$schemeCode",
                name = schemeName,
                assetType = schemeCategory,
                currentPrice = currentNav,
                fiftyTwoWeekHigh = high52,
                fiftyTwoWeekLow = low52,
                peRatio = null,
                volume = 0L,
                marketCap = null,
                priceHistory = monthly
            )
        } catch (_: Exception) { null }
    }

    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (compatible; StockAdvisor/1.0)")
        .build()
}
