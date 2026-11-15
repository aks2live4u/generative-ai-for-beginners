package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment

/** All keyword lists live here. Matched case-insensitively on whole words. */
internal object DocumentKeywords {
    val ID = listOf(
        "aadhaar", "aadhar", "uidai", "government of india", "income tax",
        "permanent account number", "driving licence", "driving license",
        "passport", "election commission", "voter id",
    )
    val DRIVING = listOf("driving licence", "driving license")
    val BANK_NAMES = listOf(
        "sbi", "state bank of india", "hdfc", "icici", "axis bank", "kotak", "pnb",
        "punjab national bank", "bank of baroda", "canara", "union bank", "yes bank", "idfc",
    )
    val PAYMENT = listOf(
        "upi", "paid to", "transaction id", "utr", "debited", "credited", "balance",
        "statement", "account no", "ifsc",
    )
    val BANKING = BANK_NAMES + PAYMENT
}

private const val STRONG = 0.9f
private const val MULTI = 0.95f
private const val KEYWORD_ONLY = 0.6f // one weak signal (keyword or UPI handle): Review band
private const val OTHER_DOCS_CONFIDENCE = 0.7f
private const val OTHER_DOCS_MIN_WORDS = 60

private val CARD_RUN = Regex("(?<!\\d)\\d(?:[ -]?\\d){12,18}(?!\\d)")
private val IFSC = Regex("\\b[A-Z]{4}0[A-Z0-9]{6}\\b")
private val UPI = Regex("(?<![a-z0-9._-])[a-z0-9._-]{2,}@[a-z]{2,}(?![a-z0-9]|\\.[a-z])")
private val PAN = Regex("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b")
private val AADHAAR = Regex("(?<!\\d)(?<!\\d[ -])\\d{4}[ -]?\\d{4}[ -]?\\d{4}(?![ -]?\\d)")
private val DL_NUMBER = Regex("\\b[A-Z]{2}[ -]?\\d{2}[ -]?\\d{4}[ -]?\\d{7}\\b")
private val MRZ_FIRST = Regex("^P<[A-Z]{3}[A-Z0-9<]{25,}$")
private val MRZ_LIKE = Regex("^(?=.*[<0-9])[A-Z0-9<]{30,}$")
private val WORD = Regex("\\p{L}{2,}")

private class Signals(val keywords: Int, val patterns: Int, val strong: Boolean) {
    fun confidence(): Float? = when {
        keywords + patterns >= 2 -> MULTI
        strong -> STRONG
        keywords + patterns == 1 -> KEYWORD_ONLY
        else -> null
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

private fun countKeywords(lower: String, keywords: List<String>): Int =
    keywords.count { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }

private fun hasMrz(lines: List<String>): Boolean {
    val first = lines.indexOfFirst { MRZ_FIRST.matches(it) }
    if (first < 0) return false
    return lines.withIndex().any { (i, l) -> i != first && MRZ_LIKE.matches(l) }
}

private fun idSignals(lines: List<String>, upper: String, lower: String): Signals {
    val keywords = countKeywords(lower, DocumentKeywords.ID)
    val mrz = hasMrz(lines)
    var patterns = if (mrz) 1 else 0
    if (keywords > 0) {
        if (PAN.containsMatchIn(upper)) patterns++
        if (AADHAAR.containsMatchIn(upper)) patterns++
        if (countKeywords(lower, DocumentKeywords.DRIVING) > 0 && DL_NUMBER.containsMatchIn(upper)) patterns++
    }
    return Signals(keywords, patterns, strong = mrz)
}

private fun bankingSignals(upper: String, lower: String): Signals {
    val keywords = countKeywords(lower, DocumentKeywords.BANKING)
    val card = CARD_RUN.findAll(upper).any { luhnValid(it.value.filter(Char::isDigit)) }
    val ifsc = IFSC.containsMatchIn(upper)
    val upi = UPI.containsMatchIn(lower)
    val patterns = listOf(card, ifsc, upi).count { it }
    return Signals(keywords, patterns, strong = card || ifsc)
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
