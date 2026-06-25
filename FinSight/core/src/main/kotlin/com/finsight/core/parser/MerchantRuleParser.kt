package com.finsight.core.parser

/**
 * Recognizes "teach a rule" chat messages (e.g. "ATM SBI Main Branch is Mother Support", "tag
 * Swiggy as Food Delivery") so [MerchantRuleBook] can learn from them, without adding any new UI -
 * the existing Chat screen's free-text input is the only surface this needs.
 */
object MerchantRuleParser {

    private val explicitPrefixes = listOf("tag ", "label ", "categorize ", "mark ")
    private val genericSubjects = setOf("this", "that", "it", "i", "he", "she", "they", "we", "you")
    private val questionWords = listOf("how", "what", "where", "when", "why", "who", "which")
    private val asSplitter = Regex("^(.+?)\\s+as\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val isSplitter = Regex("^(.{2,40}?)\\s+is\\s+(.{2,40})$", RegexOption.IGNORE_CASE)

    fun parse(text: String): MerchantRule? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        for (prefix in explicitPrefixes) {
            if (trimmed.lowercase().startsWith(prefix)) {
                val rest = trimmed.substring(prefix.length)
                val match = asSplitter.find(rest) ?: continue
                val merchant = match.groupValues[1].trim()
                val label = match.groupValues[2].trim()
                if (merchant.isEmpty() || label.isEmpty()) continue
                return MerchantRule(merchant, label)
            }
        }

        if (trimmed.endsWith("?")) return null
        val lower = trimmed.lowercase()
        if (questionWords.any { lower == it || lower.startsWith("$it ") || lower.contains(" $it ") }) return null

        val match = isSplitter.matchEntire(trimmed) ?: return null
        val merchantPhrase = match.groupValues[1].trim()
        val label = match.groupValues[2].trim()
        if (merchantPhrase.lowercase() in genericSubjects) return null
        if (label.split(Regex("\\s+")).size > 6) return null
        return MerchantRule(merchantPhrase, label)
    }
}
