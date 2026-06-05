package com.stockadvisor.data.model

data class PricePoint(
    val date: String,
    val closePrice: Double
)

data class StockData(
    val symbol: String,
    val name: String = "",
    val assetType: String = "EQUITY",
    val currentPrice: Double,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val peRatio: Double?,
    val pbRatio: Double? = null,
    val eps: Double? = null,
    val debtToEquity: Double? = null,
    val dividendYield: Double? = null,
    val roe: Double? = null,
    val revenueGrowth: Double? = null,
    val earningsGrowth: Double? = null,
    val volume: Long,
    val marketCap: Long?,
    val priceHistory: List<PricePoint>
)
