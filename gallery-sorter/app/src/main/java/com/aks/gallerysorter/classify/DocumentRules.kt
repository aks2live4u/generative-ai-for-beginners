package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment

/**
 * All keyword lists live here. Matched case-insensitively on whole words.
 * SPECIFIC keywords are distinctive on their own; GENERIC ones appear in ordinary prose
 * and can never lift a result to the top confidence band without a specific hit or pattern.
 */
internal object DocumentKeywords {
    val ID_SPECIFIC = listOf(
        "aadhaar", "aadhar", "uidai", "government of india", "permanent account number",
        "driving licence", "driving license", "income tax department",
    )
    val ID_GENERIC = listOf("passport", "income tax", "election commission", "voter id", "licence", "license")
    val DRIVING = listOf("driving licence", "driving license")
    val BANK_NAMES = listOf(
        "sbi", "state bank of india", "hdfc", "icici", "axis bank", "kotak", "pnb",
        "punjab national bank", "bank of baroda", "canara", "union bank", "yes bank", "idfc",
    )
    val BANKING_SPECIFIC = BANK_NAMES + listOf("upi", "paid to", "transaction id", "utr", "account no", "ifsc")
    val BANKING_GENERIC = listOf("balance", "statement", "credited", "debited")
}

private const val STRONG = 0.9f
private const val MULTI = 0.95f
private const val GENERIC_ONLY = 0.7f
private const val KEYWORD_ONLY = 0.6f // one weak signal (keyword or UPI handle): Review band
private const val OTHER_DOCS_CONFIDENCE = 0.7f
private const val OTHER_DOCS_MIN_WORDS = 60

private val DIGITS = Regex("\\d+")
private val IFSC = Regex("\\b[A-Z]{4}0[A-Z0-9]{6}\\b")
private val UPI = Regex("(?<![a-z0-9._-])[a-z0-9._-]{2,}@[a-z]{2,}(?![a-z0-9]|\\.[a-z])")
private val PAN = Regex("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b")
private val AADHAAR = Regex("(?<!\\d)(?<!\\d[ -])\\d{4}[ -]?\\d{4}[ -]?\\d{4}(?![ -]?\\d)")
private val DL_NUMBER = Regex("\\b[A-Z]{2}[ -]?\\d{2}[ -]?\\d{4}[ -]?\\d{7}\\b")
private val MRZ_FIRST = Regex("^P<[A-Z]{3}[A-Z0-9<]{25,}$")
private val MRZ_LIKE = Regex("^(?=.*[<0-9])[A-Z0-9<]{30,}$")
private val WORD = Regex("\\p{L}{2,}")

private class DocSignals(val specific: Int, val generic: Int, val patterns: Int, val strong: Boolean) {
    fun confidence(): Float? {
        val total = specific + generic + patterns
        return when {
            total >= 2 && specific + patterns >= 1 -> MULTI
            strong -> STRONG
            total >= 2 -> GENERIC_ONLY
            total == 1 -> KEYWORD_ONLY
            else -> null
        }
    }
}

fun luhnValid(digits: String): Boolean {
    if (digits.isEmpty() || digits.any { it !in '0'..'9' }) return false
    var sum = 0
    digits.reversed().forEachIndexed { i, c ->
        var d = c - '0'
        if (i % 2 == 1) {
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
    }
    return sum % 10 == 0
}

private val keywordRegexes = HashMap<String, Regex>()

private fun keywordRegex(k: String): Regex =
    keywordRegexes.getOrPut(k) { Regex("\\b${Regex.escape(k)}\\b") }

private fun matched(lower: String, keywords: List<String>): List<String> =
    keywords.filter { keywordRegex(it).containsMatchIn(lower) }

/** Generic keywords already covered by a matched specific keyword (e.g. "income tax") don't count twice. */
private fun countGeneric(lower: String, generic: List<String>, matchedSpecific: List<String>): Int =
    matched(lower, generic).count { g -> matchedSpecific.none { it.contains(g) } }

private fun hasMrz(lines: List<String>): Boolean {
    val first = lines.indexOfFirst { MRZ_FIRST.matches(it) }
    if (first < 0) return false
    return lines.withIndex().any { (i, l) -> i != first && MRZ_LIKE.matches(l) }
}

/** Slide over consecutive digit groups (separated by one space or hyphen) looking for a Luhn-valid 13-19 digit run. */
private fun hasValidCard(text: String): Boolean {
    val groups = DIGITS.findAll(text).toList()
    for (i in groups.indices) {
        val sb = StringBuilder()
        var j = i
        while (j < groups.size) {
            if (j > i) {
                val gap = text.substring(groups[j - 1].range.last + 1, groups[j].range.first)
                if (gap != " " && gap != "-") break
            }
            sb.append(groups[j].value)
            if (sb.length > 19) break
            if (sb.length >= 13 && luhnValid(sb.toString())) return true
            j++
        }
    }
    return false
}

private fun idSignals(lines: List<String>, upper: String, lower: String): DocSignals {
    val specific = matched(lower, DocumentKeywords.ID_SPECIFIC)
    val generic = countGeneric(lower, DocumentKeywords.ID_GENERIC, specific)
    val mrz = hasMrz(lines)
    var patterns = if (mrz) 1 else 0
    if (specific.isNotEmpty() || generic > 0) {
        if (PAN.containsMatchIn(upper)) patterns++
        if (AADHAAR.containsMatchIn(upper)) patterns++
        if (matched(lower, DocumentKeywords.DRIVING).isNotEmpty() && DL_NUMBER.containsMatchIn(upper)) patterns++
    }
    return DocSignals(specific.size, generic, patterns, strong = mrz)
}

private fun bankingSignals(upper: String, lower: String): DocSignals {
    val specific = matched(lower, DocumentKeywords.BANKING_SPECIFIC)
    val generic = countGeneric(lower, DocumentKeywords.BANKING_GENERIC, specific)
    val card = hasValidCard(upper)
    val ifsc = IFSC.containsMatchIn(upper)
    val upi = UPI.containsMatchIn(lower)
    val patterns = listOf(card, ifsc, upi).count { it }
    return DocSignals(specific.size, generic, patterns, strong = card || ifsc)
}

/** IDS beats BANKING beats OTHER_DOCS; null when the OCR text shows no document signal. */
fun documentCategory(ocrText: String): CategoryAssignment? {
    val lines = ocrText.lines().map { it.uppercase().filter { c -> !c.isWhitespace() } }
    val text = ocrText.trim().replace(Regex("\\s+"), " ")
    val upper = text.uppercase()
    val lower = text.lowercase()

    idSignals(lines, upper, lower).confidence()?.let {
        return CategoryAssignment(Category.IDS, it)
    }
    bankingSignals(upper, lower).confidence()?.let {
        return CategoryAssignment(Category.BANKING, it)
    }
    if (WORD.findAll(text).count() >= OTHER_DOCS_MIN_WORDS) {
        return CategoryAssignment(Category.OTHER_DOCS, OTHER_DOCS_CONFIDENCE)
    }
    return null
}
