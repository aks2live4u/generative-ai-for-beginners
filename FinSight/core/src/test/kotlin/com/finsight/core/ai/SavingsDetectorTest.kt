package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.Subscription
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SavingsDetectorTest {

    @Test
    fun `flags subscription unused for over 45 days`() {
        val now = LocalDate.of(2024, 6, 15)
        val opportunities = SavingsDetector.detect(
            transactions = emptyList(),
            subscriptions = listOf(Subscription("Spotify", now.plusDays(5), 119.0, lastUsedDate = now.minusDays(60))),
            now = now
        )
        assertTrue(opportunities.any { it.type == SavingsOpportunityType.UNUSED_SUBSCRIPTION })
    }

    @Test
    fun `flags duplicate OTT subscriptions`() {
        val now = LocalDate.of(2024, 6, 15)
        val opportunities = SavingsDetector.detect(
            transactions = emptyList(),
            subscriptions = listOf(
                Subscription("Netflix", now.plusDays(5), 499.0, lastUsedDate = now.minusDays(1)),
                Subscription("Disney+ Hotstar", now.plusDays(10), 299.0, lastUsedDate = now.minusDays(1))
            ),
            now = now
        )
        assertTrue(opportunities.any { it.type == SavingsOpportunityType.DUPLICATE_SERVICE })
    }

    @Test
    fun `flags recurring category spend increase over 30 percent vs trailing 3-month average`() {
        val currentMonth = LocalDate.of(2024, 6, 10)
        val transactions = listOf(
            tx(1000.0, Category.FOOD_DELIVERY, TransactionType.EXPENSE, LocalDate.of(2024, 3, 10)),
            tx(1000.0, Category.FOOD_DELIVERY, TransactionType.EXPENSE, LocalDate.of(2024, 4, 10)),
            tx(1000.0, Category.FOOD_DELIVERY, TransactionType.EXPENSE, LocalDate.of(2024, 5, 10)),
            tx(1500.0, Category.FOOD_DELIVERY, TransactionType.EXPENSE, currentMonth)
        )
        val opportunities = SavingsDetector.detect(transactions, emptyList(), now = currentMonth)
        assertTrue(opportunities.any { it.type == SavingsOpportunityType.EXCESS_SPENDING })
    }

    @Test
    fun `does not flag a one-off purchase in a sporadic category`() {
        val currentMonth = LocalDate.of(2024, 6, 10)
        val transactions = listOf(
            tx(1500.0, Category.MOVIES, TransactionType.EXPENSE, currentMonth)
        )
        val opportunities = SavingsDetector.detect(transactions, emptyList(), now = currentMonth)
        assertTrue(opportunities.none { it.type == SavingsOpportunityType.EXCESS_SPENDING })
    }
}
