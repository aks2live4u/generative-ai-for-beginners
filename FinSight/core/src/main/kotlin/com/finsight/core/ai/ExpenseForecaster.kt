package com.finsight.core.ai

import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/** A mean +/- one-standard-deviation forecast range, rather than a single naive average. */
data class MonthlyForecast(val expected: Double, val low: Double, val high: Double, val monthsUsed: Int)

/**
 * Forecasts next month's income/expenses from the last [MONTHS_OF_HISTORY] months. Reports a
 * mean +/- standard-deviation range instead of a flat average, since a single number reads as
 * falsely precise and hides how volatile the user's spending actually is.
 */
object ExpenseForecaster {

    private const val MONTHS_OF_HISTORY = 3

    fun forecastNextMonth(transactions: List<Transaction>, now: LocalDate): MonthlyForecast? =
        forecast(transactions, now, TransactionType.EXPENSE)

    fun forecastNextMonthIncome(transactions: List<Transaction>, now: LocalDate): MonthlyForecast? =
        forecast(transactions, now, TransactionType.INCOME)

    private fun forecast(transactions: List<Transaction>, now: LocalDate, type: TransactionType): MonthlyForecast? {
        val months = (1..MONTHS_OF_HISTORY).map { YearMonth.from(now.minusMonths(it.toLong())) }
        val totals = months.mapNotNull { month ->
            val total = transactions
                .filter { it.type == type }
                .filter { YearMonth.from(it.date.atZone(java.time.ZoneId.systemDefault()).toLocalDate()) == month }
                .sumOf { it.amount }
            if (total > 0) total else null
        }
        if (totals.isEmpty()) return null
        val mean = totals.average()
        val variance = totals.sumOf { (it - mean) * (it - mean) } / totals.size
        val stdDev = Math.sqrt(variance)
        return MonthlyForecast(
            expected = mean,
            low = (mean - stdDev).coerceAtLeast(0.0),
            high = mean + stdDev,
            monthsUsed = totals.size
        )
    }
}
