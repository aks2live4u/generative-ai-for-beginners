package com.personalfinanceai.core.model

data class Merchant(
    val name: String,
    val category: Category,
    val transactionCount: Int = 0
)

data class Subscription(
    val serviceName: String,
    val renewalDate: java.time.LocalDate,
    val monthlyCost: Double,
    val lastUsedDate: java.time.LocalDate? = null
)

data class Goal(
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double
) {
    val progressPercent: Double
        get() = if (targetAmount <= 0) 0.0 else (savedAmount / targetAmount * 100).coerceIn(0.0, 100.0)

    val remainingAmount: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)
}
