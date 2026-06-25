package com.finsight.core.ai.llm

import com.finsight.core.ai.FakeFinanceDataProvider
import com.finsight.core.ai.FinancialHealthFactor
import com.finsight.core.ai.FinancialHealthScore
import com.finsight.core.ai.SavingsOpportunity
import com.finsight.core.ai.SavingsOpportunityType
import com.finsight.core.ai.tx
import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FinanceContextBuilderTest {

    private val now = LocalDate.of(2024, 6, 15)

    @Test
    fun `chat context includes income, expense and category totals`() {
        val data = FakeFinanceDataProvider(
            listOf(
                tx(75000.0, Category.SALARY, TransactionType.INCOME, now),
                tx(30000.0, Category.GROCERIES, TransactionType.EXPENSE, now)
            )
        )
        val context = FinanceContextBuilder.buildChatContext(data, now)
        assertTrue(context.contains("75000"))
        assertTrue(context.contains("30000"))
        assertTrue(context.contains("Groceries"))
    }

    @Test
    fun `chat context never includes raw SMS text`() {
        val data = FakeFinanceDataProvider(
            listOf(tx(500.0, Category.GROCERIES, TransactionType.EXPENSE, now))
        )
        val context = FinanceContextBuilder.buildChatContext(data, now)
        assertFalse(context.contains("test"))
    }

    @Test
    fun `smart scan context redacts long account numbers in raw text`() {
        val transaction = tx(500.0, Category.GROCERIES, TransactionType.EXPENSE, now)
            .copy(rawText = "Rs.500 debited from A/c 123456789012")
        val context = FinanceContextBuilder.buildSmartScanContext(listOf(transaction))
        assertFalse(context.contains("123456789012"))
        assertTrue(context.contains("9012"))
    }

    @Test
    fun `health score context includes overall score and every factor note`() {
        val score = FinancialHealthScore(
            score = 72,
            interpretation = "Good",
            factors = listOf(FinancialHealthFactor("Savings Rate", 30, 40, "Saved 25% of income"))
        )
        val context = FinanceContextBuilder.buildHealthScoreContext(score)
        assertTrue(context.contains("72/100"))
        assertTrue(context.contains("Savings Rate"))
        assertTrue(context.contains("Saved 25% of income"))
    }

    @Test
    fun `savings opportunities context includes title and estimated savings`() {
        val opportunities = listOf(
            SavingsOpportunity(
                type = SavingsOpportunityType.UNUSED_SUBSCRIPTION,
                title = "Unused Netflix subscription",
                description = "No usage detected in 45 days",
                estimatedAnnualSavings = 1999.0
            )
        )
        val context = FinanceContextBuilder.buildSavingsOpportunitiesContext(opportunities)
        assertTrue(context.contains("Unused Netflix subscription"))
        assertTrue(context.contains("1999"))
    }
}
