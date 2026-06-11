package com.prettycountdown.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.Event
import com.prettycountdown.util.CountdownMath

/** A single event row used on the Home screen and inside collections. */
@Composable
fun EventCard(event: Event, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val accent = Color(palette.primary)
    val line = CountdownMath.formatLine(event.countdownFormat, event.targetDateTime, event.startDateTime, now)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(event.category.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    CountdownMath.formatDate(event.targetDateTime, event.hasTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .background(accent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(line, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
