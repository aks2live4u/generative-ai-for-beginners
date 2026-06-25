package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.Subscription
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ChatAssistantEngineTest {

    private val now = LocalDate.of(2024, 6, 15)

    @Test
    fun `answers spend on food last month`() {
        val lastMonth = now.minusMonths(1).withDayOfMonth(10)
        val data = FakeFinanceDataProvider(
            listOf(
                tx(599.0, Category.FOOD_DELIVERY, TransactionType.EXPENSE, lastMonth),
                tx(200.0, Category.GROCERIES, TransactionType.EXPENSE, lastMonth)
            )
        )
        val answer = ChatAssistantEngine.answer("How much did I spend on food last month?", data, now)
        assertTrue(answer.contains("799"))
    }

    @Test
    fun `answers what subscriptions am I paying for`() {
        val data = FakeFinanceDataProvider(
            emptyList(),
            listOf(Subscription("Netflix", now.plusDays(5), 499.0))
        )
        val answer = ChatAssistantEngine.answer("What subscriptions am I paying for?", data, now)
        assertTrue(answer.contains("Netflix"))
    }

    @Test
    fun `answers how much did I spend on amazon`() {
        val data = FakeFinanceDataProvider(
            listOf(tx(2399.0, Category.AMAZON, TransactionType.EXPENSE, now, merchant = "Amazon"))
        )
        val answer = ChatAssistantEngine.answer("How much did I spend on Amazon?", data, now)
        assertTrue(answer.contains("2399"))
    }

    @Test
    fun `answers where can I save money`() {
        val data = FakeFinanceDataProvider(
            emptyList(),
            listOf(Subscription("Spotify", now.plusDays(5), 119.0, lastUsedDate = now.minusDays(90)))
        )
        val answer = ChatAssistantEngine.answer("Where can I save money?", data, now)
        assertTrue(answer.contains("Spotify"))
    }

    @Test
    fun `answers summarize my finances`() {
        val data = FakeFinanceDataProvider(
            listOf(
                tx(75000.0, Category.SALARY, TransactionType.INCOME, now),
                tx(30000.0, Category.GROCERIES, TransactionType.EXPENSE, now)
            )
        )
        val answer = ChatAssistantEngine.answer("Summarize my finances", data, now)
        assertTrue(answer.contains("75000"))
        assertTrue(answer.contains("30000"))
    }
}
