package com.prettycountdown.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.Event
import com.prettycountdown.util.CountdownMath
import kotlin.math.roundToInt

/** A single event row used on the Home screen and inside collections. */
@Composable
fun EventCard(event: Event, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val accent = Color(palette.primary)
    val breakdown = CountdownMath.breakdown(event.targetDateTime, now)
    val progress = CountdownMath.progressFraction(event.startDateTime, event.targetDateTime, now)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "card-progress")
    val line = CountdownMath.formatLine(event.countdownFormat, event.targetDateTime, event.startDateTime, now)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(event.category.emoji, style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        CountdownMath.formatDate(event.targetDateTime, event.hasTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .background(accent, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(line, color = Color.White, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            if (!breakdown.isPast) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LiveTickerText(breakdown = breakdown, color = accent)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${(animatedProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                )
            }
        }
    }
}
