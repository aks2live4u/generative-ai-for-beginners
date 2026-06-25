package com.finsight.ui.components

import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats an amount as a rupee figure with thousands grouping, e.g. "₹75,000" or "-₹5,000". */
fun formatRupees(amount: Double): String {
    val rounded = Math.round(amount)
    val sign = if (rounded < 0) "-" else ""
    val text = String.format(Locale.US, "%,d", Math.abs(rounded))
    return "$sign₹$text"
}

val mediumDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
