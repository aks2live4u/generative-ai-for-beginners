package com.prettycountdown.data.model

/**
 * The category an [Event] belongs to. Each category ships with a default emoji and
 * default [com.prettycountdown.data.model.ColorPalette] used when a new event is created.
 */
enum class EventCategory(val displayName: String, val emoji: String, val defaultPaletteKey: String) {
    BIRTHDAY("Birthday", "🎂", "blossom"),
    WEDDING("Wedding", "💍", "lavender"),
    ANNIVERSARY("Anniversary", "💞", "sunset"),
    TRAVEL("Travel", "✈️", "ocean"),
    VACATION("Vacation", "🏖️", "ocean"),
    EXAM("Exam", "📚", "midnight"),
    FITNESS_GOAL("Fitness Goal", "💪", "forest"),
    BABY_ARRIVAL("Baby Arrival", "👶", "blossom"),
    FESTIVAL("Festival", "🎉", "gold"),
    NEW_YEAR("New Year", "🎆", "midnight"),
    PRODUCT_LAUNCH("Product Launch", "🚀", "midnight"),
    GOAL("Goal", "🎯", "forest"),
    HABIT_STREAK("Habit & Streak", "🔥", "gold"),
    MILESTONE("Personal Milestone", "⭐", "lavender"),
    CUSTOM("Custom", "⭐", "mono");

    companion object {
        val default = CUSTOM
    }
}
