package com.finsight.ui.state

import com.finsight.core.ai.FinancialHealthScoreCalculator
import com.finsight.core.ai.SavingsDetector
import com.finsight.core.model.Category
import com.finsight.core.model.CategoryGroup
import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.ui.dashboard.CategoryBreakdown
import com.finsight.ui.dashboard.DashboardUiState
import com.finsight.ui.insights.InsightsUiState
import com.finsight.ui.insights.PaymentMethodBreakdown
import com.finsight.ui.transactions.TransactionFilter
import com.finsight.ui.transactions.TransactionsUiState
import com.finsight.ui.transactions.UpcomingBill
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private fun monthOf(tx: Transaction): YearMonth = YearMonth.from(tx.date.atZone(ZoneId.systemDefault()).toLocalDate())
private fun dateOf(tx: Transaction): LocalDate = tx.date.atZone(ZoneId.systemDefault()).toLocalDate()

private fun percentChange(current: Double, previous: Double): Double? {
    if (previous <= 0) return null
    return (current - previous) / previous * 100
}

/** Top expense categories within [range], sorted by spend descending. */
private fun categoryBreakdownFor(transactions: List<Transaction>, range: ClosedRange<LocalDate>): List<CategoryBreakdown> {
    val expensesInRange = transactions.filter { it.type == TransactionType.EXPENSE && dateOf(it) in range }
    val total = expensesInRange.sumOf { it.amount }
    if (total <= 0) return emptyList()
    return expensesInRange
        .groupBy { it.category }
        .map { (category, txs) ->
            val amount = txs.sumOf { it.amount }
            CategoryBreakdown(
                category = category,
                label = category.displayName,
                group = category.group,
                amount = amount,
                percentOfSpend = amount / total * 100
            )
        }
        .sortedByDescending { it.amount }
}

/**
 * Picks the category with the largest percentage drop in spend vs. [previousRange] (at least a
 * 10% drop, to avoid flagging noise) and phrases it as a one-line insight. Returns null when
 * there's no previous period to compare against (e.g. [TimePeriod.ALL_TIME]) or nothing dropped
 * meaningfully.
 */
private fun buildInsightText(
    transactions: List<Transaction>,
    currentRange: ClosedRange<LocalDate>,
    previousRange: ClosedRange<LocalDate>?,
    comparisonLabel: String
): String? {
    if (previousRange == null) return null
    val previousBreakdown = categoryBreakdownFor(transactions, previousRange)
    if (previousBreakdown.isEmpty()) return null
    val currentByLabel = categoryBreakdownFor(transactions, currentRange).associateBy { it.label }
    val biggestDrop = previousBreakdown
        .mapNotNull { prev ->
            val currentAmount = currentByLabel[prev.label]?.amount ?: 0.0
            val change = percentChange(currentAmount, prev.amount) ?: return@mapNotNull null
            prev.label to change
        }
        .filter { (_, change) -> change <= -10.0 }
        .minByOrNull { (_, change) -> change }
        ?: return null
    val (label, change) = biggestDrop
    return "You spent ${"%.0f".format(kotlin.math.abs(change))}% less on $label compared to $comparisonLabel."
}

