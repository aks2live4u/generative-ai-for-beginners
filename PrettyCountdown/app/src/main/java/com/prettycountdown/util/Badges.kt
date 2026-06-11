package com.prettycountdown.util

/** Stats used to evaluate which [Badge]s the user has unlocked. */
data class UserStats(val eventsCreated: Int, val openStreak: Int)

data class Badge(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val isUnlocked: (UserStats) -> Boolean
)

object Badges {
    val all = listOf(
        Badge("first_event", "First Event", "🌱", "Create your first countdown") { it.eventsCreated >= 1 },
        Badge("five_events", "Getting Organized", "🗂️", "Create 5 countdowns") { it.eventsCreated >= 5 },
        Badge("ten_events", "10 Events", "📅", "Create 10 countdowns") { it.eventsCreated >= 10 },
        Badge("hundred_countdowns", "100 Countdowns", "💯", "Create 100 countdowns") { it.eventsCreated >= 100 },
        Badge("streak_3", "3-Day Streak", "🔥", "Open the app 3 days in a row") { it.openStreak >= 3 },
        Badge("streak_7", "7-Day Streak", "🏆", "Open the app 7 days in a row") { it.openStreak >= 7 },
    )
}
