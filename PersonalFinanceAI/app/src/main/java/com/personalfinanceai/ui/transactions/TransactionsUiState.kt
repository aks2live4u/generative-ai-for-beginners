package com.personalfinanceai.ui.transactions

import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.ui.dashboard.CategoryBreakdown

data class UpcomingBill(
    val name: String,
    val amount: Double,
    val dueDate: java.time.LocalDate
)

data class TransactionsUiState(
    val spendingBreakdown: List<CategoryBreakdown> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val nextSalaryDate: java.time.LocalDate? = null,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = ""
)
