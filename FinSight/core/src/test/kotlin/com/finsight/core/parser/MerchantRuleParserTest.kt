package com.finsight.core.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MerchantRuleParserTest {

    @Test
    fun `parses a plain 'X is Y' teaching command`() {
        val rule = MerchantRuleParser.parse("ATM SBI Main Branch is Mother Support")
        assertEquals(MerchantRule("ATM SBI Main Branch", "Mother Support"), rule)
    }

    @Test
    fun `parses an explicit 'tag X as Y' command`() {
        val rule = MerchantRuleParser.parse("tag Swiggy as Food Delivery")
        assertEquals(MerchantRule("Swiggy", "Food Delivery"), rule)
    }

    @Test
    fun `parses an explicit 'label X as Y' command`() {
        val rule = MerchantRuleParser.parse("label Uber as Commute")
        assertEquals(MerchantRule("Uber", "Commute"), rule)
    }

    @Test
    fun `does not treat a finance question as a teaching command`() {
        assertNull(MerchantRuleParser.parse("How much did I spend on food this month?"))
    }

    @Test
    fun `does not treat a 'what is' question as a teaching command`() {
        assertNull(MerchantRuleParser.parse("What is my balance?"))
    }

    @Test
    fun `ignores a generic pronoun subject`() {
        assertNull(MerchantRuleParser.parse("This is great"))
    }

    @Test
    fun `ignores plain chit-chat with no 'is' or explicit verb`() {
        assertNull(MerchantRuleParser.parse("Hey, are we still on for lunch?"))
    }
}
