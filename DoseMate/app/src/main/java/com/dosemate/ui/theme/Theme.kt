package com.dosemate.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dosemate.data.ThemeMode
import com.dosemate.data.ThemePrefs

private fun colorScheme(colors: DoseMateColors, isDark: Boolean) = if (isDark) {
    darkColorScheme(
        primary = colors.accent,
        secondary = TealLight,
        background = DarkBgTop,
        surface = DarkBgBottom,
        onPrimary = DarkBgTop,
        onBackground = colors.textPrimary,
        onSurface = colors.textPrimary
    )
} else {
    lightColorScheme(
        primary = colors.accent,
        secondary = TealLight,
        background = MintBackground,
        surface = MintBackground,
        onPrimary = Color.White,
        onBackground = colors.textPrimary,
        onSurface = colors.textPrimary
    )
}

private fun typography(colors: DoseMateColors) = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = colors.textPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.textPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.textPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = colors.textPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = colors.textSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
)

@Composable
fun DoseMateTheme(content: @Composable () -> Unit) {
    val mode by ThemePrefs.current
    val isDark = mode == ThemeMode.DARK
    val colors = if (isDark) DarkDoseMateColors else LightDoseMateColors

    CompositionLocalProvider(LocalDoseMateColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme(colors, isDark),
            typography = typography(colors),
            content = content
        )
    }
}

@Composable
fun GlassBackdrop(content: @Composable () -> Unit) {
    val colors = LocalDoseMateColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        content()
    }
}
