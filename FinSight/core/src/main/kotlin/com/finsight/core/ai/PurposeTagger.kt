package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.Purpose
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.core.parser.MerchantMatcher

/** A user-taught override of the default [Category] to [Purpose] mapping for one merchant, e.g. "tag mom as family". */
data class MerchantPurposeRule(val merchantKey: String, val purpose: Purpose)

/**
 * Tags every transaction with *why* the money moved (Need/Want/Investment/Family/Growth/Lifestyle/
 * Debt), not just which [Category] it falls under. Starts from a sensible default per category,
 * then lets a personal [MerchantPurposeRule] (taught the same way as [com.finsight.core.parser.MerchantRuleBook]
 * merchant labels) override it per-merchant.
 */
object PurposeTagger {

    private val defaultMapping: Map<Category, Purpose> = mapOf(
        Category.GROCERIES to Purpose.NEED,
        Category.ELECTRICITY to Purpose.NEED,
        Category.WATER to Purpose.NEED,
        Category.INTERNET to Purpose.NEED,
        Category.MOBILE to Purpose.NEED,
        Category.FUEL to Purpose.NEED,
        Category.TRANSPORTATION_OTHER to Purpose.NEED,
        Category.MEDICINES to Purpose.NEED,
        Category.DOCTORS to Purpose.NEED,
        Category.INSURANCE to Purpose.NEED,

        Category.RESTAURANTS to Purpose.WANT,
        Category.FOOD_DELIVERY to Purpose.WANT,
        Category.AMAZON to Purpose.WANT,
        Category.FLIPKART to Purpose.WANT,
        Category.MYNTRA to Purpose.WANT,
        Category.SHOPPING_OTHER to Purpose.WANT,
        Category.RIDE_SHARING to Purpose.WANT,
        Category.MISCELLANEOUS to Purpose.WANT,

        Category.OTT to Purpose.LIFESTYLE,
        Category.MOVIES to Purpose.LIFESTYLE,
        Category.GAMES to Purpose.LIFESTYLE,

        Category.BOOKS to Purpose.GROWTH,
        Category.COURSES to Purpose.GROWTH,

        Category.INVESTMENT_OUTFLOW to Purpose.INVESTMENT,

        Category.EMI to Purpose.DEBT,
        Category.CREDIT_CARD_BILL to Purpose.DEBT
    )

    fun purposeFor(transaction: Transaction, overrides: List<MerchantPurposeRule> = emptyList()): Purpose {
        val override = overrides.firstOrNull { MerchantMatcher.isSameMerchant(it.merchantKey, transaction.merchant) }
        if (override != null) return override.purpose
        return defaultMapping[transaction.category] ?: Purpose.WANT
    }

    /** Total expense amount grouped by [Purpose], for "needs vs wants" style breakdowns. */
    fun purposeBreakdown(transactions: List<Transaction>, overrides: List<MerchantPurposeRule> = emptyList()): Map<Purpose, Double> =
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { purposeFor(it, overrides) }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
}
