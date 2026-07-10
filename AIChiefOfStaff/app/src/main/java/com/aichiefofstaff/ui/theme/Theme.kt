package com.aichiefofstaff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aichiefofstaff.data.prefs.ThemeMode

private val DarkColors = darkColorScheme(
    primary = PurplePrimaryLight,
    secondary = AccentPink,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF201F2C),
    onPrimary = DarkBackground,
    onBackground = Color(0xFFEDEDF2),
    onSurface = Color(0xFFEDEDF2),
    outline = Color(0xFF3A3950)
)

private val LightColors = lightColorScheme(
    primary = PurplePrimary,
    secondary = AccentPink,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = Color(0xFFF1EEFB),
    onPrimary = Color.White,
    onBackground = Color(0xFF16161F),
    onSurface = Color(0xFF16161F),
    outline = Color(0xFFDCD6F0)
)

@Composable
fun AiChiefOfStaffTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AiChiefTypography,
        shapes = AiChiefShapes,
        content = content
    )
}
