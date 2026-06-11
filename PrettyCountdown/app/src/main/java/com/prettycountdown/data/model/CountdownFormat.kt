package com.prettycountdown.data.model

/** Controls how the remaining time is expressed on widgets and in the app. */
enum class CountdownFormat(val displayName: String) {
    DAYS_ONLY("Days Only"),
    DAYS_HOURS("Days + Hours"),
    DAYS_HOURS_MINUTES("Days + Hours + Minutes"),
    SLEEPS_LEFT("Sleeps Left"),
    WEEKS_REMAINING("Weeks Remaining"),
    PERCENTAGE_COMPLETE("Percentage Complete");

    companion object {
        val default = DAYS_ONLY
    }
}
