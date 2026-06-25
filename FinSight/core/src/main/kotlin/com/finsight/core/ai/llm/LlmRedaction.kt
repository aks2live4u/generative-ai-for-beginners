package com.finsight.core.ai.llm

/**
 * Masks long digit runs (account numbers, card numbers, phone numbers) before any text is sent
 * to a third-party LLM API. Amounts are short (rarely more than 7-8 digits incl. paise) and
 * dates/OTPs are 4-6 digits, so a 9+ digit threshold catches account/card numbers while leaving
 * everything an LLM actually needs (merchant, amount, date) untouched.
 */
object LlmRedaction {

    private val longDigitRun = Regex("""\d{9,}""")

    /** Replaces any run of 9+ consecutive digits with a last-4-preserving mask, e.g. "XXXXX1234". */
    fun redact(text: String): String =
        longDigitRun.replace(text) { match ->
            val digits = match.value
            "X".repeat((digits.length - 4).coerceAtLeast(0)) + digits.takeLast(4)
        }
}
