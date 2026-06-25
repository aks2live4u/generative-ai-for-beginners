package com.finsight.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finsight.core.ai.FinancialHealthFactor
import com.finsight.core.ai.FinancialHealthScore
import com.finsight.core.ai.SavingsOpportunity
import com.finsight.ui.components.PrimaryButton
import com.finsight.ui.components.ScoreRing
import com.finsight.ui.components.formatRupees
import com.finsight.ui.theme.financeColors
import kotlinx.coroutines.launch

data class InsightsUiState(
    val healthScore: FinancialHealthScore? = null,
    val savingsOpportunities: List<SavingsOpportunity> = emptyList()
)

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onOpenChat: () -> Unit,
    onBackupNow: suspend () -> Boolean
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))
            state.healthScore?.let { score ->
                FinancialHealthScoreCard(score)
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (state.savingsOpportunities.isNotEmpty()) {
                HiddenExpenseFinderFullCard(state.savingsOpportunities)
                Spacer(modifier = Modifier.height(20.dp))
            }
            BackupCard(onBackupNow)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackupCard(onBackupNow: suspend () -> Boolean) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Text(
                text = "Your encrypted database is backed up automatically every day. You can also trigger a backup right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
            )
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                PrimaryButton(
                    text = "Back Up Now",
                    onClick = {
                        isRunning = true
                        resultMessage = null
                        scope.launch {
                            val success = onBackupNow()
                            resultMessage = if (success) "Backup created successfully." else "Backup failed."
                            isRunning = false
                        }
                    }
                )
            }
            resultMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.contains("success")) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun FinancialHealthScoreCard(score: FinancialHealthScore) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Financial Health Score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                ScoreRing(
                    score = score.score,
                    color = MaterialTheme.financeColors.income,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${score.score}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = score.interpretation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            score.factors.forEach { factor -> FactorRow(factor) }
        }
    }
}

@Composable
private fun FactorRow(factor: FinancialHealthFactor) {
    val good = factor.score >= factor.maxScore / 2
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (good) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = if (good) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = factor.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = factor.note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${factor.score}/${factor.maxScore}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HiddenExpenseFinderFullCard(opportunities: List<SavingsOpportunity>) {
    val totalAnnual = opportunities.sumOf { it.estimatedAnnualSavings }
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hidden Expense Finder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Potential savings: ${formatRupees(totalAnnual)}/year",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.financeColors.income
            )
            Spacer(modifier = Modifier.height(14.dp))
            opportunities.forEach { opportunity ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = opportunity.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatRupees(opportunity.estimatedAnnualSavings) + "/yr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financeColors.income
                        )
                    }
                    Text(
                        text = opportunity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
