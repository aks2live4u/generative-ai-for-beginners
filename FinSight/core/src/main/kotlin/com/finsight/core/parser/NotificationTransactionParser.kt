package com.finsight.core.parser

import com.finsight.core.categorize.CategoryEngine
import com.finsight.core.model.PaymentMethod
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionSource
import com.finsight.core.model.TransactionType
import java.time.Instant

/**
 * Parses notifications from a fixed allow-list of apps (Swiggy, Zomato, Uber, Ola, Blinkit,
 * Amazon, ...). Unlike SMS/email these are first-party app notifications, so they're almost
 * always an expense and the source app already tells us the merchant.
 */
object NotificationTransactionParser {

    private val supportedPackageToMerchant = mapOf(
        "in.swiggy.android" to "Swiggy",
        "com.application.zomato" to "Zomato",
        "com.ubercab" to "Uber",
        "com.olacabs.customer" to "Ola",
        "app.blinkit.ondc" to "Blinkit",
        "com.grofers.customerapp" to "Blinkit",
        "in.amazon.mShop.android.shopping" to "Amazon"
    )

    fun isSupportedPackage(packageName: String): Boolean = supportedPackageToMerchant.containsKey(packageName)

    fun parse(packageName: String, title: String, text: String, postedAt: Instant): ParseResult {
        val merchant = supportedPackageToMerchant[packageName]
            ?: return ParseResult.NotFinancial("Unsupported notification source: $packageName")

        val combinedText = "$title $text"
        if (MoneyTextExtractor.isPromotionalOrScam(combinedText)) {
            return ParseResult.NotFinancial("Promotional/scam message")
        }

        val amount = MoneyTextExtractor.extractAmount(combinedText)
            ?: return ParseResult.NotFinancial("No amount found in notification")

        val type = MoneyTextExtractor.extractTransactionType(combinedText) ?: TransactionType.EXPENSE
        val category = CategoryEngine.categorize("$merchant $combinedText")

        return ParseResult.Success(
            Transaction(
                amount = amount,
                date = postedAt,
                merchant = merchant,
                category = category,
                type = type,
                paymentMethod = PaymentMethod.UNKNOWN,
                source = TransactionSource.NOTIFICATION,
                rawText = combinedText
            )
        )
    }
}
