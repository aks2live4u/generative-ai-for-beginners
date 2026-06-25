package com.finsight.ui.transactions

import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.ui.dashboard.CategoryBreakdown
import com.finsight.ui.state.TimePeriod

data class UpcomingBill(
    val name: String,
    val amount: Double,
    val dueDate: java.time.LocalDate
)

/** Which rows the Transactions list shows. Null means no filtering by type. */
enum class TransactionFilter(val label: String, val type: TransactionType?) {
    ALL("All", null),
    INCOME("Income", TransactionType.INCOME),
    EXPENSE("Expense", TransactionType.EXPENSE)
}

data class TransactionsUiState(
    val period: TimePeriod = TimePeriod.MONTH,
    val periodLabel: String = TimePeriod.MONTH.label,
    val filter: TransactionFilter = TransactionFilter.ALL,
    val spendingBreakdown: List<CategoryBreakdown> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val nextSalaryDate: java.time.LocalDate? = null,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = ""
)
