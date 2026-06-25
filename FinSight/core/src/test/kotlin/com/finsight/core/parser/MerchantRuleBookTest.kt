package com.finsight.core.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MerchantRuleBookTest {

    private val rules = listOf(
        MerchantRule("ATM SBI Main Branch", "Mother Support"),
        MerchantRule("Swiggy", "Food Delivery")
    )

    @Test
    fun `resolves a label for an exact merchant match`() {
        assertEquals("Mother Support", MerchantRuleBook.resolveLabel(rules, "ATM SBI Main Branch"))
    }

    @Test
    fun `resolves a label for a fuzzy merchant variant`() {
        assertEquals("Food Delivery", MerchantRuleBook.resolveLabel(rules, "SWIGGY*ORDER"))
    }

    @Test
    fun `returns null when no rule matches`() {
        assertNull(MerchantRuleBook.resolveLabel(rules, "Amazon"))
    }
}
