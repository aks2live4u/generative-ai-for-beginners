package com.finsight.ui.transactions

import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.ui.dashboard.CategoryBreakdown
import com.finsight.ui.state.TimePeriod

data class UpcomingBill(
    val name: String,
    val amount: Double,
    val dueDate: java.time.LocalDate
)

data class TransactionsUiState(
    val period: TimePeriod = TimePeriod.MONTH,
    val periodLabel: String = TimePeriod.MONTH.label,
    val spendingBreakdown: List<CategoryBreakdown> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val nextSalaryDate: java.time.LocalDate? = null,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = ""
)
