package com.prettycountdown.util

import com.prettycountdown.data.model.Event

/** Friendly, on-brand copy for milestone notifications - the opposite of "Reminder: event in 3 days". */
object SmartNotifications {
    /** Days-remaining values that trigger a milestone notification. */
    val milestones = setOf(100, 90, 50, 30, 14, 7, 3, 1, 0)

    fun messageFor(event: Event, daysRemaining: Int): Pair<String, String>? {
        if (daysRemaining !in milestones) return null
        val emoji = event.category.emoji
        return when (daysRemaining) {
            0 -> "Today's the day! 🎉" to "${event.name} is happening today."
            1 -> "Tomorrow is the day 🎉" to "${event.name} is tomorrow. Get ready!"
            3 -> "Almost there... ⏳" to "Just 3 days until ${event.name}."
            7 -> "1 week to go" to "7 days to go until ${event.name} $emoji"
            14 -> "2 weeks left" to "14 days until ${event.name}."
            30 -> "1 month to go" to "Only 30 days until ${event.name} $emoji"
            50 -> "50 days left" to "50 days left until ${event.name}."
            90 -> "90 days left" to "90 days left until ${event.name}."
            100 -> "100 days left!" to "100 days left until ${event.name}. The countdown begins!"
            else -> "$daysRemaining days left" to "$daysRemaining days until ${event.name}."
        }
    }
}
