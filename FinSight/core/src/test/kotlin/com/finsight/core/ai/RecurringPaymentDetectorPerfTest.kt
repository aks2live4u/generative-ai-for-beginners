package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.system.measureTimeMillis

/**
 * Regression guard for a real-device ANR: with ~1600 transactions, O(n^2) merchant clustering
 * combined with per-comparison regex recompilation took long enough to freeze the UI thread and
 * get the app killed. Synthetic test fixtures elsewhere in this suite are too small to ever
 * exercise that path, so this test exists purely to catch a return of the slowdown.
 */
class RecurringPaymentDetectorPerfTest {

    @Test
    fun `detect stays fast at realistic device scale`() {
        val merchantNames = (1..200).map { "Merchant $it Pvt Ltd Payments" }
        val transactions = (0 until 1600).map { i ->
            val merchant = merchantNames[i % merchantNames.size]
            tx(
                amount = 100.0 + (i % 50),
                category = Category.SHOPPING_OTHER,
                type = TransactionType.EXPENSE,
                date = LocalDate.of(2024, 1 + (i % 6), 1 + (i % 28)),
                merchant = merchant
            )
        }

        val elapsed = measureTimeMillis {
            RecurringPaymentDetector.detect(transactions, now = LocalDate.of(2024, 12, 31))
        }

        assert(elapsed < 5000) { "detect() took ${elapsed}ms for ${transactions.size} transactions - regressed back to O(n^2) territory" }
    }
}
