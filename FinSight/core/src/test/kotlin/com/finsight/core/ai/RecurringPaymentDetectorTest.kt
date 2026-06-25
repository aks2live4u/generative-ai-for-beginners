package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecurringPaymentDetectorTest {

    @Test
    fun `detects a same-day-of-month, same-amount recurring payment`() {
        val transactions = listOf(
            tx(499.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 4, 5), merchant = "Netflix"),
            tx(499.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 5, 5), merchant = "Netflix"),
            tx(499.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 6, 5), merchant = "Netflix")
        )
        val detections = RecurringPaymentDetector.detect(transactions, now = LocalDate.of(2024, 6, 10))
        assertEquals(1, detections.size)
        val detection = detections.first()
        assertEquals("Netflix", detection.merchantLabel)
        assertEquals(499.0, detection.monthlyCost)
        assertEquals(LocalDate.of(2024, 7, 5), detection.nextExpectedDate)
        assertTrue(detection.confidence >= 65)
    }

    @Test
    fun `does not flag a one-off purchase as recurring`() {
        val transactions = listOf(
            tx(2500.0, Category.SHOPPING_OTHER, TransactionType.EXPENSE, LocalDate.of(2024, 6, 12), merchant = "Amazon")
        )
        assertEquals(emptyList<RecurringPaymentDetection>(), RecurringPaymentDetector.detect(transactions, now = LocalDate.of(2024, 6, 20)))
    }

    @Test
    fun `does not flag irregular amounts on the same merchant as recurring`() {
        val transactions = listOf(
            tx(300.0, Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2024, 4, 8), merchant = "Bigbasket"),
            tx(1200.0, Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2024, 5, 20), merchant = "Bigbasket")
        )
        assertEquals(emptyList<RecurringPaymentDetection>(), RecurringPaymentDetector.detect(transactions, now = LocalDate.of(2024, 6, 1)))
    }

    @Test
    fun `matches fuzzy merchant spelling variants across sources`() {
        val transactions = listOf(
            tx(199.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 4, 10), merchant = "Spotify"),
            tx(199.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 5, 10), merchant = "SPOTIFY*PREMIUM")
        )
        val detections = RecurringPaymentDetector.detect(transactions, now = LocalDate.of(2024, 5, 15))
        assertEquals(1, detections.size)
    }
}
