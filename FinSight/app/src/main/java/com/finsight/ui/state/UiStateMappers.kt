package com.finsight.ui.state

import com.finsight.core.ai.FinancialHealthScoreCalculator
import com.finsight.core.ai.SavingsDetector
import com.finsight.core.model.Category
import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.ui.dashboard.CategoryBreakdown
import com.finsight.ui.dashboard.DashboardUiState
import com.finsight.ui.insights.InsightsUiState
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
                label = category.displayName,
                group = category.group,
                amount = amount,
                percentOfSpend = amount / total * 100
            )
        }
        .sortedByDescending { it.amount }
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

    val incomeThisPeriod = transactions.filter { it.type == TransactionType.INCOME && dateOf(it) in currentRange }.sumOf { it.amount }
    val expenseThisPeriod = transactions.filter { it.type == TransactionType.EXPENSE && dateOf(it) in currentRange }.sumOf { it.amount }
    val incomeChangePercent = previousRange?.let { range ->
        val incomePrevious = transactions.filter { it.type == TransactionType.INCOME && dateOf(it) in range }.sumOf { it.amount }
        percentChange(incomeThisPeriod, incomePrevious)
    }
    val expenseChangePercent = previousRange?.let { range ->
        val expensePrevious = transactions.filter { it.type == TransactionType.EXPENSE && dateOf(it) in range }.sumOf { it.amount }
        percentChange(expenseThisPeriod, expensePrevious)
    }

    val currentMonth = YearMonth.from(now)
    val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
    val incomeTrend = months.map { month -> transactions.filter { it.type == TransactionType.INCOME && monthOf(it) == month }.sumOf { it.amount } }
    val expenseTrend = months.map { month -> transactions.filter { it.type == TransactionType.EXPENSE && monthOf(it) == month }.sumOf { it.amount } }
    val trendMonthLabels = months.map { "${it.month.name.take(3)} ${it.year.toString().takeLast(2)}" }

    // No liquid-balance tracking exists yet in this MVP, so the Emergency Fund factor of the
    // health score conservatively assumes 0 - it will read low until balance tracking ships.
    val healthScore = FinancialHealthScoreCalculator.calculate(transactions, subscriptions, liquidSavingsBalance = 0.0, now = now)
    val savingsOpportunities = SavingsDetector.detect(transactions, subscriptions, now)

    return DashboardUiState(
        userName = userName,
        period = period,
        periodLabel = period.label,
        comparisonLabel = period.comparisonLabel,
        income = incomeThisPeriod,
        expense = expenseThisPeriod,
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

    val transactionsInPeriod = transactions.filter { dateOf(it) in currentRange }
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
        spendingBreakdown = categoryBreakdownFor(transactions, currentRange),
        subscriptions = subscriptions,
        upcomingBills = upcomingBills,
        nextSalaryDate = nextSalaryDate,
        transactions = filteredTransactions,
        searchQuery = searchQuery
    )
}

fun buildInsightsState(
    transactions: List<Transaction>,
    subscriptions: List<Subscription>,
    now: LocalDate = LocalDate.now()
): InsightsUiState = InsightsUiState(
    healthScore = FinancialHealthScoreCalculator.calculate(transactions, subscriptions, liquidSavingsBalance = 0.0, now = now),
    savingsOpportunities = SavingsDetector.detect(transactions, subscriptions, now)
)
