package com.finsight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Extra semantic colors the Material3 [androidx.compose.material3.ColorScheme] has no slot for. */
data class FinanceExtendedColors(
    val income: Color,
    val expense: Color
)

private val LocalFinanceExtendedColors = staticCompositionLocalOf {
    FinanceExtendedColors(income = IncomeGreen, expense = ExpenseRed)
}

val MaterialTheme.financeColors: FinanceExtendedColors
    @Composable get() = LocalFinanceExtendedColors.current

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = IncomeGreen,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ExpenseRed
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    secondary = IncomeGreenDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ExpenseRedDark
)

@Composable
fun FinSightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) {
        FinanceExtendedColors(income = IncomeGreenDark, expense = ExpenseRedDark)
    } else {
        FinanceExtendedColors(income = IncomeGreen, expense = ExpenseRed)
    }

    CompositionLocalProvider(LocalFinanceExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
