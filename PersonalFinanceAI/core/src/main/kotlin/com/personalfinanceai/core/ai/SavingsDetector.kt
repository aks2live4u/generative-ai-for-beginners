package com.personalfinanceai.core.ai

import com.personalfinanceai.core.model.Category
import com.personalfinanceai.core.model.CategoryGroup
import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class SavingsOpportunityType { UNUSED_SUBSCRIPTION, DUPLICATE_SERVICE, EXCESS_SPENDING }

data class SavingsOpportunity(
    val type: SavingsOpportunityType,
    val title: String,
    val description: String,
    val estimatedAnnualSavings: Double
)

/**
 * Heuristic savings detector: flags subscriptions unused for 45+ days, duplicate subscriptions
 * within the same category (e.g. two OTT services), and categories whose spend rose sharply
 * month-over-month.
 */
object SavingsDetector {

    private const val UNUSED_THRESHOLD_DAYS = 45
    private const val EXCESS_SPEND_THRESHOLD = 0.30 // 30% increase vs previous month

    fun detect(
        transactions: List<Transaction>,
        subscriptions: List<Subscription>,
        now: LocalDate = LocalDate.now()
    ): List<SavingsOpportunity> {
        val opportunities = mutableListOf<SavingsOpportunity>()
        opportunities += detectUnusedSubscriptions(subscriptions, now)
        opportunities += detectDuplicateServices(subscriptions)
        opportunities += detectExcessSpending(transactions, now)
        return opportunities.sortedByDescending { it.estimatedAnnualSavings }
    }

    private fun detectUnusedSubscriptions(subscriptions: List<Subscription>, now: LocalDate): List<SavingsOpportunity> =
        subscriptions
            .filter { sub ->
                val lastUsed = sub.lastUsedDate
                lastUsed == null || java.time.temporal.ChronoUnit.DAYS.between(lastUsed, now) >= UNUSED_THRESHOLD_DAYS
            }
            .map { sub ->
                SavingsOpportunity(
                    type = SavingsOpportunityType.UNUSED_SUBSCRIPTION,
                    title = "${sub.serviceName} appears unused",
                    description = "No usage detected in the last $UNUSED_THRESHOLD_DAYS+ days, but you're still being charged Rs.${"%.0f".format(sub.monthlyCost)}/month.",
                    estimatedAnnualSavings = sub.monthlyCost * 12
                )
            }

    private fun detectDuplicateServices(subscriptions: List<Subscription>): List<SavingsOpportunity> {
        val ottServiceNames = setOf("netflix", "hotstar", "prime video", "sonyliv", "zee5", "sun nxt", "jiocinema")
        val ottSubs = subscriptions.filter { sub -> ottServiceNames.any { sub.serviceName.lowercase().contains(it) } }
        if (ottSubs.size <= 1) return emptyList()
        val cheapest = ottSubs.minByOrNull { it.monthlyCost }
        val redundant = ottSubs.filter { it != cheapest }
        return listOf(
            SavingsOpportunity(
                type = SavingsOpportunityType.DUPLICATE_SERVICE,
                title = "Multiple OTT subscriptions detected",
                description = "You're paying for ${ottSubs.joinToString(", ") { it.serviceName }}. Consider keeping just one.",
                estimatedAnnualSavings = redundant.sumOf { it.monthlyCost } * 12
            )
        )
    }

    private fun detectExcessSpending(transactions: List<Transaction>, now: LocalDate): List<SavingsOpportunity> {
        val currentMonth = YearMonth.from(now)
        val previousMonth = currentMonth.minusMonths(1)

        val spendByCategoryAndMonth = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) ->
                txs.groupBy { YearMonth.from(it.date.atZone(ZoneId.systemDefault()).toLocalDate()) }
                    .mapValues { (_, monthTxs) -> monthTxs.sumOf { it.amount } }
            }

        val opportunities = mutableListOf<SavingsOpportunity>()
        for ((category, monthTotals) in spendByCategoryAndMonth) {
            if (category.group == CategoryGroup.INCOME) continue
            val current = monthTotals[currentMonth] ?: continue
            val previous = monthTotals[previousMonth] ?: continue
            if (previous <= 0) continue
            val increase = (current - previous) / previous
            if (increase >= EXCESS_SPEND_THRESHOLD) {
                opportunities += SavingsOpportunity(
                    type = SavingsOpportunityType.EXCESS_SPENDING,
                    title = "${category.displayName} spending increased ${"%.0f".format(increase * 100)}%",
                    description = "You spent Rs.${"%.0f".format(current)} on ${category.displayName} this month, up from Rs.${"%.0f".format(previous)} last month.",
                    estimatedAnnualSavings = (current - previous) * 12
                )
            }
        }
        return opportunities
    }
}
