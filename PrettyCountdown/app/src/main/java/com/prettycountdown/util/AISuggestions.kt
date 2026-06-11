package com.prettycountdown.util

import com.prettycountdown.data.model.ColorPalette
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.EventCategory

/**
 * Lightweight "AI" helpers used by the create-event flow (event name ideas and
 * theme suggestions). Pretty Countdown is local-first with no account and no
 * network access, so these are curated, on-device templates rather than calls to
 * a hosted model - written behind small functions so a real LLM could replace
 * them later without touching the UI.
 */
object AISuggestions {

    fun nameSuggestions(category: EventCategory): List<String> = when (category) {
        EventCategory.BIRTHDAY -> listOf(
            "Level Up Day", "Another Trip Around the Sun", "Cake Day", "Birthday Bash"
        )
        EventCategory.WEDDING -> listOf(
            "The Big Day", "I Do Day", "Forever Starts Here", "Our Wedding"
        )
        EventCategory.ANNIVERSARY -> listOf(
            "Our Anniversary", "Another Year of Us", "Love Day", "Cheers To Us"
        )
        EventCategory.TRAVEL -> listOf(
            "Next Adventure", "Wanderlust Trip", "Off We Go", "Passport Ready"
        )
        EventCategory.VACATION -> listOf(
            "Dream Vacation", "Time To Unplug", "Sun & Sand", "Out Of Office"
        )
        EventCategory.EXAM -> listOf(
            "Big Exam Day", "Final Boss: The Exam", "Show Time", "Test Day"
        )
        EventCategory.FITNESS_GOAL -> listOf(
            "New Me Goal", "Personal Best Day", "Goal Day", "Race Day"
        )
        EventCategory.BABY_ARRIVAL -> listOf(
            "Little One Arrives", "Due Date", "Hello Baby", "Bump To Baby"
        )
        EventCategory.FESTIVAL -> listOf(
            "Festival Time", "Party Day", "Celebration Day", "Big Festival"
        )
        EventCategory.NEW_YEAR -> listOf(
            "New Year's Eve", "Fresh Start", "Countdown To Midnight", "New Year, New Us"
        )
        EventCategory.PRODUCT_LAUNCH -> listOf(
            "Launch Day", "Ship It Day", "Go Live", "Big Reveal"
        )
        EventCategory.GOAL -> listOf(
            "Goal Day", "Deadline", "Finish Line", "Target Date"
        )
        EventCategory.HABIT_STREAK -> listOf(
            "Streak Goal", "Habit Milestone", "Consistency Check", "Streak Day"
        )
        EventCategory.MILESTONE -> listOf(
            "Big Milestone", "Important Day", "Milestone Unlocked", "The Day"
        )
        EventCategory.CUSTOM -> listOf(
            "My Countdown", "Special Day", "The Big Day", "Important Date"
        )
    }

    /** A short, fun subtitle used in the Elegant/Calendar widget styles. */
    fun taglineFor(category: EventCategory): String = when (category) {
        EventCategory.BIRTHDAY -> "Cake. Candles. Countdown."
        EventCategory.WEDDING -> "Forever is getting closer."
        EventCategory.ANNIVERSARY -> "Celebrating us."
        EventCategory.TRAVEL, EventCategory.VACATION -> "Adventure awaits."
        EventCategory.EXAM -> "You've got this."
        EventCategory.FITNESS_GOAL -> "One day closer."
        EventCategory.BABY_ARRIVAL -> "So close to meeting you."
        EventCategory.FESTIVAL -> "Let's celebrate."
        EventCategory.NEW_YEAR -> "New year, new you."
        EventCategory.PRODUCT_LAUNCH -> "Almost ready to ship."
        EventCategory.GOAL -> "Stay focused."
        EventCategory.HABIT_STREAK -> "Keep the streak alive."
        EventCategory.MILESTONE -> "A big moment ahead."
        EventCategory.CUSTOM -> "Counting down."
    }

    /** Suggested color palettes for a category - the category default first, then a few alternates. */
    fun paletteSuggestions(category: EventCategory): List<ColorPalette> {
        val default = ColorPalettes.byKey(category.defaultPaletteKey)
        val others = ColorPalettes.all.filter { it.key != default.key }
        return (listOf(default) + others.take(3))
    }
}
