package com.personalfinanceai.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme palette, sampled from the provided light-mode dashboard mockups.
val LightBackground = Color(0xFFF5F6FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFF1F7)
val LightPrimary = Color(0xFF5B5FEF)
val LightOnBackground = Color(0xFF14151F)
val LightOnSurfaceVariant = Color(0xFF6B7280)
val LightOutline = Color(0xFFE5E7EB)

// Dark theme palette, sampled from the provided dark-mode dashboard mockups.
val DarkBackground = Color(0xFF0F1020)
val DarkSurface = Color(0xFF1A1B2E)
val DarkSurfaceVariant = Color(0xFF23253B)
val DarkPrimary = Color(0xFF7B7FFF)
val DarkOnBackground = Color(0xFFF5F6FA)
val DarkOnSurfaceVariant = Color(0xFF9CA3AF)
val DarkOutline = Color(0xFF2A2C42)

// Shared semantic colors: income/positive (green) and expense/negative (red), used in both
// themes for amount text, chart lines, and health-score gauges - matches both mockup sets.
val IncomeGreen = Color(0xFF1FAA59)
val IncomeGreenDark = Color(0xFF22D38B)
val ExpenseRed = Color(0xFFFF5A5F)
val ExpenseRedDark = Color(0xFFFF6B6B)

// Category accent swatches used for merchant/category icon chips on Transactions/Insights.
object CategoryAccents {
    val Orange = Color(0xFFFF8A00)
    val Purple = Color(0xFF8B5CF6)
    val Blue = Color(0xFF3B82F6)
    val Red = Color(0xFFEF4444)
    val Teal = Color(0xFF14B8A6)
    val Pink = Color(0xFFEC4899)
}
