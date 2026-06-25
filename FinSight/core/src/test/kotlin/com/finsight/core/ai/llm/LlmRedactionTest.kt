package com.finsight.core.ai.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LlmRedactionTest {

    @Test
    fun `masks a long account number but keeps the last 4 digits`() {
        val redacted = LlmRedaction.redact("Rs.500 debited from A/c 123456789012 for Swiggy")
        assertEquals("Rs.500 debited from A/c XXXXXXXX9012 for Swiggy", redacted)
    }

    @Test
    fun `leaves short numbers like amounts and dates untouched`() {
        val redacted = LlmRedaction.redact("Rs.4,12,922 debited on 01-Jun-24, OTP 123456")
        assertEquals("Rs.4,12,922 debited on 01-Jun-24, OTP 123456", redacted)
    }

    @Test
    fun `leaves text with no digit runs untouched`() {
        assertEquals("Swiggy order delivered", LlmRedaction.redact("Swiggy order delivered"))
    }
}
