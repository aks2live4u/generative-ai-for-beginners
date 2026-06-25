package com.finsight.core.parser

import com.finsight.core.model.Category
import com.finsight.core.model.PaymentMethod
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class SmsTransactionParserTest {

    private val now = Instant.parse("2024-05-22T10:15:30Z")

    @Test
    fun `parses UPI debit to Swiggy as food delivery expense`() {
        val result = SmsTransactionParser.parse(
            "INR 599.00 debited via UPI to Swiggy on 22-05-24. UPI Ref 123456789",
            now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(599.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(PaymentMethod.UPI, tx.paymentMethod)
        assertEquals(Category.FOOD_DELIVERY, tx.category)
    }

    @Test
    fun `parses salary credit as income`() {
        val result = SmsTransactionParser.parse(
            "Your A/c XX1234 is credited with Rs.75,000.00 on 01-May-24 by SALARY-ACME CORP",
            now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(75000.0, tx.amount)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(Category.SALARY, tx.category)
    }

    @Test
    fun `parses credit card spend at Amazon`() {
        val result = SmsTransactionParser.parse(
            "Rs.1,250.00 spent on your HDFC Bank Credit Card XX5678 at Amazon on 20-05-24",
            now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(1250.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(PaymentMethod.CREDIT_CARD, tx.paymentMethod)
        assertEquals(Category.AMAZON, tx.category)
    }

    @Test
    fun `parses bank debit via net banking for electricity bill`() {
        val result = SmsTransactionParser.parse(
            "Rs.2200.00 debited from A/c XX9876 via NetBanking to BESCOM Electricity on 15-05-24",
            now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(PaymentMethod.NET_BANKING, tx.paymentMethod)
        assertEquals(Category.ELECTRICITY, tx.category)
    }

    @Test
    fun `rejects OTP messages as not financial`() {
        val result = SmsTransactionParser.parse(
            "123456 is your OTP for transaction. Do not share with anyone.",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }

    @Test
    fun `rejects promotional messages with no debit-credit keyword`() {
        val result = SmsTransactionParser.parse(
            "Get Rs.500 cashback... wait actually just generic promo with no amount keyword",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }

    @Test
    fun `rejects loan-offer scam message even though it contains 'received' and an amount`() {
        val result = SmsTransactionParser.parse(
            "Service update: we have received your payment and you can get a personal loan offer of Rs.4,00,000.",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }

    @Test
    fun `rejects pre-approved loan spam`() {
        val result = SmsTransactionParser.parse(
            "Congratulations! You are pre-approved for an instant loan of Rs.50,000. Apply now, click here.",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }

    @Test
    fun `rejects standing instruction activation notice as not financial`() {
        val result = SmsTransactionParser.parse(
            "We have activated Standing Instruction on ICICI Bank Credit Card 6003. Merchant: Google Cloud, " +
                "Maximum Amount: INR 75000.00, Frequency: As Presented, Start Date: 25/06/2026, " +
                "End Date: 31/12/2036, Mandate ID: YVyxZWKrOL",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }

    @Test
    fun `rejects e-mandate registration notice as not financial`() {
        val result = SmsTransactionParser.parse(
            "Your e-mandate for Rs.999.00 has been registered with Netflix on your HDFC Bank account.",
            now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }
}
