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
        val clusters = mutableListOf<MutableList<Transaction>>()
        for (transaction in expenses) {
            val cluster = clusters.firstOrNull { MerchantMatcher.isSameMerchant(it.first().merchant, transaction.merchant) }
            if (cluster != null) cluster.add(transaction) else clusters.add(mutableListOf(transaction))
        }
        return clusters
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
