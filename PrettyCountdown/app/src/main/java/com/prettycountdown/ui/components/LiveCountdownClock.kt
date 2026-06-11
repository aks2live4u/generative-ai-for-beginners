package com.prettycountdown.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.prettycountdown.util.CountdownBreakdown

/**
 * A flip-clock style countdown made of animated digit groups (Days / Hrs / Min / Sec).
 * Every segment slides and fades whenever its value ticks over, so the headline number
 * reads as a genuinely live clock instead of a static label.
 */
@Composable
fun FlipCountdownClock(
    breakdown: CountdownBreakdown,
    accentColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val units = buildList {
        if (breakdown.totalDays > 0) add(FlipUnitData(breakdown.totalDays, "DAYS", padded = false))
        add(FlipUnitData(breakdown.hours, "HRS", padded = true))
        add(FlipUnitData(breakdown.minutes, "MIN", padded = true))
        add(FlipUnitData(breakdown.seconds, "SEC", padded = true))
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        units.forEach { unit -> FlipUnit(unit, accentColor, labelColor) }
    }
}

private data class FlipUnitData(val value: Long, val label: String, val padded: Boolean)

@Composable
private fun FlipUnit(data: FlipUnitData, accentColor: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = data.value,
                transitionSpec = {
                    (slideInVertically(tween(280)) { height -> height } + fadeIn(tween(200)))
                        .togetherWith(slideOutVertically(tween(280)) { height -> -height } + fadeOut(tween(150)))
                },
                label = "flip-${data.label}",
            ) { value ->
                Text(
                    text = if (data.padded) value.toString().padStart(2, '0') else value.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                    color = accentColor,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(data.label, style = MaterialTheme.typography.labelSmall, color = labelColor)
    }
}

/** A compact "12:34:07"-style ticker with a softly pulsing live dot, for list rows. */
@Composable
fun LiveTickerText(breakdown: CountdownBreakdown, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ticker-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    val text = if (breakdown.isPast) {
        "Happening now"
    } else {
        buildString {
            if (breakdown.totalDays > 0) append("${breakdown.totalDays}d ")
            append(breakdown.hours.toString().padStart(2, '0'))
            append(":")
            append(breakdown.minutes.toString().padStart(2, '0'))
            append(":")
            append(breakdown.seconds.toString().padStart(2, '0'))
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer { alpha = if (breakdown.isPast) 1f else pulseAlpha }
                .background(color, CircleShape)
        )
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace), color = color)
    }
}
