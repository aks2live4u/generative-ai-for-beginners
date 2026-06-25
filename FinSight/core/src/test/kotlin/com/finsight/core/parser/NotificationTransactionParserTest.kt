package com.finsight.core.parser

import com.finsight.core.model.Category
import com.finsight.core.model.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationTransactionParserTest {

    private val now = Instant.parse("2024-05-22T10:15:30Z")

    @Test
    fun `parses Swiggy order notification`() {
        val result = NotificationTransactionParser.parse(
            packageName = "in.swiggy.android",
            title = "Order delivered!",
            text = "Your order total was Rs.349.00. Enjoy your meal!",
            postedAt = now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(349.0, tx.amount)
        assertEquals("Swiggy", tx.merchant)
        assertEquals(Category.FOOD_DELIVERY, tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
    }

    @Test
    fun `parses Uber ride notification`() {
        val result = NotificationTransactionParser.parse(
            packageName = "com.ubercab",
            title = "Trip completed",
            text = "You paid Rs.220.00 for your trip",
            postedAt = now
        )
        val tx = (result as ParseResult.Success).transaction
        assertEquals(Category.RIDE_SHARING, tx.category)
    }

    @Test
    fun `ignores notifications from unsupported apps`() {
        val result = NotificationTransactionParser.parse(
            packageName = "com.whatsapp",
            title = "New message",
            text = "Hey, are you free for Rs.500 lunch?",
            postedAt = now
        )
        assertTrue(result is ParseResult.NotFinancial)
    }
}
