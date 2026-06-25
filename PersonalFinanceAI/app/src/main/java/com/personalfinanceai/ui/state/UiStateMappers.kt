package com.personalfinanceai.ui.state

import com.personalfinanceai.core.ai.FinancialHealthScoreCalculator
import com.personalfinanceai.core.ai.SavingsDetector
import com.personalfinanceai.core.model.Category
import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.core.model.TransactionType
import com.personalfinanceai.ui.dashboard.CategoryBreakdown
import com.personalfinanceai.ui.dashboard.DashboardUiState
import com.personalfinanceai.ui.insights.InsightsUiState
import com.personalfinanceai.ui.transactions.TransactionsUiState
import com.personalfinanceai.ui.transactions.UpcomingBill
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private fun monthOf(tx: Transaction): YearMonth = YearMonth.from(tx.date.atZone(ZoneId.systemDefault()).toLocalDate())

private fun percentChange(current: Double, previous: Double): Double? {
    if (previous <= 0) return null
    return (current - previous) / previous * 100
}

/** Top expense categories for [month], sorted by spend descending. */
private fun categoryBreakdownFor(transactions: List<Transaction>, month: YearMonth): List<CategoryBreakdown> {
    val expensesThisMonth = transactions.filter { it.type == TransactionType.EXPENSE && monthOf(it) == month }
    val total = expensesThisMonth.sumOf { it.amount }
    if (total <= 0) return emptyList()
    return expensesThisMonth
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
    now: LocalDate = LocalDate.now()
): DashboardUiState {
    val currentMonth = YearMonth.from(now)
    val previousMonth = currentMonth.minusMonths(1)

    val incomeThisMonth = transactions.filter { it.type == TransactionType.INCOME && monthOf(it) == currentMonth }.sumOf { it.amount }
    val expenseThisMonth = transactions.filter { it.type == TransactionType.EXPENSE && monthOf(it) == currentMonth }.sumOf { it.amount }
    val incomeLastMonth = transactions.filter { it.type == TransactionType.INCOME && monthOf(it) == previousMonth }.sumOf { it.amount }
    val expenseLastMonth = transactions.filter { it.type == TransactionType.EXPENSE && monthOf(it) == previousMonth }.sumOf { it.amount }

    val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
    val incomeTrend = months.map { month -> transactions.filter { it.type == TransactionType.INCOME && monthOf(it) == month }.sumOf { it.amount } }
    val expenseTrend = months.map { month -> transactions.filter { it.type == TransactionType.EXPENSE && monthOf(it) == month }.sumOf { it.amount } }

    // No liquid-balance tracking exists yet in this MVP, so the Emergency Fund factor of the
    // health score conservatively assumes 0 - it will read low until balance tracking ships.
    val healthScore = FinancialHealthScoreCalculator.calculate(transactions, subscriptions, liquidSavingsBalance = 0.0, now = now)
    val savingsOpportunities = SavingsDetector.detect(transactions, subscriptions, now)

    return DashboardUiState(
        userName = userName,
        income = incomeThisMonth,
        expense = expenseThisMonth,
        incomeChangePercent = percentChange(incomeThisMonth, incomeLastMonth),
        expenseChangePercent = percentChange(expenseThisMonth, expenseLastMonth),
        incomeTrend = incomeTrend,
        expenseTrend = expenseTrend,
        topCategories = categoryBreakdownFor(transactions, currentMonth).take(5),
        healthScore = healthScore,
        savingsOpportunities = savingsOpportunities
    )
}

fun buildTransactionsState(
    transactions: List<Transaction>,
    subscriptions: List<Subscription>,
    searchQuery: String,
    now: LocalDate = LocalDate.now()
): TransactionsUiState {
    val currentMonth = YearMonth.from(now)
    val upcomingBills = subscriptions
        .filter { !it.renewalDate.isBefore(now) && it.renewalDate.isBefore(now.plusDays(30)) }
        .sortedBy { it.renewalDate }
        .map { UpcomingBill(name = it.serviceName, amount = it.monthlyCost, dueDate = it.renewalDate) }

    val lastSalary = transactions
        .filter { it.category == Category.SALARY }
        .maxByOrNull { it.date }
    val nextSalaryDate = lastSalary?.date?.atZone(ZoneId.systemDefault())?.toLocalDate()?.plusMonths(1)

    val filteredTransactions = if (searchQuery.isBlank()) {
        transactions
    } else {
        transactions.filter {
            it.merchant.contains(searchQuery, ignoreCase = true) || it.category.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    return TransactionsUiState(
        spendingBreakdown = categoryBreakdownFor(transactions, currentMonth),
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
