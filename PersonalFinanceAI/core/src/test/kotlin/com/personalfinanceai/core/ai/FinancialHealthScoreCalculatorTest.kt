package com.personalfinanceai.core.ai

import com.personalfinanceai.core.model.Category
import com.personalfinanceai.core.model.Subscription
import com.personalfinanceai.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FinancialHealthScoreCalculatorTest {

    @Test
    fun `healthy saver scores high`() {
        val now = LocalDate.of(2024, 6, 15)
        val transactions = (0..5).flatMap { i ->
            val month = now.minusMonths(i.toLong()).withDayOfMonth(1)
            listOf(
                tx(75000.0, Category.SALARY, TransactionType.INCOME, month),
                tx(30000.0, Category.GROCERIES, TransactionType.EXPENSE, month.plusDays(2))
            )
        }
        val result = FinancialHealthScoreCalculator.calculate(
            transactions = transactions,
            subscriptions = listOf(Subscription("Netflix", now.plusDays(10), 199.0)),
            liquidSavingsBalance = 300000.0,
            now = now
        )
        assertTrue(result.score >= 70, "Expected high score for healthy saver, got ${result.score}")
    }

    @Test
    fun `no income results in low savings rate score`() {
        val now = LocalDate.of(2024, 6, 15)
        val transactions = listOf(tx(5000.0, Category.GROCERIES, TransactionType.EXPENSE, now))
        val result = FinancialHealthScoreCalculator.calculate(
            transactions = transactions,
            subscriptions = emptyList(),
            liquidSavingsBalance = 0.0,
            now = now
        )
        val savingsFactor = result.factors.first { it.name == "Savings Rate" }
        assertTrue(savingsFactor.score == 0)
    }
}
