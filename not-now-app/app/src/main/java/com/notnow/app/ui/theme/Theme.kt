package com.notnow.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE85D04),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFF1E3A5F),
    onSecondary = Color.White,
    background = Color(0xFF0D0D0D),
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF333333),
    error = Color(0xFFCF6679)
)

@Composable
fun NotNowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
