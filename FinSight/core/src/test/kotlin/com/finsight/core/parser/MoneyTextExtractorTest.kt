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

    @Test
    fun `flags an upcoming bill reminder as a payment reminder, not a transaction`() {
        assertEquals(true, MoneyTextExtractor.isPaymentReminder("Your standing instruction for Rs 3,500 is due tomorrow"))
    }

    @Test
    fun `does not flag a completed debit as a payment reminder`() {
        val text = "Rs.1,250.00 spent on your HDFC Bank Credit Card XX5678 at Amazon on 20-05-24"
        assertEquals(false, MoneyTextExtractor.isPaymentReminder(text))
    }

    @Test
    fun `does not capture the currency prefix as a merchant when no payee name is present`() {
        val text = "Rs.361.00 has been debited to Rs.361 towards Mobile Recharge on 25-Jun-26"
        assertEquals(null, MoneyTextExtractor.extractMerchant(text))
    }

    @Test
    fun `flags a credit card payment receipt confirmation, not a normal credit`() {
        val text = "We have received your payment of Rs.3,64,892.00 towards your Credit Card ending 6766. " +
            "Thank you for your payment."
        assertEquals(true, MoneyTextExtractor.isCardPaymentConfirmation(text))
    }

    @Test
    fun `does not flag an ordinary UPI credit as a card payment confirmation`() {
        val text = "Rs.5,000.00 credited to your account via UPI from John Doe on 25-Jun-26"
        assertEquals(false, MoneyTextExtractor.isCardPaymentConfirmation(text))
    }

    @Test
    fun `flags a 'credited to your credit card' bill payment receipt`() {
        // Real-world bank wording ("credited to", not "credited towards") for the credit-card side
        // of a bill payment the user already paid from their bank account - without this, it was
        // recorded as a second, spurious INCOME transaction for the same payment.
        val text = "Payment of Rs.50,000.00 has been credited to your HDFC Bank Credit Card ending 1234 " +
            "towards your bill. Thank you."
        assertEquals(true, MoneyTextExtractor.isCardPaymentConfirmation(text))
    }

    @Test
    fun `does not flag a genuine cashback credited to a credit card as a bill payment confirmation`() {
        val text = "Cashback of Rs.50.00 credited to your Credit Card ending 1234 on 25-Jun-26"
        assertEquals(false, MoneyTextExtractor.isCardPaymentConfirmation(text))
    }

    @Test
    fun `prefers the actual transaction amount over a leading total amount due figure`() {
        // Previously "total amount due" wasn't recognized as a balance/due-amount phrase (only the
        // shorter "total due" was), so when a statement-style message led with this figure, it was
        // mistaken for the real transaction amount instead of being skipped in favour of the
        // genuine spend that follows.
        val text = "Total Amount Due: Rs.5,73,749.00 as per your last statement. Rs.1,500.00 spent at Amazon on 20-Jun-26."
        assertEquals(1500.0, MoneyTextExtractor.extractAmount(text))
    }
}
