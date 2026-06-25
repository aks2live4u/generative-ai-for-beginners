package com.finsight.core.parser

import com.finsight.core.categorize.CategoryEngine
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionSource
import com.finsight.core.model.TransactionType
import java.time.Instant

/**
 * Parses Gmail subject + snippet/body text into a [Transaction]. Email is generally weaker
 * signal than SMS (marketing mail, order confirmations without amounts), so this is stricter
 * about requiring both an amount and either a clear sender-based category or direction keyword.
 */
object EmailTransactionParser {

    private val knownTransactionalSenders = listOf(
        "amazon", "flipkart", "myntra", "netflix", "swiggy", "zomato",
        "lic", "policybazaar", "zerodha", "groww", "upstox",
        "airtel", "jio", "electricity", "payroll", "hr@"
    )

    fun parse(senderEmail: String, subject: String, bodySnippet: String, receivedAt: Instant): ParseResult {
        val combinedText = "$subject $bodySnippet"
        if (MoneyTextExtractor.isPromotionalOrScam(combinedText)) {
            return ParseResult.NotFinancial("Promotional/scam message")
        }

        val amount = MoneyTextExtractor.extractAmount(combinedText)
            ?: return ParseResult.NotFinancial("No amount found in email")

        val senderLower = senderEmail.lowercase()
        val isKnownSender = knownTransactionalSenders.any { senderLower.contains(it) }
        val explicitType = MoneyTextExtractor.extractTransactionType(combinedText)

        if (explicitType == null && !isKnownSender) {
            return ParseResult.NotFinancial("Unrecognized sender and no direction keyword")
        }

        val type = explicitType ?: if (senderLower.contains("hr@") || senderLower.contains("payroll")) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val merchant = MoneyTextExtractor.extractMerchant(combinedText)
            ?: knownTransactionalSenders.firstOrNull { senderLower.contains(it) }
            ?: senderEmail.substringBefore("@")

        val category = CategoryEngine.categorize("$merchant $combinedText $senderEmail")

        return ParseResult.Success(
            Transaction(
                amount = amount,
                date = receivedAt,
                merchant = merchant,
                category = category,
                type = type,
                paymentMethod = com.finsight.core.model.PaymentMethod.UNKNOWN,
                source = TransactionSource.GMAIL,
                rawText = combinedText
            )
        )
    }
}
