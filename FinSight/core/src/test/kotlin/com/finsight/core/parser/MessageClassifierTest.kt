package com.finsight.core.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageClassifierTest {

    @Test
    fun `classifies a well-formed debit with merchant as a high-confidence transaction`() {
        val result = MessageClassifier.classify("INR 599.00 debited via UPI to Swiggy on 22-05-24. UPI Ref 123456789")
        assertEquals(MessageType.TRANSACTION, result.type)
        assertTrue(result.confidence >= 90)
    }

    @Test
    fun `classifies an amount with no extractable merchant as a lower-confidence transaction`() {
        val result = MessageClassifier.classify("Rs.500.00 debited from your account on 01-Jun-24")
        assertEquals(MessageType.TRANSACTION, result.type)
        assertTrue(result.confidence < 90)
    }

    @Test
    fun `classifies an OTP message`() {
        val result = MessageClassifier.classify("123456 is your OTP for transaction. Do not share with anyone.")
        assertEquals(MessageType.OTP, result.type)
    }

    @Test
    fun `classifies a promotional message as spam`() {
        val result = MessageClassifier.classify("Congratulations! You are pre-approved for an instant loan of Rs.50,000. Apply now.")
        assertEquals(MessageType.SPAM, result.type)
    }

    @Test
    fun `classifies a standing instruction activation as a reminder`() {
        val text = "We have activated Standing Instruction on ICICI Bank Credit Card 6003. Merchant: Google Cloud, " +
            "Maximum Amount: INR 75000.00, Mandate ID: YVyxZWKrOL"
        val result = MessageClassifier.classify(text)
        assertEquals(MessageType.REMINDER, result.type)
    }

    @Test
    fun `classifies an upcoming due-date notice as a reminder`() {
        val result = MessageClassifier.classify("Your standing instruction for Rs 3,500 is due tomorrow")
        assertEquals(MessageType.REMINDER, result.type)
    }

    @Test
    fun `classifies text with no amount as unrecognized`() {
        val result = MessageClassifier.classify("Hey, are we still on for lunch?")
        assertEquals(MessageType.UNRECOGNIZED, result.type)
    }

    @Test
    fun `classifies a coupon credit as spam, not income`() {
        val result = MessageClassifier.classify("Rs 1000 coupon credited to your Zepto account. Use it before it expires!")
        assertEquals(MessageType.SPAM, result.type)
    }
}
