package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.Subscription
import com.finsight.core.model.TransactionType
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

    @Test
    fun `ATM withdrawals and cash given to family do not count as expenses`() {
        // A large cash withdrawal that's actually handed to a family member (or just unexplained
        // cash) must not tank the savings rate the same way INVESTMENT_OUTFLOW is excluded already.
        val now = LocalDate.of(2024, 6, 15)
        val transactions = listOf(
            tx(75000.0, Category.SALARY, TransactionType.INCOME, now),
            tx(5000.0, Category.GROCERIES, TransactionType.EXPENSE, now),
            tx(50000.0, Category.ATM_WITHDRAWAL, TransactionType.EXPENSE, now),
            tx(20000.0, Category.GIVEN_TO_FAMILY, TransactionType.EXPENSE, now)
        )
        val result = FinancialHealthScoreCalculator.calculate(
            transactions = transactions,
            subscriptions = emptyList(),
            liquidSavingsBalance = 0.0,
            now = now
        )
        val savingsFactor = result.factors.first { it.name == "Savings Rate" }
        assertTrue(savingsFactor.note.contains("93%"), "Expected ~93% savings rate ignoring transfers, got: ${savingsFactor.note}")
    }
}
