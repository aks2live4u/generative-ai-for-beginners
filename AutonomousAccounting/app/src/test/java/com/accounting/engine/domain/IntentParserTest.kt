package com.accounting.engine.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun `salary received of 1 lakh`() {
        val input = IntentParser.parse("Salary received of 1 lakh")
        requireNotNull(input)
        assertEquals(100_000.0, input.amount, 0.0001)
        assertTrue(input.intent is FinancialIntent.SalaryReceived)
    }

    @Test
    fun `received amount from a named payer`() {
        val input = IntentParser.parse("Received 2,000 from X")
        requireNotNull(input)
        assertEquals(2_000.0, input.amount, 0.0001)
        val intent = input.intent as FinancialIntent.PaymentReceived
        assertEquals("X", intent.sender)
    }

    @Test
    fun `loan given to a named recipient`() {
        val input = IntentParser.parse("Gave a loan of 2,002 to Y")
        requireNotNull(input)
        assertEquals(2_002.0, input.amount, 0.0001)
        val intent = input.intent as FinancialIntent.LoanGiven
        assertEquals("Y", intent.recipient)
    }

    @Test
    fun `credit card expense extracts trailing category, not the on-credit-card clause`() {
        val input = IntentParser.parse("Spent 2,000 on credit card for dining")
        requireNotNull(input)
        assertEquals(2_000.0, input.amount, 0.0001)
        val intent = input.intent as FinancialIntent.CreditCardExpense
        assertEquals("dining", intent.category)
    }

    @Test
    fun `credit card bill payment`() {
        val input = IntentParser.parse("Paid credit card bill 15,000")
        requireNotNull(input)
        assertEquals(15_000.0, input.amount, 0.0001)
        assertTrue(input.intent is FinancialIntent.CreditCardBillPayment)
    }

    @Test
    fun `contingency percentage leaves amount unresolved for the engine to compute`() {
        val input = IntentParser.parse("Set aside 10% for contingency")
        requireNotNull(input)
        assertEquals(0.0, input.amount, 0.0001)
        val intent = input.intent as FinancialIntent.ContingencyAllocation
        assertEquals(10.0, intent.percentage, 0.0001)
    }

    @Test
    fun `unrecognized input returns null`() {
        assertNull(IntentParser.parse("What's the weather today?"))
    }
}