fun buildDashboardState(
    transactions: List<Transaction>,
    subscriptions: List<Subscription>,
    userName: String,
    period: TimePeriod = TimePeriod.MONTH,
    now: LocalDate = LocalDate.now()
): DashboardUiState {
    val currentRange = period.currentRange(now)
    val previousRange = period.previousRange(now)

    // Money moved into investments (SIPs, mutual funds, etc.) is saved, not spent, and money in
    // CategoryGroup.TRANSFERS (ATM withdrawals, cash given to family) left the account without the
    // user necessarily spending it themselves - both are excluded here so they don't inflate the
    // expense/savings figures the same way they're excluded from the health score calculation.
    // They still appear in categoryBreakdownFor below so users can see exactly where that money went.
    fun isRealExpense(tx: Transaction) = tx.type == TransactionType.EXPENSE &&
        tx.category != Category.INVESTMENT_OUTFLOW &&
        tx.category.group != CategoryGroup.TRANSFERS

    val incomeThisPeriod = transactions.filter { it.type == TransactionType.INCOME && dateOf(it) in currentRange }.sumOf { it.amount }
    val expenseThisPeriod = transactions.filter { isRealExpense(it) && dateOf(it) in currentRange }.sumOf { it.amount }
    val incomeChangePercent = previousRange?.let { range ->
        val incomePrevious = transactions.filter { it.type == TransactionType.INCOME && dateOf(it) in range }.sumOf { it.amount }
        percentChange(incomeThisPeriod, incomePrevious)
    }
    val expenseChangePercent = previousRange?.let { range ->
        val expensePrevious = transactions.filter { isRealExpense(it) && dateOf(it) in range }.sumOf { it.amount }
        percentChange(expenseThisPeriod, expensePrevious)
    }

    val currentMonth = YearMonth.from(now)
    val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
    val incomeTrend = months.map { month -> transactions.filter { it.type == TransactionType.INCOME && monthOf(it) == month }.sumOf { it.amount } }
    val expenseTrend = months.map { month -> transactions.filter { isRealExpense(it) && monthOf(it) == month }.sumOf { it.amount } }
    val trendMonthLabels = months.map { "${it.month.name.take(3)} ${it.year.toString().takeLast(2)}" }

    // No liquid-balance tracking exists yet in this MVP, so the Emergency Fund factor of the
    // health score conservatively assumes 0 - it will read low until balance tracking ships.
    val healthScore = FinancialHealthScoreCalculator.calculate(transactions, subscriptions, liquidSavingsBalance = 0.0, now = now)
    val savingsOpportunities = SavingsDetector.detect(transactions, subscriptions, now)

    val savings = incomeThisPeriod - expenseThisPeriod
    val savingsRatePercent = if (incomeThisPeriod > 0) savings / incomeThisPeriod * 100 else 0.0
    val insightText = buildInsightText(transactions, currentRange, previousRange, period.comparisonLabel)

    return DashboardUiState(
        userName = userName,
        period = period,
        periodLabel = period.label,
        comparisonLabel = period.comparisonLabel,
        income = incomeThisPeriod,
        expense = expenseThisPeriod,
        savings = savings,
        savingsRatePercent = savingsRatePercent,
        insightText = insightText,
        incomeChangePercent = incomeChangePercent,
        expenseChangePercent = expenseChangePercent,
        incomeTrend = incomeTrend,
        expenseTrend = expenseTrend,
        trendMonthLabels = trendMonthLabels,
        topCategories = categoryBreakdownFor(transactions, currentRange).take(5),
        healthScore = healthScore,
        savingsOpportunities = savingsOpportunities
    )
}

fun buildTransactionsState(
    transactions: List<Transaction>,
    subscriptions: List<Subscription>,
    searchQuery: String,
    period: TimePeriod = TimePeriod.MONTH,
    filter: TransactionFilter = TransactionFilter.ALL,
    now: LocalDate = LocalDate.now()
): TransactionsUiState {
    val currentRange = period.currentRange(now)
    val upcomingBills = subscriptions
        .filter { !it.renewalDate.isBefore(now) && it.renewalDate.isBefore(now.plusDays(30)) }
        .sortedBy { it.renewalDate }
        .map { UpcomingBill(name = it.serviceName, amount = it.monthlyCost, dueDate = it.renewalDate) }

    val lastSalary = transactions
        .filter { it.category == Category.SALARY }
        .maxByOrNull { it.date }
    val nextSalaryDate = lastSalary?.date?.atZone(ZoneId.systemDefault())?.toLocalDate()?.plusMonths(1)

    val transactionsInPeriod = transactions
        .filter { dateOf(it) in currentRange }
        .filter { filter.type == null || it.type == filter.type }
    val filteredTransactions = if (searchQuery.isBlank()) {
        transactionsInPeriod
    } else {
        transactionsInPeriod.filter {
            it.merchant.contains(searchQuery, ignoreCase = true) || it.category.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    return TransactionsUiState(
        period = period,
        periodLabel = period.label,
        filter = filter,
        spendingBreakdown = categoryBreakdownFor(transactions, currentRange),
        subscriptions = subscriptions,
        upcomingBills = upcomingBills,
        nextSalaryDate = nextSalaryDate,
        transactions = filteredTransactions,
        searchQuery = searchQuery
    )
}

/** Spend grouped by [com.finsight.core.model.PaymentMethod] (UPI vs. credit card vs. cash, etc.) for the current month. */
private fun paymentMethodBreakdownFor(transactions: List<Transaction>, range: ClosedRange<LocalDate>): List<PaymentMethodBreakdown> {
    val expensesInRange = transactions.filter { it.type == TransactionType.EXPENSE && dateOf(it) in range }
    val total = expensesInRange.sumOf { it.amount }
    if (total <= 0) return emptyList()
    return expensesInRange
        .groupBy { it.paymentMethod }
        .map { (method, txs) ->
            val amount = txs.sumOf { it.amount }
            PaymentMethodBreakdown(method = method, amount = amount, percentOfSpend = amount / total * 100)
        }
        .sortedByDescending { it.amount }
}

fun buildInsightsState(
    transactions: List<Transaction>,
    subscriptions: List<Subscription>,
    now: LocalDate = LocalDate.now()
): InsightsUiState {
    val currentRange = TimePeriod.MONTH.currentRange(now)
    return InsightsUiState(
        healthScore = FinancialHealthScoreCalculator.calculate(transactions, subscriptions, liquidSavingsBalance = 0.0, now = now),
        savingsOpportunities = SavingsDetector.detect(transactions, subscriptions, now),
        paymentMethodBreakdown = paymentMethodBreakdownFor(transactions, currentRange)
    )
}
