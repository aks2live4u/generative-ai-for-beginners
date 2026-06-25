package com.finsight.core.ai

import com.finsight.core.model.Category
import com.finsight.core.model.Purpose
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PurposeTaggerTest {

    @Test
    fun `defaults groceries to need and OTT to lifestyle`() {
        val groceries = tx(500.0, Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1))
        val ott = tx(199.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1))
        assertEquals(Purpose.NEED, PurposeTagger.purposeFor(groceries))
        assertEquals(Purpose.LIFESTYLE, PurposeTagger.purposeFor(ott))
    }

    @Test
    fun `defaults investment outflow and EMI`() {
        val sip = tx(5000.0, Category.INVESTMENT_OUTFLOW, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1))
        val emi = tx(8000.0, Category.EMI, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1))
        assertEquals(Purpose.INVESTMENT, PurposeTagger.purposeFor(sip))
        assertEquals(Purpose.DEBT, PurposeTagger.purposeFor(emi))
    }

    @Test
    fun `merchant override beats default category mapping`() {
        val mom = tx(2000.0, Category.SHOPPING_OTHER, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1), merchant = "Mom")
        val overrides = listOf(MerchantPurposeRule("Mom", Purpose.FAMILY))
        assertEquals(Purpose.FAMILY, PurposeTagger.purposeFor(mom, overrides))
    }

    @Test
    fun `purposeBreakdown sums expenses by purpose and ignores income`() {
        val transactions = listOf(
            tx(500.0, Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2024, 6, 1)),
            tx(300.0, Category.GROCERIES, TransactionType.EXPENSE, LocalDate.of(2024, 6, 2)),
            tx(199.0, Category.OTT, TransactionType.EXPENSE, LocalDate.of(2024, 6, 3)),
            tx(50000.0, Category.SALARY, TransactionType.INCOME, LocalDate.of(2024, 6, 1))
        )
        val breakdown = PurposeTagger.purposeBreakdown(transactions)
        assertEquals(800.0, breakdown[Purpose.NEED])
        assertEquals(199.0, breakdown[Purpose.LIFESTYLE])
        assertEquals(null, breakdown[Purpose.INVESTMENT])
    }
}
