package com.finsight.core.ai.llm

import com.finsight.core.ai.FakeFinanceDataProvider
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
}
