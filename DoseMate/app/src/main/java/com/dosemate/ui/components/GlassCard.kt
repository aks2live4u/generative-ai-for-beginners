package com.dosemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dosemate.ui.theme.LocalDoseMateColors

/**
 * Lifted-glass card: translucent frosted surface, soft border, raised shadow.
 * Approximates glassmorphism without a true backdrop blur so it renders
 * consistently from minSdk 26 upward.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    val colors = LocalDoseMateColors.current
    val shape = RoundedCornerShape(cornerRadius.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 28.dp,
                shape = shape,
                ambientColor = Color(0x40064E48),
                spotColor = Color(0x59064E48)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.glassStrong, colors.glassSoft)
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(colors.glassHighlight, colors.glassBorder, colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.4f))
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        content()
    }
}
