package com.dosemate.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Theme-aware colors for the lifted-glass UI, swapped between light and dark mode. */
data class DoseMateColors(
    val background: Color,
    val glassStrong: Color,
    val glassSoft: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val headerText: Color,
    val iconSurface: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LightDoseMateColors = DoseMateColors(
    background = MintBackground,
    glassStrong = GlassWhiteStrong,
    glassSoft = GlassWhiteSoft,
    glassBorder = GlassBorder,
    glassHighlight = GlassHighlightLight,
    headerText = TextPrimary,
    iconSurface = Color.White,
    accent = TealDeep,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary
)

val DarkDoseMateColors = DoseMateColors(
    background = DarkBgTop,
    glassStrong = GlassDarkStrong,
    glassSoft = GlassDarkSoft,
    glassBorder = GlassBorderDark,
    glassHighlight = GlassHighlightDark,
    headerText = TextPrimaryDark,
    iconSurface = GlassDarkStrong,
    accent = TealPrimaryDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark
)

val LocalDoseMateColors = compositionLocalOf { LightDoseMateColors }
