package com.prettycountdown.data.model

/**
 * A small, hand-picked color theme that can be applied to an event and its widgets.
 * Colors are stored as ARGB [Long]s so they can be used directly with both
 * `androidx.compose.ui.graphics.Color` and Glance's `ColorProvider`.
 */
data class ColorPalette(
    val key: String,
    val displayName: String,
    val primary: Long,
    val secondary: Long,
    val background: Long,
    val onBackground: Long
)

object ColorPalettes {
    val all = listOf(
        ColorPalette("sunset", "Sunset", 0xFFFF7E5F, 0xFFFEB47B, 0xFFFFF1E6, 0xFF3D2B1F),
        ColorPalette("ocean", "Ocean", 0xFF2193B0, 0xFF6DD5ED, 0xFFE6F7FB, 0xFF0B2B33),
        ColorPalette("blossom", "Blossom", 0xFFFF6FB5, 0xFFFFD3E0, 0xFFFFF0F5, 0xFF5C1F36),
        ColorPalette("forest", "Forest", 0xFF11998E, 0xFF38EF7D, 0xFFE8FBF2, 0xFF0D3B2E),
        ColorPalette("midnight", "Midnight", 0xFF0F2027, 0xFF2C5364, 0xFF11161D, 0xFFE8EEF5),
        ColorPalette("lavender", "Lavender", 0xFF8E54E9, 0xFF4776E6, 0xFFF3F0FF, 0xFF2E1A47),
        ColorPalette("gold", "Gold", 0xFFF7971E, 0xFFFFD200, 0xFFFFF8E1, 0xFF4A3300),
        ColorPalette("mono", "Monochrome", 0xFF333333, 0xFF777777, 0xFFF5F5F5, 0xFF1A1A1A),
    )

    val default = all.first()

    fun byKey(key: String): ColorPalette = all.find { it.key == key } ?: default
}
