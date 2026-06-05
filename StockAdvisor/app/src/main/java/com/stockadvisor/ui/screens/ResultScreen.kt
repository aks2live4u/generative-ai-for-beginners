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
import com.stockadvisor.data.model.Verdict
import com.stockadvisor.ui.theme.*
import com.stockadvisor.viewmodel.AnalysisState
import com.stockadvisor.viewmodel.StockViewModel

// ─── Parsed data model ────────────────────────────────────────────────────────

private data class ParsedAnalysis(
    val verdict: String = "",
    val confidence: String = "",
    val instrument: String = "",
    val summary: List<String> = emptyList(),
    val metrics: List<Triple<String, String, String>> = emptyList(),
    val position: List<Triple<String, String, String>> = emptyList(),
    val strengths: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val recommendation: String = "",
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
            is AnalysisState.Success -> SuccessContent(s.analysis.analysisText, s.analysis.verdict, onAnalyzeAnother)
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
    analysisText: String,
    verdict: Verdict,
    onAnalyzeAnother: () -> Unit
) {
    val parsed = remember(analysisText) { parseStructuredAnalysis(analysisText) }
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
                        text = instrumentLabel.ifBlank { "Analysis" },
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

            // ── Metrics table ─────────────────────────────────────────────────
            if (parsed.metrics.isNotEmpty()) {
                SectionHeader("METRICS")
                Spacer(modifier = Modifier.height(6.dp))
                MetricsTable(rows = parsed.metrics)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Position table (SELL / HOLD) ──────────────────────────────────
            if (parsed.position.isNotEmpty()) {
                SectionHeader("YOUR POSITION")
                Spacer(modifier = Modifier.height(6.dp))
                MetricsTable(rows = parsed.position)
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

            // ── Recommendation ────────────────────────────────────────────────
            if (parsed.recommendation.isNotBlank()) {
                SectionHeader("RECOMMENDATION")
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = verdictColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = parsed.recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        modifier = Modifier.padding(14.dp)
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
                    .height(52.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = OnPrimary
                )
            ) {
                Text("Analyze Another", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
        "↑" in s || "WISE" in s || "GOOD" in s || "PROFIT" in s || "VALUE" in s || "LOW RISK" in s -> WiseColor
        "↓" in s || "RISKY" in s || "CAUTION" in s || "LOSS" in s || "HIGH RISK" in s || "WEAK" in s -> RiskyColor
        else -> NeutralColor
    }
}

// ─── Parser ───────────────────────────────────────────────────────────────────

private fun parseStructuredAnalysis(text: String): ParsedAnalysis {
    val lines = text.lines()
    var verdict = ""; var confidence = ""; var instrument = ""
    val summary = mutableListOf<String>()
    val metrics = mutableListOf<Triple<String, String, String>>()
    val position = mutableListOf<Triple<String, String, String>>()
    val strengths = mutableListOf<String>()
    val risks = mutableListOf<String>()
    val recommendation = StringBuilder()
    var dataSource = ""

    var section = ""
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("VERDICT:") -> {
                verdict = trimmed.removePrefix("VERDICT:").trim()
                section = ""
            }
            trimmed.startsWith("CONFIDENCE:") -> {
                confidence = trimmed.removePrefix("CONFIDENCE:").trim()
                section = ""
            }
            trimmed.startsWith("INSTRUMENT:") -> {
                instrument = trimmed.removePrefix("INSTRUMENT:").trim()
                section = ""
            }
            trimmed.startsWith("SUMMARY") && trimmed.endsWith(":") -> section = "SUMMARY"
            trimmed == "METRICS:" || trimmed.startsWith("METRICS") && trimmed.endsWith(":") -> section = "METRICS"
            trimmed == "YOUR POSITION:" || trimmed.startsWith("YOUR POSITION") -> section = "POSITION"
            trimmed.startsWith("STRENGTHS") && trimmed.endsWith(":") -> section = "STRENGTHS"
            trimmed.startsWith("RISKS") && trimmed.endsWith(":") -> section = "RISKS"
            trimmed.startsWith("RECOMMENDATION") && trimmed.endsWith(":") -> section = "RECOMMENDATION"
            trimmed.startsWith("DATA:") -> {
                dataSource = trimmed.removePrefix("DATA:").trim()
                section = ""
            }
            trimmed.startsWith("DISCLAIMER:") -> section = ""
            trimmed.isBlank() -> { /* keep section */ }
            else -> when (section) {
                "SUMMARY" -> if (trimmed.startsWith("•") || trimmed.startsWith("-"))
                    summary.add(trimmed.trimStart('•', '-', ' '))
                "METRICS" -> parseTableRow(trimmed)?.let { metrics.add(it) }
                "POSITION" -> parseTableRow(trimmed)?.let { position.add(it) }
                "STRENGTHS" -> if (trimmed.startsWith("•") || trimmed.startsWith("-"))
                    strengths.add(trimmed.trimStart('•', '-', ' '))
                "RISKS" -> if (trimmed.startsWith("•") || trimmed.startsWith("-"))
                    risks.add(trimmed.trimStart('•', '-', ' '))
                "RECOMMENDATION" -> {
                    if (recommendation.isNotEmpty()) recommendation.append(" ")
                    recommendation.append(trimmed)
                }
            }
        }
    }

    return ParsedAnalysis(
        verdict = verdict,
        confidence = confidence,
        instrument = instrument,
        summary = summary,
        metrics = metrics,
        position = position,
        strengths = strengths,
        risks = risks,
        recommendation = recommendation.toString().trim(),
        dataSource = dataSource
    )
}

private fun parseTableRow(line: String): Triple<String, String, String>? {
    val parts = line.split("|").map { it.trim() }
    if (parts.size < 2) return null
    val label = parts[0].ifBlank { return null }
    val value = parts.getOrElse(1) { "" }
    val signal = parts.getOrElse(2) { "" }
    return Triple(label, value, signal)
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
