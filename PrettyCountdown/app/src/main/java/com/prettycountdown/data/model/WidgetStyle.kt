package com.prettycountdown.data.model

/**
 * Visual style applied to a home-screen countdown widget. The widget itself is the
 * product, so every style needs to look great at small, medium and large sizes.
 */
enum class WidgetStyle(val displayName: String, val description: String, val emoji: String) {
    MINIMAL("Minimal", "Big number, clean typography, nothing else.", "▫️"),
    ELEGANT("Elegant", "Soft serif vibes with an icon and subtitle.", "✨"),
    FLIP_CLOCK("Flip Clock", "Retro split-flap digits.", "🔢"),
    CIRCULAR_PROGRESS("Circular Progress", "A ring of dots fills as the date nears.", "◎"),
    GLASSMORPHISM("Glassmorphism", "Frosted glass card over a color wash.", "🪩"),
    KAWAII("Cute Kawaii", "Pastel colors, friendly emoji, sleeps left.", "🐼"),
    RETRO_PIXEL("Retro Pixel", "Monospace, bracketed, 8-bit energy.", "👾"),
    NEON("Neon", "Glowing digits on a midnight background.", "💜"),
    CALENDAR("Calendar", "A torn calendar page for the big day.", "📅"),
    PHOTO("Photo", "Your photo fills the background.", "🖼️"),
    JOURNEY("Journey", "A little traveler walks toward the date.", "🚶");

    companion object {
        val default = MINIMAL
    }
}
