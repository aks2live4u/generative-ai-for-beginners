package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpenseForecasterTest {

    private val now = LocalDate.of(2024, 6, 15)

    @Test
    fun `forecasts mean and std-dev range from last 3 months of expenses`() {
        val transactions = listOf(
            tx(1000.0, Category.GROCERIES, TransactionType.EXPENSE, now.minusMonths(1)),
            tx(2000.0, Category.GROCERIES, TransactionType.EXPENSE, now.minusMonths(2)),
            tx(3000.0, Category.GROCERIES, TransactionType.EXPENSE, now.minusMonths(3))
        )
        val forecast = ExpenseForecaster.forecastNextMonth(transactions, now)
        assertEquals(3, forecast?.monthsUsed)
        assertEquals(2000.0, forecast?.expected)
        assertTrue(forecast!!.low < forecast.expected)
        assertTrue(forecast.high > forecast.expected)
    }

    @Test
    fun `returns null with no expense history`() {
        assertNull(ExpenseForecaster.forecastNextMonth(emptyList(), now))
    }

    @Test
    fun `forecasts income separately from expenses`() {
        val transactions = listOf(
            tx(50000.0, Category.SALARY, TransactionType.INCOME, now.minusMonths(1)),
            tx(1000.0, Category.GROCERIES, TransactionType.EXPENSE, now.minusMonths(1))
        )
        val incomeForecast = ExpenseForecaster.forecastNextMonthIncome(transactions, now)
        assertEquals(50000.0, incomeForecast?.expected)
    }
}
