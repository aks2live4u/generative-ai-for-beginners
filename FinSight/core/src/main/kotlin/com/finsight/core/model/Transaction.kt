package com.finsight.core.model

import java.time.Instant

/** Transactions parsed below this [Transaction.confidence] surface in a review queue instead of silently affecting totals. */
const val TRANSACTION_REVIEW_THRESHOLD = 85

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionSource { SMS, GMAIL, NOTIFICATION, MANUAL }

enum class PaymentMethod { UPI, DEBIT_CARD, CREDIT_CARD, NET_BANKING, WALLET, CASH, UNKNOWN }

/**
 * A single detected financial transaction. [id] is null until persisted.
 *
 * [confidence] (0-99) is how sure the parser was that this is a genuine, correctly-extracted
 * transaction - see [com.finsight.core.parser.MessageClassifier]. Manually entered/merged/migrated
 * transactions default to 99 (already trusted); anything parsed below the review threshold should
 * surface in a review queue rather than silently affecting totals.
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
    val notes: String? = null,
    val confidence: Int = 99
)
