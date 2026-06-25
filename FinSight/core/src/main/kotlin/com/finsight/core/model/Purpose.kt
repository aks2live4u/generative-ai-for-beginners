package com.finsight.core.model

/** Why money moved, not just where - the "Life Intelligence" dimension layered on top of [Category]. */
enum class Purpose(val displayName: String) {
    NEED("Need"),
    WANT("Want"),
    INVESTMENT("Investment"),
    FAMILY("Family"),
    GROWTH("Growth"),
    LIFESTYLE("Lifestyle"),
    DEBT("Debt");

    companion object {
        fun fromDisplayName(name: String): Purpose? = entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}
