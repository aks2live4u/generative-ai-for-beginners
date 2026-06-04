package com.stockadvisor.data.model

enum class Verdict { WISE, RISKY, NEUTRAL }

data class StockAnalysis(
    val symbol: String,
    val decision: String,
    val analysisText: String,
    val verdict: Verdict
)
