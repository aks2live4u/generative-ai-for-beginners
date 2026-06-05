package com.stockadvisor.data.repository

import com.stockadvisor.data.model.StockAnalysis
import com.stockadvisor.data.model.StockData
import com.stockadvisor.data.model.Verdict
import com.stockadvisor.data.network.AnthropicService
import com.stockadvisor.data.network.MfApiService
import com.stockadvisor.data.network.YahooFinanceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockRepository(
    private val yahooFinanceApi: YahooFinanceApi,
    private val mfApiService: MfApiService,
    private val anthropicService: AnthropicService
) {
    suspend fun fetchStockData(symbol: String): StockData = withContext(Dispatchers.IO) {
        // For mutual fund queries, try mfapi.in first
        if (mfApiService.isMutualFundQuery(symbol)) {
            val mfData = try { mfApiService.fetchMutualFundData(symbol) } catch (_: Exception) { null }
            if (mfData != null) return@withContext mfData
        }
        yahooFinanceApi.fetchStockData(symbol)
    }

    suspend fun analyzeStock(
        stockData: StockData,
        decision: String,
        quantity: Int? = null,
        avgBuyPrice: Double? = null
    ): StockAnalysis = withContext(Dispatchers.IO) {
        val analysisText = anthropicService.analyzeStock(stockData, decision, quantity, avgBuyPrice)
        val verdict = extractVerdict(analysisText)
        StockAnalysis(
            symbol = stockData.name.ifBlank { stockData.symbol },
            decision = decision,
            analysisText = analysisText,
            verdict = verdict
        )
    }

    private fun extractVerdict(text: String): Verdict {
        val verdictLine = text.lines().firstOrNull {
            it.trimStart().uppercase().startsWith("VERDICT:")
        } ?: ""
        return when {
            "WISE" in verdictLine.uppercase() -> Verdict.WISE
            "RISKY" in verdictLine.uppercase() -> Verdict.RISKY
            else -> Verdict.NEUTRAL
        }
    }
}
