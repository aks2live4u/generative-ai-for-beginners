package com.finsight.core.parser

import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class EmailTransactionParserTest {

    private val now = Instant.parse("2024-05-22T10:15:30Z")

    @Test
    fun `parses amazon order confirmation email as shopping expense`() {
        val result = EmailTransactionParser.parse(
            senderEmail = "auto-confirm@amazon.in",
            subject = "Your Amazon.in order has shipped",
            bodySnippet = "Order total Rs.2399.00 for 1 item",
            receivedAt = now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(2399.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(Category.AMAZON, tx.category)
    }

    @Test
    fun `parses payroll email as income`() {
        val result = EmailTransactionParser.parse(
            senderEmail = "hr@acmecorp.com",
            subject = "Payslip for May 2024",
            bodySnippet = "Net pay credited Rs.75000.00",
            receivedAt = now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(TransactionType.INCOME, tx.type)
    }

    @Test
    fun `rejects newsletter with no amount`() {
        val result = EmailTransactionParser.parse(
            senderEmail = "news@example.com",
            subject = "Top 10 deals this week",
            bodySnippet = "Check out our weekly roundup",
            receivedAt = now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }
}
