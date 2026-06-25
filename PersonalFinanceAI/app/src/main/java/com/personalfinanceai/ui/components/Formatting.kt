package com.personalfinanceai.ui.components

import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats an amount as a rupee figure with thousands grouping, e.g. "₹75,000". */
fun formatRupees(amount: Double): String {
    val rounded = Math.round(amount)
    val text = String.format(Locale.US, "%,d", rounded)
    return "₹$text"
}

val mediumDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
