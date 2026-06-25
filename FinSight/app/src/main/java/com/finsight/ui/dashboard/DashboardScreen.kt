package com.finsight.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finsight.ui.components.PeriodSelector
import com.finsight.ui.components.TrendLineChart
import com.finsight.ui.components.colorForGroup
import com.finsight.ui.components.formatRupees
import com.finsight.ui.components.iconForGroup
import com.finsight.ui.state.TimePeriod
import com.finsight.ui.theme.CategoryAccents
import com.finsight.ui.theme.financeColors

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenChat: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenNotifications: () -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit = {}
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            GreetingHeader(userName = state.userName, onOpenNotifications = onOpenNotifications)
            Spacer(modifier = Modifier.height(16.dp))
            PeriodSelector(selected = state.period, onSelected = onPeriodSelected)
            Spacer(modifier = Modifier.height(20.dp))
            SnapshotRow(state = state)
            Spacer(modifier = Modifier.height(12.dp))
            SavingsRow(state = state)
            Spacer(modifier = Modifier.height(20.dp))
            state.insightText?.let { insight ->
                AiInsightCard(insight = insight, onClick = onOpenInsights)
                Spacer(modifier = Modifier.height(20.dp))
            }
            TrendCard(state = state)
            Spacer(modifier = Modifier.height(20.dp))
            if (state.topCategories.isNotEmpty()) {
                Text(
                    text = "Top Categories (${state.periodLabel})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                TopCategoriesRow(categories = state.topCategories)
                Spacer(modifier = Modifier.height(20.dp))
            }
            AiAssistantPreviewCard(preview = state.latestChatPreview, onClick = onOpenChat)
            Spacer(modifier = Modifier.height(20.dp))
            state.healthScore?.let { score ->
                HealthScoreMiniCard(score = score.score, interpretation = score.interpretation, onClick = onOpenInsights)
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (state.savingsOpportunities.isNotEmpty()) {
                HiddenExpenseFinderCard(opportunities = state.savingsOpportunities.take(2), onClick = onOpenInsights)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GreetingHeader(userName: String, onOpenNotifications: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good morning, $userName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Here's your financial overview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenNotifications) {
            Icon(
                imageVector = Icons.Filled.NotificationsNone,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SnapshotRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SnapshotCard(
            label = "Income (${state.periodLabel})",
            amount = state.income,
            changePercent = state.incomeChangePercent,
            comparisonLabel = state.comparisonLabel,
            amountColor = MaterialTheme.financeColors.income,
            modifier = Modifier.weight(1f)
        )
        SnapshotCard(
            label = "Expenses (${state.periodLabel})",
            amount = state.expense,
            changePercent = state.expenseChangePercent,
            comparisonLabel = state.comparisonLabel,
            amountColor = MaterialTheme.financeColors.expense,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SnapshotCard(
    label: String,
    amount: Double,
    changePercent: Double?,
    comparisonLabel: String,
    amountColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatRupees(amount),
                style = MaterialTheme.typography.headlineSmall,
                color = amountColor
            )
            changePercent?.let {
                Spacer(modifier = Modifier.height(4.dp))
                val arrow = if (it >= 0) "↑" else "↓"
                Text(
                    text = "$arrow ${"%.0f".format(kotlin.math.abs(it))}% vs $comparisonLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SavingsRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You've Saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatRupees(state.savings),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.savings >= 0) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Savings Rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${"%.0f".format(state.savingsRatePercent)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (state.savingsRatePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.financeColors.income,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiInsightCard(insight: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Insight",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Text(
                text = "View Details",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TrendCard(state: DashboardUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Last 6 months",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = MaterialTheme.financeColors.income, label = "Income")
                LegendDot(color = MaterialTheme.financeColors.expense, label = "Expenses")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TrendLineChart(
                incomeSeries = state.incomeTrend,
                expenseSeries = state.expenseTrend,
                monthLabels = state.trendMonthLabels,
                incomeColor = MaterialTheme.financeColors.income,
                expenseColor = MaterialTheme.financeColors.expense
            )
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun TopCategoriesRow(categories: List<CategoryBreakdown>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(categories) { category ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorForGroup(category.group).copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = iconForGroup(category.group),
                            contentDescription = null,
                            tint = colorForGroup(category.group),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${"%.0f".format(category.percentOfSpend)}% - ${formatRupees(category.amount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAssistantPreviewCard(preview: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "AI Financial Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = preview ?: "Ask me anything about your money",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthScoreMiniCard(score: Int, interpretation: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.financeColors.income.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financeColors.income
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = "Financial Health Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = interpretation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HiddenExpenseFinderCard(
    opportunities: List<com.finsight.core.ai.SavingsOpportunity>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hidden Expense Finder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            opportunities.forEach { opportunity ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = opportunity.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatRupees(opportunity.estimatedAnnualSavings) + "/yr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CategoryAccents.Orange
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Review & Save",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
