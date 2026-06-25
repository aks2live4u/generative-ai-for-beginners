package com.finsight.core.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MerchantMatcherTest {

    @Test
    fun `matches identical merchant names`() {
        assertEquals(true, MerchantMatcher.isSameMerchant("Swiggy", "Swiggy"))
    }

    @Test
    fun `matches a noisy order-confirmation variant of the same merchant`() {
        assertEquals(true, MerchantMatcher.isSameMerchant("Swiggy", "SWIGGY*ORDER"))
    }

    @Test
    fun `matches a legal-entity variant of the same merchant`() {
        assertEquals(true, MerchantMatcher.isSameMerchant("Bundl Technologies Pvt Ltd", "Bundl"))
    }

    @Test
    fun `does not match unrelated merchants`() {
        assertEquals(false, MerchantMatcher.isSameMerchant("Swiggy", "Amazon"))
    }

    @Test
    fun `does not match on a too-short common substring`() {
        assertEquals(false, MerchantMatcher.isSameMerchant("BP", "Bigbasket"))
    }

    @Test
    fun `blank or unknown merchants never match`() {
        assertEquals(false, MerchantMatcher.isSameMerchant("Unknown", ""))
    }
}
