package com.nexus.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** NEXUS palette: obsidian surfaces, one luminous cyan, amber reserved for things that need you. */
object Nx {
    val Obsidian = Color(0xFF040608)
    val Surface = Color(0xFF0A0F14)
    val SurfaceHi = Color(0xFF101921)
    val Cyan = Color(0xFF38E8FF)
    val CyanSoft = Color(0xFF9AF5FF)
    val CyanGlow = Color(0x5538E8FF)
    val CyanDim = Color(0x2238E8FF)
    val Line = Color(0x2E38E8FF)
    val Text = Color(0xFFE8F8FF)
    val Muted = Color(0xFF8AA2B2)
    val Amber = Color(0xFFFFB547)
    val Danger = Color(0xFFFF5C7A)
    val Mint = Color(0xFF5CFFB0)
}

val Hud = FontFamily.Monospace

private val NexusTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Thin, fontSize = 88.sp, letterSpacing = (-2).sp, color = Nx.Text),
    headlineMedium = TextStyle(fontWeight = FontWeight.Light, fontSize = 28.sp, color = Nx.Text),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, color = Nx.Text),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Nx.Text),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, color = Nx.Text),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = Nx.Text),
    labelSmall = TextStyle(fontFamily = Hud, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.sp, color = Nx.Muted),
    labelMedium = TextStyle(fontFamily = Hud, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.5.sp, color = Nx.Muted),
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Nx.Cyan,
            onPrimary = Nx.Obsidian,
            secondary = Nx.Amber,
            background = Nx.Obsidian,
            onBackground = Nx.Text,
            surface = Nx.Surface,
            onSurface = Nx.Text,
            surfaceVariant = Nx.SurfaceHi,
            onSurfaceVariant = Nx.Muted,
            outline = Nx.Line,
            error = Nx.Danger,
        ),
        typography = NexusTypography,
        content = content,
    )
}
