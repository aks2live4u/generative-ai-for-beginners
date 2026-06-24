package com.dosemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dosemate.ui.theme.LocalDoseMateColors

/**
 * Lifted-glass card: translucent frosted surface, soft top sheen, and a bright top edge
 * fading into a dimmer border — approximates glassmorphism without a true backdrop blur
 * so it renders consistently from minSdk 26 upward.
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
    Box(
        modifier = modifier
            .shadow(
                elevation = 28.dp,
                shape = shape,
                ambientColor = Color(0x40064E48),
                spotColor = Color(0x59064E48)
            )
            .clip(shape)
            .background(colors.glassSoft)
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(colors.glassHighlight, colors.glassBorder, colors.glassBorder.copy(alpha = colors.glassBorder.alpha * 0.3f))
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.glassHighlight.copy(alpha = colors.glassHighlight.alpha * 0.55f), Color.Transparent)
                    )
                )
        )
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
