package com.stockadvisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockadvisor.data.model.StockAnalysis
import com.stockadvisor.data.model.StockData
import com.stockadvisor.data.model.Verdict
import com.stockadvisor.ui.theme.*
import com.stockadvisor.viewmodel.AnalysisState
import com.stockadvisor.viewmodel.StockViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Parsed text model (only the AI-written sections) ────────────────────────

private data class ParsedAnalysis(
    val verdict: String = "",
    val confidence: String = "",
    val instrument: String = "",
    val summary: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val adviceBuyToday: String = "",
    val adviceLongTerm: String = "",
    val adviceTrader: String = "",
    val dataSource: String = ""
)

// ─── Screen entry point ───────────────────────────────────────────────────────

@Composable
fun ResultScreen(
    symbol: String,
    decision: String,
    viewModel: StockViewModel,
    onAnalyzeAnother: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        when (val s = state) {
            is AnalysisState.Idle -> {
                LaunchedEffect(Unit) { viewModel.analyzeStock(symbol, decision) }
                LoadingContent("Initializing…")
            }
            is AnalysisState.LoadingMarketData -> LoadingContent("Fetching market data…")
            is AnalysisState.LoadingAIAnalysis ->
                LoadingContent("Running AI research…\nThis may take 15–30 seconds.")
            is AnalysisState.Success -> SuccessContent(s.analysis, onAnalyzeAnother)
            is AnalysisState.Error -> ErrorContent(s.message, onAnalyzeAnother) {
                viewModel.analyzeStock(symbol, decision)
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = GreenAccent,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// ─── Success ──────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    analysis: StockAnalysis,
    onAnalyzeAnother: () -> Unit
) {
    val parsed = remember(analysis.analysisText) { parseStructuredAnalysis(analysis.analysisText) }
    val metricsRows = remember(analysis.stockData) { buildMetricsRows(analysis.stockData) }
    val positionRows = remember(analysis.stockData, analysis.avgBuyPrice, analysis.quantity) {
        buildPositionRows(analysis.stockData, analysis.avgBuyPrice, analysis.quantity)
    }

    val verdict = analysis.verdict
    val verdictColor = when (verdict) {
        Verdict.WISE -> WiseColor
        Verdict.RISKY -> RiskyColor
        Verdict.NEUTRAL -> NeutralColor
    }
    val verdictIcon = when (verdict) {
        Verdict.WISE -> Icons.Default.CheckCircle
        Verdict.RISKY -> Icons.Default.Warning
        Verdict.NEUTRAL -> Icons.Default.Info
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Verdict banner ────────────────────────────────────────────────────
        Surface(
            color = verdictColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val instrumentLabel = parsed.instrument.split("|").firstOrNull()?.trim()
                        ?: parsed.instrument
                    Text(
                        text = instrumentLabel.ifBlank { analysis.symbol },
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (parsed.confidence.isNotBlank()) {
                        Text(
                            text = "Confidence: ${parsed.confidence}",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = verdictColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = verdictIcon,
                            contentDescription = verdict.name,
                            tint = verdictColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = verdict.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = verdictColor
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Summary bullets ───────────────────────────────────────────────
            if (parsed.summary.isNotEmpty()) {
                SectionHeader("SUMMARY")
                Spacer(modifier = Modifier.height(6.dp))
                BulletCard(items = parsed.summary, bulletColor = GoldAccent, bullet = "•")
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Metrics table (always built from StockData) ───────────────────
            if (metricsRows.isNotEmpty()) {
                SectionHeader("METRICS")
                Spacer(modifier = Modifier.height(6.dp))
                MetricsTable(rows = metricsRows)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Position table (SELL / HOLD with avgBuyPrice) ─────────────────
            if (positionRows.isNotEmpty()) {
                SectionHeader("YOUR POSITION")
                Spacer(modifier = Modifier.height(6.dp))
                MetricsTable(rows = positionRows)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Strengths / Risks in two columns ─────────────────────────────
            if (parsed.strengths.isNotEmpty() || parsed.risks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (parsed.strengths.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("STRENGTHS")
                            Spacer(modifier = Modifier.height(6.dp))
                            BulletCard(
                                items = parsed.strengths,
                                bulletColor = WiseColor,
                                bullet = "✓",
                                cardColor = WiseColor.copy(alpha = 0.08f)
                            )
                        }
                    }
                    if (parsed.risks.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("RISKS")
                            Spacer(modifier = Modifier.height(6.dp))
                            BulletCard(
                                items = parsed.risks,
                                bulletColor = RiskyColor,
                                bullet = "✗",
                                cardColor = RiskyColor.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Three-persona expert advice ───────────────────────────────────
            if (parsed.adviceBuyToday.isNotBlank() || parsed.adviceLongTerm.isNotBlank() || parsed.adviceTrader.isNotBlank()) {
                SectionHeader("EXPERT ADVICE")
                Spacer(modifier = Modifier.height(8.dp))
                if (parsed.adviceBuyToday.isNotBlank()) {
                    AdviceCard(
                        emoji = "🎯",
                        title = "Buying Today",
                        body = parsed.adviceBuyToday,
                        accentColor = RiskyColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (parsed.adviceLongTerm.isNotBlank()) {
                    AdviceCard(
                        emoji = "🌱",
                        title = "Long Term / SIP",
                        body = parsed.adviceLongTerm,
                        accentColor = WiseColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (parsed.adviceTrader.isNotBlank()) {
                    AdviceCard(
                        emoji = "⚡",
                        title = "Short-Term Trader",
                        body = parsed.adviceTrader,
                        accentColor = AmberAccent
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Data source + disclaimer ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (parsed.dataSource.isNotBlank()) {
                        Text(
                            text = "Source: ${parsed.dataSource}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Text(
                        text = "For educational purposes only. This is not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAnalyzeAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = OnPrimary
                )
            ) {
                Text("Analyze Another", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Metrics builder — always computed from StockData ────────────────────────

private fun buildMetricsRows(stockData: StockData): List<Triple<String, String, String>> {
    val currency = if ("NS" in stockData.symbol || "BO" in stockData.symbol ||
        stockData.symbol.startsWith("MF:")) "₹" else "$"
    val isMf = stockData.assetType.contains("FUND", ignoreCase = true) || stockData.symbol.startsWith("MF:")
    val priceLabel = if (isMf) "NAV" else "Price"

    fun fmt2(v: Double) = String.format("%.2f", v)
    fun fmt1(v: Double) = String.format("%.1f", v)

    val prices = stockData.priceHistory
    val startPrice = prices.firstOrNull()?.closePrice ?: stockData.currentPrice
    val pctChange = if (startPrice > 0)
        ((stockData.currentPrice - startPrice) / startPrice * 100).roundToInt() else 0

    val maxDrawdown: Double = run {
        if (prices.isEmpty()) return@run 0.0
        var peak = prices.first().closePrice
        var maxDD = 0.0
        for (p in prices) {
            if (p.closePrice > peak) peak = p.closePrice
            val dd = if (peak > 0) (peak - p.closePrice) / peak * 100 else 0.0
            if (dd > maxDD) maxDD = dd
        }
        maxDD
    }

    val n = prices.size
    val recentTrend3M = if (n >= 4) {
        val p3m = prices[n - 4].closePrice
        val pNow = prices.last().closePrice
        if (p3m > 0) ((pNow - p3m) / p3m * 100).roundToInt() else 0
    } else 0

    val vsHigh = if (stockData.fiftyTwoWeekHigh > 0)
        ((stockData.currentPrice - stockData.fiftyTwoWeekHigh) / stockData.fiftyTwoWeekHigh * 100).roundToInt() else 0
    val vsLow = if (stockData.fiftyTwoWeekLow > 0)
        ((stockData.currentPrice - stockData.fiftyTwoWeekLow) / stockData.fiftyTwoWeekLow * 100).roundToInt() else 100

    val trendLabel = when {
        recentTrend3M >= 10  -> "↑ Uptrend (+$recentTrend3M%)"
        recentTrend3M <= -10 -> "↓ Downtrend ($recentTrend3M%)"
        else                 -> "→ Sideways ($recentTrend3M%)"
    }
    val trendSignal = when {
        recentTrend3M >= 10  -> "↑ BULLISH"
        recentTrend3M <= -10 -> "↓ BEARISH"
        else                 -> "→ NEUTRAL"
    }

    val rows = mutableListOf<Triple<String, String, String>>()
    rows += Triple("Current $priceLabel", "$currency${fmt2(stockData.currentPrice)}", "-")
    rows += Triple("52W High", "$currency${fmt2(stockData.fiftyTwoWeekHigh)}", "-")
    rows += Triple("52W Low",  "$currency${fmt2(stockData.fiftyTwoWeekLow)}", "-")
    rows += Triple("vs 52W High", "$vsHigh%",
        if (vsHigh > -5) "↑ GOOD" else "↓ CAUTION")
    rows += Triple("vs 52W Low", "+$vsLow%", when {
        vsLow < 10 -> "↓ AT BOTTOM"
        vsLow < 30 -> "→ RECOVERING"
        else       -> "↑ ABOVE"
    })
    rows += Triple("3M Trend", trendLabel, trendSignal)
    rows += Triple("5Y Return", "$pctChange%", when {
        pctChange > 50 -> "↑ STRONG"
        pctChange > 10 -> "→ MODERATE"
        else           -> "↓ WEAK"
    })
    rows += Triple("Max Drawdown", "${maxDrawdown.roundToInt()}%", when {
        maxDrawdown > 40 -> "↓ HIGH RISK"
        maxDrawdown > 20 -> "→ MODERATE"
        else             -> "↑ LOW"
    })

    stockData.peRatio?.let { pe ->
        rows += Triple("P/E (TTM)", fmt1(pe), when {
            pe > 80 -> "↓ EXPENSIVE"
            pe < 20 -> "↑ CHEAP"
            else    -> "→ FAIR"
        })
    }
    stockData.pbRatio?.let { pb ->
        rows += Triple("P/B Ratio", fmt1(pb), when {
            pb > 5  -> "↓ PRICEY"
            pb < 1  -> "↑ CHEAP"
            else    -> "→ FAIR"
        })
    }
    stockData.eps?.let { eps ->
        rows += Triple("EPS (TTM)", "$currency${fmt2(eps)}", "-")
    }
    stockData.roe?.let { roe ->
        rows += Triple("ROE", "${fmt1(roe)}%", when {
            roe > 15 -> "↑ GOOD"
            roe > 8  -> "→ OK"
            else     -> "↓ POOR"
        })
    }
    stockData.debtToEquity?.let { de ->
        rows += Triple("Debt/Equity", fmt1(de), when {
            de < 0.5 -> "↑ LOW RISK"
            de < 1.5 -> "→ MODERATE"
            else     -> "↓ HIGH"
        })
    }
    stockData.dividendYield?.let { dy ->
        rows += Triple("Div Yield", "${fmt1(dy)}%", "-")
    }
    stockData.revenueGrowth?.let { rg ->
        val sign = if (rg >= 0) "+" else ""
        rows += Triple("Revenue Growth", "$sign${fmt1(rg)}% YoY",
            if (rg > 0) "↑ GROWING" else "↓ SHRINKING")
    }
    stockData.marketCap?.let { mc ->
        val cap = when {
            mc >= 1_000_000_000_000L -> "$currency${"%.2f".format(mc / 1_000_000_000_000.0)}T"
            mc >= 1_000_000_000L     -> "$currency${"%.2f".format(mc / 1_000_000_000.0)}B"
            mc >= 1_000_000L         -> "$currency${"%.2f".format(mc / 1_000_000.0)}M"
            else                     -> "$currency$mc"
        }
        rows += Triple("Market Cap", cap, "-")
    }

    return rows
}

private fun buildPositionRows(
    stockData: StockData,
    avgBuyPrice: Double?,
    quantity: Int?
): List<Triple<String, String, String>> {
    if (avgBuyPrice == null && quantity == null) return emptyList()
    val currency = if ("NS" in stockData.symbol || "BO" in stockData.symbol ||
        stockData.symbol.startsWith("MF:")) "₹" else "$"
    val rows = mutableListOf<Triple<String, String, String>>()
    if (avgBuyPrice != null) {
        rows += Triple("Avg Buy Price", "$currency${String.format("%.2f", avgBuyPrice)}", "-")
    }
    if (avgBuyPrice != null && quantity != null) {
        val pnl = (stockData.currentPrice - avgBuyPrice) * quantity
        val pnlPct = if (avgBuyPrice > 0) (stockData.currentPrice - avgBuyPrice) / avgBuyPrice * 100 else 0.0
        val sign = if (pnl >= 0) "+" else ""
        val pctSign = if (pnlPct >= 0) "+" else ""
        rows += Triple(
            "Unrealized P&L",
            "$sign$currency${String.format("%.0f", abs(pnl))}",
            if (pnl >= 0) "↑ PROFIT" else "↓ LOSS"
        )
        rows += Triple(
            "P&L %",
            "$pctSign${String.format("%.1f", pnlPct)}%",
            if (pnlPct >= 0) "↑ PROFIT" else "↓ LOSS"
        )
    }
    return rows
}

// ─── Composable helpers ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = GoldAccent,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun MetricsTable(rows: List<Triple<String, String, String>>) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            rows.forEachIndexed { index, (label, value, signal) ->
                if (index > 0) HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (signal.isNotBlank() && signal != "-") {
                        Text(
                            text = signal,
                            style = MaterialTheme.typography.labelSmall,
                            color = signalColor(signal),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.3f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdviceCard(
    emoji: String,
    title: String,
    body: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(emoji, fontSize = 20.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BulletCard(
    items: List<String>,
    bulletColor: Color,
    bullet: String,
    cardColor: Color = Surface
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = bullet,
                        color = bulletColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface
                    )
                }
            }
        }
    }
}

// ─── Signal colour ────────────────────────────────────────────────────────────

private fun signalColor(signal: String): Color {
    val s = signal.uppercase()
    return when {
        "↑" in s || "WISE" in s || "GOOD" in s || "PROFIT" in s || "GROWING" in s ||
            "LOW RISK" in s || "BULLISH" in s || "CHEAP" in s -> WiseColor
        "↓" in s || "RISKY" in s || "CAUTION" in s || "LOSS" in s || "HIGH RISK" in s ||
            "WEAK" in s || "BEARISH" in s || "EXPENSIVE" in s || "PRICEY" in s ||
            "SHRINKING" in s || "POOR" in s -> RiskyColor
        else -> NeutralColor
    }
}

// ─── Parser — only parses AI-written text sections ───────────────────────────

private fun parseStructuredAnalysis(text: String): ParsedAnalysis {
    val lines = text.lines()
    var verdict = ""; var confidence = ""; var instrument = ""
    val summary = mutableListOf<String>()
    val strengths = mutableListOf<String>()
    val risks = mutableListOf<String>()
    var dataSource = ""
    var section = ""

    for (line in lines) {
        val trimmed = line.trim()
        // Strip markdown bold/italic so **HEADER:** is treated the same as HEADER:
        val hdr = trimmed.replace("**", "").replace("*", "").replace("_", "").trim().uppercase()

        when {
            hdr.startsWith("VERDICT:") -> { verdict = hdr.removePrefix("VERDICT:").trim(); section = "" }
            hdr.startsWith("CONFIDENCE:") -> { confidence = hdr.removePrefix("CONFIDENCE:").trim(); section = "" }
            hdr.startsWith("INSTRUMENT:") -> {
                instrument = trimmed.replace("**", "").replace("*", "").replace("_", "").trim()
                    .removePrefix("INSTRUMENT:").removePrefix("instrument:").trim()
                section = ""
            }
            hdr.startsWith("SUMMARY") -> section = "SUMMARY"
            hdr.startsWith("STRENGTHS") -> section = "STRENGTHS"
            hdr.startsWith("RISKS") -> section = "RISKS"
            // Expert advice section — handled separately via text-search below
            hdr.startsWith("EXPERT ADVICE") || hdr.startsWith("FOR BUY") ||
            hdr.startsWith("FOR LONG") || hdr.startsWith("FOR TRAD") ||
            hdr.startsWith("FOR SHORT") || hdr.startsWith("ADVICE FOR") -> section = ""
            hdr.startsWith("DATA:") -> { dataSource = hdr.removePrefix("DATA:").trim(); section = "" }
            hdr.startsWith("DISCLAIMER:") -> section = ""
            trimmed.isBlank() -> { /* keep current section */ }
            else -> when (section) {
                "SUMMARY" -> {
                    val t = trimmed.trimStart('•', '-', ' ')
                    if (t.isNotBlank()) summary.add(t)
                }
                "STRENGTHS" -> {
                    if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                        val t = trimmed.trimStart('•', '-', ' ')
                        if (t.isNotBlank()) strengths.add(t)
                    }
                }
                "RISKS" -> {
                    if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                        val t = trimmed.trimStart('•', '-', ' ')
                        if (t.isNotBlank()) risks.add(t)
                    }
                }
            }
        }
    }

    // Advice sections use text-search which is immune to markdown headers and phrasing variants
    val adviceBuyToday = extractAdviceSection(
        text = text,
        startKeywords = listOf("FOR BUYERS", "FOR BUYER", "FOR BUYING", "ADVICE FOR BUYER"),
        endKeywords = listOf("FOR LONG", "FOR SIP", "FOR TRAD", "FOR SHORT", "DATA:", "DISCLAIMER:")
    )
    val adviceLongTerm = extractAdviceSection(
        text = text,
        startKeywords = listOf("FOR LONG TERM", "FOR LONG-TERM", "FOR SIP", "FOR LONG", "ADVICE FOR LONG"),
        endKeywords = listOf("FOR BUY", "FOR TRAD", "FOR SHORT", "DATA:", "DISCLAIMER:")
    )
    val adviceTrader = extractAdviceSection(
        text = text,
        startKeywords = listOf("FOR TRADERS", "FOR TRADER", "FOR SHORT-TERM TRADER", "FOR SHORT", "ADVICE FOR SHORT"),
        endKeywords = listOf("FOR BUY", "FOR LONG", "FOR SIP", "DATA:", "DISCLAIMER:")
    )

    return ParsedAnalysis(
        verdict = verdict,
        confidence = confidence,
        instrument = instrument,
        summary = summary,
        strengths = strengths,
        risks = risks,
        adviceBuyToday = adviceBuyToday,
        adviceLongTerm = adviceLongTerm,
        adviceTrader = adviceTrader,
        dataSource = dataSource
    )
}

/**
 * Finds the first line starting with any of [startKeywords] (after stripping markdown),
 * then collects subsequent lines as body until an [endKeywords] line is encountered.
 * Handles inline content after the colon on the header line.
 */
private fun extractAdviceSection(
    text: String,
    startKeywords: List<String>,
    endKeywords: List<String>
): String {
    val textLines = text.lines()
    var startLineIdx = -1

    for (i in textLines.indices) {
        val clean = textLines[i].replace("**", "").replace("*", "").replace("_", "").trim()
        val upper = clean.uppercase()
        if (startKeywords.any { upper.startsWith(it) }) {
            startLineIdx = i
            break
        }
    }
    if (startLineIdx < 0) return ""

    // Capture any text after the colon on the header line itself
    val headerClean = textLines[startLineIdx].replace("**", "").replace("*", "").replace("_", "").trim()
    val colonIdx = headerClean.indexOf(':')
    val inlineContent = if (colonIdx in 0 until headerClean.lastIndex)
        headerClean.substring(colonIdx + 1).trim() else ""

    // Collect body lines until an end-keyword line is found
    val bodyLines = mutableListOf<String>()
    for (i in (startLineIdx + 1) until textLines.size) {
        val clean = textLines[i].replace("**", "").replace("*", "").replace("_", "").trim()
        val upper = clean.uppercase()
        if (endKeywords.any { upper.startsWith(it) }) break
        if (clean.isNotBlank()) bodyLines.add(clean)
    }

    val body = bodyLines.joinToString(" ")
    return listOf(inlineContent, body).filter { it.isNotBlank() }.joinToString(" ").trim()
}

// ─── Error ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onAnalyzeAnother: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = RedAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analysis Failed",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = OnPrimary)
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAnalyzeAnother,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
        ) {
            Text("Search New Stock")
        }
    }
}
