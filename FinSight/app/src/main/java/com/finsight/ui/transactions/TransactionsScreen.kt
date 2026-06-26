package com.finsight.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finsight.core.model.Category
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.ui.components.CategoryPickerDialog
import com.finsight.ui.components.DonutChart
import com.finsight.ui.components.DonutSlice
import com.finsight.ui.components.PeriodSelector
import com.finsight.ui.components.MerchantBadge
import com.finsight.ui.components.colorForGroup
import com.finsight.ui.components.formatRupees
import com.finsight.ui.components.mediumDateFormatter
import com.finsight.ui.state.TimePeriod
import com.finsight.ui.theme.financeColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onSearchQueryChange: (String) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onReclassifyTransaction: (Transaction, Category) -> Unit = { _, _ -> },
    onReclassifyCategory: (Category, Category) -> Unit = { _, _ -> },
    onPeriodSelected: (TimePeriod) -> Unit = {},
    onFilterSelected: (TransactionFilter) -> Unit = {},
    onConfirmReview: (Transaction) -> Unit = {},
    onRejectReview: (Transaction) -> Unit = {}
) {
    var reclassifyTarget by remember { mutableStateOf<Transaction?>(null) }
    var bulkReclassifyTarget by remember { mutableStateOf<Category?>(null) }

    reclassifyTarget?.let { tx ->
        CategoryPickerDialog(
            title = "Move \"${tx.merchant}\" to...",
            onCategorySelected = { category ->
                onReclassifyTransaction(tx, category)
                reclassifyTarget = null
            },
            onDismiss = { reclassifyTarget = null }
        )
    }
    bulkReclassifyTarget?.let { category ->
        CategoryPickerDialog(
            title = "Move all \"${category.displayName}\" to...",
            onCategorySelected = { newCategory ->
                onReclassifyCategory(category, newCategory)
                bulkReclassifyTarget = null
            },
            onDismiss = { bulkReclassifyTarget = null }
        )
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PeriodSelector(selected = state.period, onSelected = onPeriodSelected)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionFilter.entries.forEach { filterOption ->
                        FilterChip(
                            selected = filterOption == state.filter,
                            onClick = { onFilterSelected(filterOption) },
                            label = { Text(text = filterOption.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search transactions") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (state.needsReview.isNotEmpty()) {
                item {
                    ReviewQueueCard(
                        items = state.needsReview,
                        onConfirm = onConfirmReview,
                        onRecategorize = { tx -> reclassifyTarget = tx },
                        onReject = onRejectReview
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (state.spendingBreakdown.isNotEmpty()) {
                item {
                    SpendingBreakdownCard(
                        breakdown = state.spendingBreakdown,
                        periodLabel = state.periodLabel,
                        onCategoryClick = { category -> bulkReclassifyTarget = category }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (state.subscriptions.isNotEmpty()) {
                item {
                    SubscriptionDetectorCard(state.subscriptions)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (state.upcomingBills.isNotEmpty()) {
                item {
                    UpcomingBillsCard(state.upcomingBills)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            state.nextSalaryDate?.let { date ->
                item {
                    SalaryPredictorCard(date)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            val grouped = groupByDay(state.transactions)
            grouped.forEach { (label, txs) ->
                item {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(txs) { tx ->
                    TransactionRow(
                        tx = tx,
                        onClick = {
                            onTransactionClick(tx)
                            reclassifyTarget = tx
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * Transactions the parser wasn't fully sure about ([Transaction.confidence] below the review
 * threshold) - e.g. an ambiguous email, or a notification with no explicit debit/credit keyword.
 * Shown separately from the normal list so the user decides whether they're real instead of the
 * app silently folding them into totals.
 */
@Composable
private fun ReviewQueueCard(
    items: List<Transaction>,
    onConfirm: (Transaction) -> Unit,
    onRecategorize: (Transaction) -> Unit,
    onReject: (Transaction) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Needs Review (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Not fully sure these are correct - confirm, recategorize, or remove them.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            items.forEach { tx ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.merchant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${tx.rawText.take(80)} - ${tx.confidence}% confident",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (tx.type == TransactionType.INCOME) "+" else "-") + formatRupees(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (tx.type == TransactionType.INCOME) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.TextButton(onClick = { onConfirm(tx) }) { Text("Looks right") }
                        androidx.compose.material3.TextButton(onClick = { onRecategorize(tx) }) { Text("Recategorize") }
                        androidx.compose.material3.TextButton(onClick = { onReject(tx) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingBreakdownCard(
    breakdown: List<com.finsight.ui.dashboard.CategoryBreakdown>,
    periodLabel: String,
    onCategoryClick: (Category) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending Breakdown ($periodLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap a category to reclassify it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(120.dp)) {
                    DonutChart(
                        slices = breakdown.map { DonutSlice(it.label, it.amount, colorForGroup(it.group)) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    breakdown.take(5).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .clickable(onClick = { onCategoryClick(item.category) })
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colorForGroup(item.group), CircleShape)
                            )
                            Text(
                                text = "${item.label} ${"%.0f".format(item.percentOfSpend)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDetectorCard(subscriptions: List<com.finsight.core.model.Subscription>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Subscriptions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Subscription Detector",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            subscriptions.take(4).forEach { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = com.finsight.ui.components.merchantBadgeColor(
                                sub.serviceName,
                                MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = com.finsight.ui.components.merchantInitial(sub.serviceName),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                        Text(
                            text = sub.serviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                    Text(
                        text = "${formatRupees(sub.monthlyCost)}/mo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingBillsCard(bills: List<UpcomingBill>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Upcoming Bills",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            bills.forEach { bill ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bill.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Due ${bill.dueDate.format(mediumDateFormatter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatRupees(bill.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.financeColors.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun SalaryPredictorCard(date: LocalDate) {
    val daysAway = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.financeColors.income
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Salary Predictor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (daysAway >= 0) "Expected in $daysAway days" else "Expected today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantBadge(merchant = tx.merchant, group = tx.category.group)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = tx.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = tx.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val isIncome = tx.type == TransactionType.INCOME
            Text(
                text = (if (isIncome) "+" else "-") + formatRupees(tx.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isIncome) MaterialTheme.financeColors.income else MaterialTheme.financeColors.expense
            )
        }
    }
}

private fun groupByDay(transactions: List<Transaction>): List<Pair<String, List<Transaction>>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return transactions
        .sortedByDescending { it.date }
        .groupBy { it.date.atZone(ZoneId.systemDefault()).toLocalDate() }
        .map { (date, txs) ->
            val label = when (date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> date.format(mediumDateFormatter)
            }
            label to txs
        }
}
