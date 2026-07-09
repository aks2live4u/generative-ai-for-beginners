package com.aivideotranscriber.util

/**
 * Heuristic, regex-based removal of common filler words/phrases and immediate word repeats.
 * This is intentionally simple (no NLP model) and imperfect - e.g. it will also strip
 * legitimate uses of "like" or "actually". It's an optional toggle for exactly that reason.
 */
object FillerWordCleaner {
    private val FILLER_PATTERN = Regex(
        "\\b(uh+|um+|erm+|hmm+|mm+|you know|i mean|sort of|kind of|kinda|basically|actually|like|okay so)\\b[,]?",
        RegexOption.IGNORE_CASE,
    )
    private val REPEATED_WORD_PATTERN = Regex("\\b(\\w+)(\\s+\\1\\b)+", RegexOption.IGNORE_CASE)
    private val SPACE_BEFORE_PUNCT_PATTERN = Regex("\\s+([,.!?])")
    private val EXTRA_SPACE_PATTERN = Regex("\\s{2,}")

    fun clean(text: String): String {
        var result = FILLER_PATTERN.replace(text, "")
        result = REPEATED_WORD_PATTERN.replace(result) { it.groupValues[1] }
        result = SPACE_BEFORE_PUNCT_PATTERN.replace(result, "$1")
        result = EXTRA_SPACE_PATTERN.replace(result, " ")
        return result.trim()
    }
}
