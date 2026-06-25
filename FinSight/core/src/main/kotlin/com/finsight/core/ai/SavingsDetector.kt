package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.CategoryGroup
import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
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
        // Compare against a trailing 3-month average rather than just the previous month, so a
        // single unusually quiet month doesn't make a normal month look like a huge spike.
        val trailingMonths = (1..3).map { currentMonth.minusMonths(it.toLong()) }

        val spendByCategoryAndMonth = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) ->
                txs.groupBy { YearMonth.from(it.date.atZone(ZoneId.systemDefault()).toLocalDate()) }
                    .mapValues { (_, monthTxs) -> monthTxs.sumOf { it.amount } }
            }

        val opportunities = mutableListOf<SavingsOpportunity>()
        for ((category, monthTotals) in spendByCategoryAndMonth) {
            // TRANSFERS (ATM withdrawals, cash given to family) is money leaving the account, not a
            // spending category with a trend worth flagging as "excess spending".
            if (category.group == CategoryGroup.INCOME || category.group == CategoryGroup.TRANSFERS) continue
            val current = monthTotals[currentMonth] ?: continue

            // Skip categories that aren't recurring (e.g. a single movie outing or a one-off
            // purchase) - they don't have a meaningful "trend" to flag as excess spending.
            val monthsWithSpend = trailingMonths.count { (monthTotals[it] ?: 0.0) > 0 }
            if (monthsWithSpend < 2) continue

            val baseline = trailingMonths.sumOf { monthTotals[it] ?: 0.0 } / trailingMonths.size
            if (baseline <= 0) continue
            val increase = (current - baseline) / baseline
            if (increase >= EXCESS_SPEND_THRESHOLD) {
                val monthlyIncreaseAmount = current - baseline
                opportunities += SavingsOpportunity(
                    type = SavingsOpportunityType.EXCESS_SPENDING,
                    title = "${category.displayName} spending up ${"%.0f".format(increase * 100)}% this month",
                    description = "You spent Rs.${"%.0f".format(current)} on ${category.displayName} this month, " +
                        "vs a Rs.${"%.0f".format(baseline)} average over the prior 3 months. If this continues, " +
                        "that's about Rs.${"%.0f".format(monthlyIncreaseAmount * 12)} more per year.",
                    estimatedAnnualSavings = monthlyIncreaseAmount * 12
                )
            }
        }
        return opportunities
    }
}
