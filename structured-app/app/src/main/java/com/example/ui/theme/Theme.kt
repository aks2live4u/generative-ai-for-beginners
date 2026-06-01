package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CosmosPrimary,
    secondary = CosmosSecondary,
    tertiary = CosmosTertiary,
    background = CosmosDb,
    surface = CosmosSurface,
    surfaceVariant = CosmosSurfaceVariant,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF381E72), // Elegant Dark Purple text
    onSecondary = androidx.compose.ui.graphics.Color(0xFF381E72), // Elegant Dark Purple text
    onTertiary = CosmosDb,
    onBackground = CosmosOnBackground,
    onSurface = CosmosOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = SandPrimary,
    secondary = SandSecondary,
    tertiary = SandTertiary,
    background = SandDb,
    surface = SandSurface,
    surfaceVariant = SandSurfaceVariant,
    onPrimary = SandSurface,
    onSecondary = SandSurface,
    onTertiary = SandSurface,
    onBackground = SandOnBackground,
    onSurface = SandOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
