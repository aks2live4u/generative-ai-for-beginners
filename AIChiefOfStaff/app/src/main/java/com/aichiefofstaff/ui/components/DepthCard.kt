package com.aichiefofstaff.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared elevated-card look: soft shadow, rounded shape, tonal surface.
 * Every list row / tile in the app goes through this instead of a bare Card
 * so the whole UI reads as one consistent, layered surface.
 */
@Composable
fun DepthCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 4.dp,
        pressedElevation = 1.dp,
        focusedElevation = 6.dp,
        hoveredElevation = 6.dp
    )
    val colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    }
}
