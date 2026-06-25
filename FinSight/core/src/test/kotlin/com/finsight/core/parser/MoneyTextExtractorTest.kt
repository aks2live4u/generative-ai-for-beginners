package com.finsight.core.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoneyTextExtractorTest {

    @Test
    fun `extracts the only amount present`() {
        assertEquals(599.0, MoneyTextExtractor.extractAmount("INR 599.00 debited via UPI to Swiggy"))
    }

    @Test
    fun `prefers the transaction amount over a trailing available balance figure`() {
        val text = "Rs.500.00 debited from A/c XX1234 on 01-Jun-24. Avl Bal Rs.84,011.80"
        assertEquals(500.0, MoneyTextExtractor.extractAmount(text))
    }

    @Test
    fun `prefers the transaction amount over a leading credit limit figure`() {
        val text = "Your Credit Limit Rs.8,40,118.00 is available. Rs.1,200.00 spent at Amazon on 02-Jun-24"
        assertEquals(1200.0, MoneyTextExtractor.extractAmount(text))
    }

    @Test
    fun `falls back to the first amount when every candidate is in a balance context`() {
        val text = "Avl Bal Rs.84,011.80 as of today"
        assertEquals(84011.80, MoneyTextExtractor.extractAmount(text))
    }

    @Test
    fun `handles lakh-style comma grouping`() {
        assertEquals(412922.0, MoneyTextExtractor.extractAmount("Rs.4,12,922 EMI debited from your account"))
    }

    @Test
    fun `returns null when no amount is present`() {
        assertEquals(null, MoneyTextExtractor.extractAmount("Your OTP is 123456"))
    }

    @Test
    fun `extracts payee name preceding 'credited' with no preposition`() {
        val text = "ICICI Bank Acct XX295 debited for Rs 50.00 on 25-Jun-26; HUMANAMAINA NIK credited. UPI:654268147288"
        assertEquals("HUMANAMAINA NIK", MoneyTextExtractor.extractMerchant(text))
    }

    @Test
    fun `flags standing instruction activation as a mandate registration, not a transaction`() {
        val text = "We have activated Standing Instruction on ICICI Bank Credit Card 6003. Merchant: Google Cloud, " +
            "Maximum Amount: INR 75000.00, Mandate ID: YVyxZWKrOL"
        assertEquals(true, MoneyTextExtractor.isMandateRegistration(text))
    }

    @Test
    fun `does not flag a normal debit message as a mandate registration`() {
        val text = "Rs.1,250.00 spent on your HDFC Bank Credit Card XX5678 at Amazon on 20-05-24"
        assertEquals(false, MoneyTextExtractor.isMandateRegistration(text))
    }
}
