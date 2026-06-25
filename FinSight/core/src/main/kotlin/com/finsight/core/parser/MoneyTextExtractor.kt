package com.finsight.core.parser

import com.finsight.core.model.PaymentMethod
import com.finsight.core.model.TransactionType

/**
 * Shared text-extraction helpers used by the SMS, email and notification parsers.
 * Kept regex-based (no ML model) so categorization/extraction is fully offline and deterministic.
 */
object MoneyTextExtractor {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "spent", "paid", "withdrawn", "purchase of", "debit of", "sent", "deducted"
    )
    private val creditKeywords = listOf(
        "credited", "received", "deposited", "credit of", "refunded", "cashback of"
    )

    /** Extracts the first currency amount found in [text], or null if none present. */
    fun extractAmount(text: String): Double? {
        val match = amountRegex.find(text) ?: return null
        val numeric = match.groupValues[1].replace(",", "")
        return numeric.toDoubleOrNull()
    }

    /** Infers whether the text describes money coming in or going out. */
    fun extractTransactionType(text: String): TransactionType? {
        val lower = text.lowercase()
        val isDebit = debitKeywords.any { lower.contains(it) }
        val isCredit = creditKeywords.any { lower.contains(it) }
        return when {
            isDebit && !isCredit -> TransactionType.EXPENSE
            isCredit && !isDebit -> TransactionType.INCOME
            // Ambiguous (both or neither keyword present): prefer whichever keyword occurs first.
            isDebit && isCredit -> {
                val debitIdx = debitKeywords.minOf { kw -> lower.indexOf(kw).let { if (it < 0) Int.MAX_VALUE else it } }
                val creditIdx = creditKeywords.minOf { kw -> lower.indexOf(kw).let { if (it < 0) Int.MAX_VALUE else it } }
                if (debitIdx <= creditIdx) TransactionType.EXPENSE else TransactionType.INCOME
            }
            else -> null
        }
    }

    fun extractPaymentMethod(text: String): PaymentMethod {
        val lower = text.lowercase()
        return when {
            lower.contains("upi") || lower.contains("vpa") -> PaymentMethod.UPI
            lower.contains("credit card") || Regex("""\bcc\b""").containsMatchIn(lower) -> PaymentMethod.CREDIT_CARD
            lower.contains("debit card") -> PaymentMethod.DEBIT_CARD
            lower.contains("net banking") || lower.contains("netbanking") -> PaymentMethod.NET_BANKING
            lower.contains("wallet") -> PaymentMethod.WALLET
            lower.contains("cash") -> PaymentMethod.CASH
            else -> PaymentMethod.UNKNOWN
        }
    }

    /**
     * Best-effort merchant extraction: looks for text following common prepositions used by
     * Indian bank/UPI SMS templates ("to X", "at X", "via UPI to X", "VPA x@bank").
     * Falls back to null when no recognizable merchant token is found.
     */
    fun extractMerchant(text: String): String? {
        val patterns = listOf(
            Regex("""(?:vpa)\s+([a-zA-Z0-9._-]+)@""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|at|in favou?r of)\s+([A-Za-z0-9 .&'_-]{2,40}?)(?:\s+on\b|\s+ref\b|\s+via\b|[.,]|$)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].trim()
                if (raw.isNotBlank()) return raw
            }
        }
        return null
    }
}
