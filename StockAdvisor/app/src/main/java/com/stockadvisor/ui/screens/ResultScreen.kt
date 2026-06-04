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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockadvisor.data.model.StockAnalysis
import com.stockadvisor.data.model.Verdict
import com.stockadvisor.ui.theme.*
import com.stockadvisor.viewmodel.AnalysisState
import com.stockadvisor.viewmodel.StockViewModel

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
    ) {
        when (val s = state) {
            is AnalysisState.Idle -> {
                LaunchedEffect(Unit) { viewModel.analyzeStock(symbol, decision) }
                LoadingContent("Initializing…")
            }
            is AnalysisState.LoadingMarketData -> LoadingContent("Fetching market data…")
            is AnalysisState.LoadingAIAnalysis ->
                LoadingContent("Running AI research (this may take 15–30 seconds)…")
            is AnalysisState.Success -> SuccessContent(s.analysis, onAnalyzeAnother)
            is AnalysisState.Error -> ErrorContent(s.message, onAnalyzeAnother) {
                viewModel.analyzeStock(symbol, decision)
            }
        }
    }
}

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

@Composable
private fun SuccessContent(analysis: StockAnalysis, onAnalyzeAnother: () -> Unit) {
    val scrollState = rememberScrollState()
    val verdictColor = when (analysis.verdict) {
        Verdict.WISE -> WiseColor
        Verdict.RISKY -> RiskyColor
        Verdict.NEUTRAL -> NeutralColor
    }
    val verdictIcon = when (analysis.verdict) {
        Verdict.WISE -> Icons.Default.CheckCircle
        Verdict.RISKY -> Icons.Default.Warning
        Verdict.NEUTRAL -> Icons.Default.Info
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = analysis.symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = analysis.decision,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = verdictColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = verdictIcon,
                        contentDescription = analysis.verdict.name,
                        tint = verdictColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = analysis.verdict.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = verdictColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        AnalysisContent(analysis.analysisText)

        Spacer(modifier = Modifier.height(24.dp))

        Surface(shape = RoundedCornerShape(8.dp), color = SurfaceVariant) {
            Text(
                text = "For educational purposes only. This is not financial advice.",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
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
    }
}

@Composable
private fun AnalysisContent(text: String) {
    val sections = parseAnalysisSections(text)
    if (sections.isEmpty()) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        sections.forEach { (header, body) ->
            if (header.isNotBlank()) AnalysisSection(title = header, content = body)
            else Text(text = body, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
        }
    }
}

@Composable
private fun AnalysisSection(title: String, content: String) {
    val sectionColor: Color = when {
        "VERDICT" in title.uppercase() -> GreenAccent
        "MARKET" in title.uppercase() -> Secondary
        "TECHNICAL" in title.uppercase() -> AmberAccent
        "FUNDAMENTAL" in title.uppercase() -> Secondary
        "RISK" in title.uppercase() -> RedAccent
        "RECOMMEND" in title.uppercase() -> GreenAccent
        else -> OnSurface
    }
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = sectionColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = Surface) {
            Text(
                text = content.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

private fun parseAnalysisSections(text: String): List<Pair<String, String>> {
    val sectionHeaders = listOf(
        "VERDICT:", "MARKET ANALYSIS:", "TECHNICAL ANALYSIS:",
        "FUNDAMENTAL ANALYSIS:", "RISK FACTORS:", "RECOMMENDATION:", "Disclaimer:"
    )
    val result = mutableListOf<Pair<String, String>>()
    var currentHeader = ""
    val currentBody = StringBuilder()

    for (line in text.lines()) {
        val matchedHeader = sectionHeaders.firstOrNull { line.trimStart().startsWith(it) }
        if (matchedHeader != null) {
            if (currentBody.isNotBlank()) {
                result.add(currentHeader to currentBody.toString())
                currentBody.clear()
            }
            currentHeader = line.trimStart().removeSuffix(":").trim()
            val rest = line.trimStart().removePrefix(matchedHeader).trim()
            if (rest.isNotBlank()) currentBody.append(rest)
        } else {
            if (currentBody.isNotEmpty() || line.isNotBlank()) {
                if (currentBody.isNotEmpty()) currentBody.append("\n")
                currentBody.append(line)
            }
        }
    }
    if (currentBody.isNotBlank()) result.add(currentHeader to currentBody.toString())
    return result
}

@Composable
private fun ErrorContent(
    message: String,
    onAnalyzeAnother: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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
            Text("Search New Ticker")
        }
    }
}
