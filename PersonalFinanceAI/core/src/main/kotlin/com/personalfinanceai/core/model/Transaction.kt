package com.personalfinanceai.core.model

import java.time.Instant

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionSource { SMS, GMAIL, NOTIFICATION, MANUAL }

enum class PaymentMethod { UPI, DEBIT_CARD, CREDIT_CARD, NET_BANKING, WALLET, CASH, UNKNOWN }

/**
 * A single detected financial transaction. [id] is null until persisted.
 */
data class Transaction(
    val id: Long? = null,
    val amount: Double,
    val date: Instant,
    val merchant: String,
    val category: Category,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val source: TransactionSource,
    val rawText: String,
    val notes: String? = null
)
