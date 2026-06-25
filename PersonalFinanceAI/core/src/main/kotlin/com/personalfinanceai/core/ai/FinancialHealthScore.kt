package com.personalfinanceai.core.ai

import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class FinancialHealthFactor(val name: String, val score: Int, val maxScore: Int, val note: String)

data class FinancialHealthScore(
    val score: Int,
    val interpretation: String,
    val factors: List<FinancialHealthFactor>
)

/**
 * Computes a 0-100 financial health score from the last 6 months of transactions, weighting:
 * savings rate (40), spending consistency (20), subscription burden (15), emergency fund
 * coverage (15), debt/EMI ratio (10) — matching the factor list in the product blueprint.
 */
object FinancialHealthScoreCalculator {

    fun calculate(
        transactions: List<Transaction>,
        subscriptions: List<Subscription>,
        liquidSavingsBalance: Double,
        monthsToAnalyze: Int = 6,
        now: LocalDate = LocalDate.now()
    ): FinancialHealthScore {
        val monthlyTotals = monthlyIncomeExpense(transactions, monthsToAnalyze, now)
        val avgIncome = monthlyTotals.values.map { it.first }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgExpense = monthlyTotals.values.map { it.second }.average().takeIf { !it.isNaN() } ?: 0.0

        val savingsRateFactor = scoreSavingsRate(avgIncome, avgExpense)
        val consistencyFactor = scoreSpendingConsistency(monthlyTotals.values.map { it.second })
        val subscriptionFactor = scoreSubscriptionBurden(subscriptions, avgIncome)
        val emergencyFundFactor = scoreEmergencyFund(liquidSavingsBalance, avgExpense)
        val debtFactor = scoreDebtRatio(transactions, monthsToAnalyze, now, avgIncome)

        val factors = listOf(savingsRateFactor, consistencyFactor, subscriptionFactor, emergencyFundFactor, debtFactor)
        val total = factors.sumOf { it.score }

        return FinancialHealthScore(
            score = total,
            interpretation = interpret(total),
            factors = factors
        )
    }

    private fun monthlyIncomeExpense(
        transactions: List<Transaction>,
        monthsToAnalyze: Int,
        now: LocalDate
    ): Map<YearMonth, Pair<Double, Double>> {
        val currentMonth = YearMonth.from(now)
        val months = (0 until monthsToAnalyze).map { currentMonth.minusMonths(it.toLong()) }
        val result = months.associateWith { 0.0 to 0.0 }.toMutableMap()
        for (tx in transactions) {
            val month = YearMonth.from(tx.date.atZone(ZoneId.systemDefault()).toLocalDate())
            if (month !in result) continue
            val (income, expense) = result[month]!!
            result[month] = if (tx.type == TransactionType.INCOME) {
                (income + tx.amount) to expense
            } else {
                income to (expense + tx.amount)
            }
        }
        return result
    }

    private fun scoreSavingsRate(avgIncome: Double, avgExpense: Double): FinancialHealthFactor {
        if (avgIncome <= 0) return FinancialHealthFactor("Savings Rate", 0, 40, "No income detected")
        val rate = ((avgIncome - avgExpense) / avgIncome * 100).coerceIn(-100.0, 100.0)
        val score = when {
            rate >= 30 -> 40
            rate >= 20 -> 32
            rate >= 10 -> 24
            rate >= 0 -> 14
            else -> 0
        }
        return FinancialHealthFactor("Savings Rate", score, 40, "Saving ${"%.0f".format(rate)}% of income")
    }

    private fun scoreSpendingConsistency(monthlyExpenses: List<Double>): FinancialHealthFactor {
        if (monthlyExpenses.size < 2) return FinancialHealthFactor("Spending Consistency", 20, 20, "Not enough history")
        val mean = monthlyExpenses.average()
        if (mean == 0.0) return FinancialHealthFactor("Spending Consistency", 20, 20, "No spending recorded")
        val variance = monthlyExpenses.sumOf { (it - mean) * (it - mean) } / monthlyExpenses.size
        val stdDev = Math.sqrt(variance)
        val coefficientOfVariation = stdDev / mean
        val score = when {
            coefficientOfVariation <= 0.10 -> 20
            coefficientOfVariation <= 0.25 -> 15
            coefficientOfVariation <= 0.40 -> 10
            else -> 5
        }
        return FinancialHealthFactor("Spending Consistency", score, 20, "Month-to-month variation ${"%.0f".format(coefficientOfVariation * 100)}%")
    }

    private fun scoreSubscriptionBurden(subscriptions: List<Subscription>, avgIncome: Double): FinancialHealthFactor {
        val totalMonthlySubscriptionCost = subscriptions.sumOf { it.monthlyCost }
        if (avgIncome <= 0) return FinancialHealthFactor("Subscription Burden", 7, 15, "No income to compare against")
        val burdenRatio = totalMonthlySubscriptionCost / avgIncome
        val score = when {
            burdenRatio <= 0.02 -> 15
            burdenRatio <= 0.05 -> 11
            burdenRatio <= 0.10 -> 7
            else -> 2
        }
        return FinancialHealthFactor("Subscription Burden", score, 15, "${"%.1f".format(burdenRatio * 100)}% of income on subscriptions")
    }

    private fun scoreEmergencyFund(liquidSavingsBalance: Double, avgExpense: Double): FinancialHealthFactor {
        if (avgExpense <= 0) return FinancialHealthFactor("Emergency Fund", 15, 15, "No expense history")
        val monthsCovered = liquidSavingsBalance / avgExpense
        val score = when {
            monthsCovered >= 6 -> 15
            monthsCovered >= 3 -> 11
            monthsCovered >= 1 -> 6
            else -> 2
        }
        return FinancialHealthFactor("Emergency Fund", score, 15, "${"%.1f".format(monthsCovered)} months of expenses covered")
    }

    private fun scoreDebtRatio(
        transactions: List<Transaction>,
        monthsToAnalyze: Int,
        now: LocalDate,
        avgIncome: Double
    ): FinancialHealthFactor {
        if (avgIncome <= 0) return FinancialHealthFactor("Debt Ratio", 5, 10, "No income to compare against")
        val emiCategories = setOf(
            com.personalfinanceai.core.model.Category.EMI,
            com.personalfinanceai.core.model.Category.CREDIT_CARD_BILL
        )
        val currentMonth = YearMonth.from(now)
        val cutoff = currentMonth.minusMonths((monthsToAnalyze - 1).toLong())
        val emiTotal = transactions
            .filter { it.category in emiCategories }
            .filter { YearMonth.from(it.date.atZone(ZoneId.systemDefault()).toLocalDate()) >= cutoff }
            .sumOf { it.amount }
        val avgMonthlyEmi = emiTotal / monthsToAnalyze
        val debtRatio = avgMonthlyEmi / avgIncome
        val score = when {
            debtRatio <= 0.10 -> 10
            debtRatio <= 0.25 -> 7
            debtRatio <= 0.40 -> 4
            else -> 0
        }
        return FinancialHealthFactor("Debt Ratio", score, 10, "${"%.1f".format(debtRatio * 100)}% of income to EMIs/credit bills")
    }

    private fun interpret(score: Int): String = when {
        score >= 80 -> "Excellent financial health"
        score >= 60 -> "Good financial health"
        score >= 40 -> "Fair financial health, room to improve"
        else -> "Needs attention"
    }
}
