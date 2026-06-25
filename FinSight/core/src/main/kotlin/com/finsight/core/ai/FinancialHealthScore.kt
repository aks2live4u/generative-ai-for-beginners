package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.CategoryGroup
import com.finsight.core.model.Subscription
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class FinancialHealthFactor(val name: String, val score: Int, val maxScore: Int, val note: String)

data class FinancialHealthScore(
    val score: Int,
    val interpretation: String,
    val factors: List<FinancialHealthFactor>
)

/** Categories whose spend is discretionary/lifestyle rather than a necessity, for [scoreLifestyleInflation]. */
private val lifestyleCategories = setOf(
    Category.RESTAURANTS, Category.FOOD_DELIVERY,
    Category.AMAZON, Category.FLIPKART, Category.MYNTRA, Category.SHOPPING_OTHER,
    Category.MOVIES, Category.GAMES, Category.OTT
)

/**
 * Computes a 0-100 financial health score from the last 6 months of transactions, weighting:
 * savings rate (25), income stability (15), investment rate (15), spending consistency (10),
 * lifestyle inflation (10), subscription burden (10), emergency fund coverage (10), debt/EMI
 * ratio (5) — matching the factor list in the product blueprint.
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

        val factors = listOf(
            scoreSavingsRate(avgIncome, avgExpense),
            scoreIncomeStability(monthlyTotals.values.map { it.first }),
            scoreInvestmentRate(transactions, monthsToAnalyze, now, avgIncome),
            scoreSpendingConsistency(monthlyTotals.values.map { it.second }),
            scoreLifestyleInflation(transactions, monthsToAnalyze, now),
            scoreSubscriptionBurden(subscriptions, avgIncome),
            scoreEmergencyFund(liquidSavingsBalance, avgExpense),
            scoreDebtRatio(transactions, monthsToAnalyze, now, avgIncome)
        )
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
            // Money moved into investments (SIPs, mutual funds, etc.) is saved, not spent - counting
            // it as an expense here would double-penalize the savings rate for the exact behaviour
            // (investing) the score is supposed to reward.
            if (tx.category == Category.INVESTMENT_OUTFLOW) continue
            // ATM withdrawals and cash given to family are money leaving the account, not the
            // user's own spending - the SMS only confirms cash left, not what it was spent on (or
            // that it wasn't spent by the user at all), so counting it here would falsely tank the
            // savings rate for money the user never actually spent.
            if (tx.category.group == CategoryGroup.TRANSFERS) continue
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
        if (avgIncome <= 0) return FinancialHealthFactor("Savings Rate", 0, 25, "No income detected")
        val rate = ((avgIncome - avgExpense) / avgIncome * 100).coerceIn(-100.0, 100.0)
        val score = when {
            rate >= 30 -> 25
            rate >= 20 -> 20
            rate >= 10 -> 14
            rate >= 0 -> 8
            else -> 0
        }
        return FinancialHealthFactor("Savings Rate", score, 25, "Saving ${"%.0f".format(rate)}% of income")
    }

    private fun scoreIncomeStability(monthlyIncomes: List<Double>): FinancialHealthFactor {
        if (monthlyIncomes.size < 2) return FinancialHealthFactor("Income Stability", 15, 15, "Not enough history")
        val mean = monthlyIncomes.average()
        if (mean == 0.0) return FinancialHealthFactor("Income Stability", 3, 15, "No income recorded")
        val variance = monthlyIncomes.sumOf { (it - mean) * (it - mean) } / monthlyIncomes.size
        val coefficientOfVariation = Math.sqrt(variance) / mean
        val score = when {
            coefficientOfVariation <= 0.10 -> 15
            coefficientOfVariation <= 0.25 -> 11
            coefficientOfVariation <= 0.40 -> 7
            else -> 3
        }
        return FinancialHealthFactor("Income Stability", score, 15, "Month-to-month income variation ${"%.0f".format(coefficientOfVariation * 100)}%")
    }

    private fun scoreInvestmentRate(
        transactions: List<Transaction>,
        monthsToAnalyze: Int,
        now: LocalDate,
        avgIncome: Double
    ): FinancialHealthFactor {
        if (avgIncome <= 0) return FinancialHealthFactor("Investment Rate", 0, 15, "No income to compare against")
        val currentMonth = YearMonth.from(now)
        val cutoff = currentMonth.minusMonths((monthsToAnalyze - 1).toLong())
        val investmentTotal = transactions
            .filter { it.category == Category.INVESTMENT_OUTFLOW }
            .filter { YearMonth.from(it.date.atZone(ZoneId.systemDefault()).toLocalDate()) >= cutoff }
            .sumOf { it.amount }
        val avgMonthlyInvestment = investmentTotal / monthsToAnalyze
        val rate = avgMonthlyInvestment / avgIncome
        val score = when {
            rate >= 0.20 -> 15
            rate >= 0.10 -> 11
            rate >= 0.05 -> 7
            rate > 0 -> 3
            else -> 0
        }
        return FinancialHealthFactor("Investment Rate", score, 15, "Investing ${"%.1f".format(rate * 100)}% of income")
    }

    private fun scoreSpendingConsistency(monthlyExpenses: List<Double>): FinancialHealthFactor {
        if (monthlyExpenses.size < 2) return FinancialHealthFactor("Spending Consistency", 10, 10, "Not enough history")
        val mean = monthlyExpenses.average()
        if (mean == 0.0) return FinancialHealthFactor("Spending Consistency", 10, 10, "No spending recorded")
        val variance = monthlyExpenses.sumOf { (it - mean) * (it - mean) } / monthlyExpenses.size
        val stdDev = Math.sqrt(variance)
        val coefficientOfVariation = stdDev / mean
        val score = when {
            coefficientOfVariation <= 0.10 -> 10
            coefficientOfVariation <= 0.25 -> 7
            coefficientOfVariation <= 0.40 -> 4
            else -> 2
        }
        return FinancialHealthFactor("Spending Consistency", score, 10, "Month-to-month variation ${"%.0f".format(coefficientOfVariation * 100)}%")
    }

    private fun scoreLifestyleInflation(transactions: List<Transaction>, monthsToAnalyze: Int, now: LocalDate): FinancialHealthFactor {
        val currentMonth = YearMonth.from(now)
        val months = (0 until monthsToAnalyze).map { currentMonth.minusMonths(it.toLong()) }
        val lifestyleByMonth = months.associateWith { 0.0 }.toMutableMap()
        for (tx in transactions) {
            if (tx.category !in lifestyleCategories) continue
            val month = YearMonth.from(tx.date.atZone(ZoneId.systemDefault()).toLocalDate())
            if (month !in lifestyleByMonth) continue
            lifestyleByMonth[month] = lifestyleByMonth[month]!! + tx.amount
        }
        val sortedMonths = months.sorted()
        val half = sortedMonths.size / 2
        if (half == 0) return FinancialHealthFactor("Lifestyle Inflation", 10, 10, "Not enough history")
        val olderHalf = sortedMonths.take(half).map { lifestyleByMonth[it]!! }
        val recentHalf = sortedMonths.drop(half).map { lifestyleByMonth[it]!! }
        val olderAvg = olderHalf.average()
        val recentAvg = recentHalf.average()
        if (olderAvg <= 0.0) {
            return FinancialHealthFactor("Lifestyle Inflation", 10, 10, "No lifestyle spend history to compare")
        }
        val growth = (recentAvg - olderAvg) / olderAvg
        val score = when {
            growth <= 0 -> 10
            growth <= 0.10 -> 7
            growth <= 0.25 -> 4
            else -> 0
        }
        return FinancialHealthFactor("Lifestyle Inflation", score, 10, "Lifestyle spend ${"%.0f".format(growth * 100)}% vs earlier months")
    }

    private fun scoreSubscriptionBurden(subscriptions: List<Subscription>, avgIncome: Double): FinancialHealthFactor {
        val totalMonthlySubscriptionCost = subscriptions.sumOf { it.monthlyCost }
        if (avgIncome <= 0) return FinancialHealthFactor("Subscription Burden", 5, 10, "No income to compare against")
        val burdenRatio = totalMonthlySubscriptionCost / avgIncome
        val score = when {
            burdenRatio <= 0.02 -> 10
            burdenRatio <= 0.05 -> 7
            burdenRatio <= 0.10 -> 4
            else -> 1
        }
        return FinancialHealthFactor("Subscription Burden", score, 10, "${"%.1f".format(burdenRatio * 100)}% of income on subscriptions")
    }

    private fun scoreEmergencyFund(liquidSavingsBalance: Double, avgExpense: Double): FinancialHealthFactor {
        if (avgExpense <= 0) return FinancialHealthFactor("Emergency Fund", 10, 10, "No expense history")
        // liquidSavingsBalance is always 0 for now - FinSight doesn't track a separate savings/bank
        // balance yet, only transactions - so this factor always lands in the lowest tier. Say so
        // explicitly rather than showing a generic "0.0 months covered" that reads like a real
        // (bad) measurement.
        if (liquidSavingsBalance <= 0) {
            return FinancialHealthFactor("Emergency Fund", 1, 10, "Not tracked yet - FinSight doesn't record a savings balance")
        }
        val monthsCovered = liquidSavingsBalance / avgExpense
        val score = when {
            monthsCovered >= 6 -> 10
            monthsCovered >= 3 -> 7
            monthsCovered >= 1 -> 4
            else -> 1
        }
        return FinancialHealthFactor("Emergency Fund", score, 10, "${"%.1f".format(monthsCovered)} months of expenses covered")
    }

    private fun scoreDebtRatio(
        transactions: List<Transaction>,
        monthsToAnalyze: Int,
        now: LocalDate,
        avgIncome: Double
    ): FinancialHealthFactor {
        if (avgIncome <= 0) return FinancialHealthFactor("Debt Ratio", 2, 5, "No income to compare against")
        val emiCategories = setOf(
            com.finsight.core.model.Category.EMI,
            com.finsight.core.model.Category.CREDIT_CARD_BILL
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
            debtRatio <= 0.10 -> 5
            debtRatio <= 0.25 -> 3
            debtRatio <= 0.40 -> 1
            else -> 0
        }
        return FinancialHealthFactor("Debt Ratio", score, 5, "${"%.1f".format(debtRatio * 100)}% of income to EMIs/credit bills")
    }

    private fun interpret(score: Int): String = when {
        score >= 80 -> "Excellent financial health"
        score >= 60 -> "Good financial health"
        score >= 40 -> "Fair financial health, room to improve"
        else -> "Needs attention"
    }
}
