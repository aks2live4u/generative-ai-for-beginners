package com.finsight.core.ai

import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import com.finsight.core.parser.MerchantMatcher
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/** A recurring expense (EMI/SIP/insurance/subscription/rent) discovered from transaction history, with no manual setup. */
data class RecurringPaymentDetection(
    val merchantLabel: String,
    val monthlyCost: Double,
    val nextExpectedDate: LocalDate,
    val occurrenceCount: Int,
    val confidence: Int
)

/**
 * Finds recurring monthly payments by grouping expenses on the same (fuzzy-matched) merchant and
 * checking whether both the amount and the day-of-month repeat closely enough across at least two
 * distinct months. Output feeds straight into the existing [com.finsight.core.model.Subscription]
 * storage - no manual "add a subscription" step needed.
 */
object RecurringPaymentDetector {

    private const val MIN_OCCURRENCES = 2
    private const val DAY_OF_MONTH_TOLERANCE = 4
    private const val AMOUNT_TOLERANCE_RATIO = 0.15

    fun detect(transactions: List<Transaction>, now: LocalDate = LocalDate.now()): List<RecurringPaymentDetection> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        return clusterByMerchant(expenses)
            .mapNotNull { group -> detectGroup(group, now) }
            .sortedByDescending { it.confidence }
    }

    /** Groups transactions by fuzzy merchant match (see [MerchantMatcher]) rather than exact spelling. */
    private fun clusterByMerchant(expenses: List<Transaction>): List<List<Transaction>> {
        // Each merchant name is normalized once up front rather than on every pairwise comparison -
        // with a real device's multi-thousand-transaction history this loop is O(n^2) in comparisons,
        // so re-normalizing (regex split/filter/join) on every single one made it slow enough to
        // block the UI thread for a long time.
        val normalizedClusters = mutableListOf<MutableList<Transaction>>()
        val clusterKeys = mutableListOf<String>()
        for (transaction in expenses) {
            val normalized = MerchantMatcher.normalize(transaction.merchant)
            val index = clusterKeys.indexOfFirst { MerchantMatcher.isSameNormalizedMerchant(it, normalized) }
            if (index >= 0) {
                normalizedClusters[index].add(transaction)
            } else {
                normalizedClusters.add(mutableListOf(transaction))
                clusterKeys.add(normalized)
            }
        }
        return normalizedClusters
    }

    private fun detectGroup(group: List<Transaction>, now: LocalDate): RecurringPaymentDetection? {
        val sorted = group.sortedBy { it.date }
        val dates = sorted.map { it.date.atZone(ZoneId.systemDefault()).toLocalDate() }
        val distinctMonths = dates.map { YearMonth.from(it) }.distinct()
        if (distinctMonths.size < MIN_OCCURRENCES) return null

        val avgDay = dates.map { it.dayOfMonth }.average()
        val dayConsistent = dates.all { abs(it.dayOfMonth - avgDay) <= DAY_OF_MONTH_TOLERANCE }
        if (!dayConsistent) return null

        val amounts = sorted.map { it.amount }
        val avgAmount = amounts.average()
        if (avgAmount <= 0) return null
        val amountConsistent = amounts.all { abs(it - avgAmount) / avgAmount <= AMOUNT_TOLERANCE_RATIO }
        if (!amountConsistent) return null

        val occurrences = distinctMonths.size
        val confidence = (50 + (occurrences - MIN_OCCURRENCES) * 15).coerceAtMost(95)

        return RecurringPaymentDetection(
            merchantLabel = sorted.last().merchant,
            monthlyCost = avgAmount,
            nextExpectedDate = nextOccurrenceAfter(dates.last(), avgDay.toInt().coerceIn(1, 28), now),
            occurrenceCount = occurrences,
            confidence = confidence
        )
    }

    private fun nextOccurrenceAfter(lastDate: LocalDate, targetDay: Int, now: LocalDate): LocalDate {
        var candidate = lastDate.withDayOfMonth(minOf(targetDay, lastDate.lengthOfMonth()))
        while (!candidate.isAfter(now)) {
            candidate = candidate.plusMonths(1)
            candidate = candidate.withDayOfMonth(minOf(targetDay, candidate.lengthOfMonth()))
        }
        return candidate
    }
}
