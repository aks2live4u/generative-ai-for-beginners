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

    private val otpKeywords = listOf("otp", "one time password", "do not share")

    fun parse(rawText: String, receivedAt: Instant): ParseResult {
        val lower = rawText.lowercase()
        if (otpKeywords.any { lower.contains(it) }) {
            return ParseResult.NotFinancial("OTP message")
        }
        if (MoneyTextExtractor.isPromotionalOrScam(rawText)) {
            return ParseResult.NotFinancial("Promotional/scam message")
        }
        if (MoneyTextExtractor.isMandateRegistration(rawText)) {
            return ParseResult.NotFinancial("Standing instruction/mandate registration, not an actual transaction")
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
                rawText = rawText
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
