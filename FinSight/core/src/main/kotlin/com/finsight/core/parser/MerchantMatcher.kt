package com.finsight.core.parser

/**
 * Same real-world purchase often arrives through more than one channel (a bank SMS, the UPI app's
 * notification, an order-confirmation email) with slightly different merchant spellings -
 * "Swiggy" vs "SWIGGY*ORDER" vs "Bundl Technologies Pvt Ltd". [TransactionRepository]'s exact
 * dedup match misses these, so it would record the same purchase two or three times instead of
 * raising confidence on one. This does the normalization + fuzzy comparison needed to recognize
 * "same merchant, different source" without a schema change.
 */
object MerchantMatcher {

    private val noiseWords = setOf(
        "pvt", "ltd", "limited", "private", "technologies", "technology", "india",
        "services", "service", "online", "payments", "payment", "order", "orders"
    )

    // Compiled once and reused - normalize() is called O(n^2) times by RecurringPaymentDetector's
    // merchant clustering, so recompiling these per call (as Regex(...) literals) made that
    // function slow enough to ANR the UI thread on a real device's multi-thousand-transaction
    // history (it's only ever invoked on tiny test fixtures otherwise).
    private val nonAlphaRegex = Regex("[^a-z\\s]")
    private val whitespaceRegex = Regex("\\s+")

    /** Lowercases, strips punctuation/digits/noise business words, and collapses whitespace. */
    fun normalize(name: String): String {
        val cleaned = name
            .lowercase()
            .replace(nonAlphaRegex, " ")
        val words = cleaned.split(whitespaceRegex)
            .filter { it.isNotBlank() && it !in noiseWords }
        return words.joinToString("")
    }

    /**
     * True when [a] and [b] likely refer to the same merchant: identical after normalization, or
     * one normalized form contains the other (e.g. "swiggy" within "swiggyorder"), guarded by a
     * minimum length so short generic words don't match everything.
     */
    fun isSameMerchant(a: String, b: String): Boolean = isSameNormalizedMerchant(normalize(a), normalize(b))

    /** Same comparison as [isSameMerchant], but for callers that already normalized both names. */
    fun isSameNormalizedMerchant(normA: String, normB: String): Boolean {
        if (normA.isBlank() || normB.isBlank()) return false
        if (normA == normB) return true
        val shorter = if (normA.length <= normB.length) normA else normB
        val longer = if (normA.length <= normB.length) normB else normA
        return shorter.length >= 4 && longer.contains(shorter)
    }
}
