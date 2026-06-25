package com.finsight.core.ai

import com.finsight.core.model.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatCommandParserTest {

    @Test
    fun `recognizes a category-to-category reclassify command`() {
        val command = ChatCommandParser.parse("ATM Withdrawal is Given to Family")
        assertEquals(ChatCommand.ReclassifyCategory(Category.ATM_WITHDRAWAL, Category.GIVEN_TO_FAMILY), command)
    }

    @Test
    fun `recognizes an explicit 'tag X as Y' merchant reclassify command`() {
        val command = ChatCommandParser.parse("tag Swiggy as Food Delivery")
        assertEquals(ChatCommand.ReclassifyMerchant("Swiggy", Category.FOOD_DELIVERY), command)
    }

    @Test
    fun `falls back to null when the right-hand side is not a real category`() {
        assertNull(ChatCommandParser.parse("ATM SBI Main Branch is Mother Support"))
    }

    @Test
    fun `does not treat a finance question as a command`() {
        assertNull(ChatCommandParser.parse("How much did I spend on food this month?"))
    }
}
