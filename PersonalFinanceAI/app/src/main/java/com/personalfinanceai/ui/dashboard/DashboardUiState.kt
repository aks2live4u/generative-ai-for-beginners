package com.personalfinanceai.ui.dashboard

import com.personalfinanceai.core.ai.FinancialHealthScore
import com.personalfinanceai.core.ai.SavingsOpportunity
import com.personalfinanceai.core.model.CategoryGroup

data class CategoryBreakdown(
    val label: String,
    val group: CategoryGroup,
    val amount: Double,
    val percentOfSpend: Double
)

data class DashboardUiState(
    val userName: String = "",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val incomeChangePercent: Double? = null,
    val expenseChangePercent: Double? = null,
    val incomeTrend: List<Double> = emptyList(),
    val expenseTrend: List<Double> = emptyList(),
    val topCategories: List<CategoryBreakdown> = emptyList(),
    val healthScore: FinancialHealthScore? = null,
    val savingsOpportunities: List<SavingsOpportunity> = emptyList(),
    val latestChatPreview: String? = null
)
