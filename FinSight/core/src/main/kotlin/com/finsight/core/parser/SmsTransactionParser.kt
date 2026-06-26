package com.finsight.core.parser

import com.finsight.core.categorize.CategoryEngine
import com.finsight.core.model.Category
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionSource
import java.time.Instant

/**
 * Parses bank/UPI/wallet SMS text into a [Transaction]. Only acts on messages that contain a
 * recognizable currency amount and debit/credit direction; anything else (OTPs, promos) is
 * reported as [ParseResult.NotFinancial] and never persisted.
 */
object SmsTransactionParser {

    fun parse(rawText: String, receivedAt: Instant): ParseResult {
        val lower = rawText.lowercase()
        val classification = MessageClassifier.classify(rawText)
        if (classification.type != MessageType.TRANSACTION) {
            return ParseResult.NotFinancial(classification.reason)
        }

        val amount = MoneyTextExtractor.extractAmount(rawText)
            ?: return ParseResult.NotFinancial("No amount found")
        val type = MoneyTextExtractor.extractTransactionType(rawText)
            ?: return ParseResult.NotFinancial("No debit/credit keyword found")

        val paymentMethod = MoneyTextExtractor.extractPaymentMethod(rawText)
        val merchant = MoneyTextExtractor.extractMerchant(rawText)
            ?: inferMerchantForSalaryOrGeneric(lower)
            ?: "Unknown"

        val category: Category = CategoryEngine.categorize("$merchant $rawText")

        return ParseResult.Success(
            Transaction(
                amount = amount,
                date = receivedAt,
                merchant = merchant,
                category = category,
                type = type,
                paymentMethod = paymentMethod,
                source = TransactionSource.SMS,
                rawText = rawText,
                confidence = classification.confidence
            )
        )
    }

    private fun inferMerchantForSalaryOrGeneric(lower: String): String? = when {
        lower.contains("salary") -> "Salary"
        lower.contains("interest credited") -> "Bank Interest"
        lower.contains("dividend") -> "Dividend"
        else -> null
    }
}
