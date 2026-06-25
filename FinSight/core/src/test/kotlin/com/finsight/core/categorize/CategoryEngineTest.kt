package com.finsight.core.categorize

import com.finsight.core.model.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CategoryEngineTest {

    @Test
    fun `recognizes brand merchants over generic group fallbacks`() {
        assertEquals(Category.FOOD_DELIVERY, CategoryEngine.categorize("Swiggy order #123"))
        assertEquals(Category.FOOD_DELIVERY, CategoryEngine.categorize("Uber Eats delivery"))
        assertEquals(Category.RIDE_SHARING, CategoryEngine.categorize("Uber trip payment"))
        assertEquals(Category.AMAZON, CategoryEngine.categorize("Amazon.in order"))
        assertEquals(Category.GROCERIES, CategoryEngine.categorize("Blinkit grocery order"))
    }

    @Test
    fun `does not false-positive match short keywords inside unrelated words`() {
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Police station fine"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Public library fee"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Premium membership fee"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Academic fee payment"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Gossip magazine subscription"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Smaller item purchase"))
    }

    @Test
    fun `matches short keywords when they appear as whole words`() {
        assertEquals(Category.EMI, CategoryEngine.categorize("Home loan EMI payment"))
        assertEquals(Category.INVESTMENT_OUTFLOW, CategoryEngine.categorize("SIP installment - Zerodha"))
        assertEquals(Category.SHOPPING_OTHER, CategoryEngine.categorize("Mall shopping spree"))
    }

    @Test
    fun `covers previously unreachable categories`() {
        assertEquals(Category.SHOPPING_OTHER, CategoryEngine.categorize("Decathlon retail store purchase"))
        assertEquals(Category.TRANSPORTATION_OTHER, CategoryEngine.categorize("IRCTC train ticket booking"))
        assertEquals(Category.TRANSPORTATION_OTHER, CategoryEngine.categorize("Toll plaza FASTag deduction"))
        assertEquals(Category.RENT_INCOME, CategoryEngine.categorize("Rent received from tenant"))
        assertEquals(Category.INVESTMENT_INCOME, CategoryEngine.categorize("Mutual fund redemption credited"))
    }

    @Test
    fun `falls back to miscellaneous for unrecognized text`() {
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("XYZ Traders Pvt Ltd"))
    }

    @Test
    fun `insurance keywords require explicit insurance context`() {
        assertEquals(Category.INSURANCE, CategoryEngine.categorize("LIC India premium due"))
        assertEquals(Category.INSURANCE, CategoryEngine.categorize("HDFC Life insurance premium"))
        assertEquals(Category.MISCELLANEOUS, CategoryEngine.categorize("Police verification fee"))
    }
}
