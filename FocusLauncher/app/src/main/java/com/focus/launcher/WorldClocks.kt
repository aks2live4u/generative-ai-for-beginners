package com.focus.launcher

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

const val MAX_WORLD_CLOCKS = 4

/** Shown first in the city picker before you type anything. */
private val popularZones = listOf(
    "Asia/Kolkata", "Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Dubai",
    "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney", "Europe/Paris", "Europe/Berlin",
    "America/Chicago", "America/Toronto", "Asia/Hong_Kong", "Asia/Shanghai", "Asia/Riyadh",
    "Africa/Johannesburg", "America/Sao_Paulo", "Pacific/Auckland", "Asia/Kathmandu", "Asia/Dhaka",
)

private val allCityZones: List<String> by lazy {
    ZoneId.getAvailableZoneIds()
        .filter { id ->
            val region = id.substringBefore('/')
            '/' in id && region in setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")
        }
        .sortedBy { cityName(it) }
}

/** "America/Argentina/Buenos_Aires" → "Buenos Aires". */
fun cityName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

/** "UTC+5:30" style offset for a zone, right now. */
fun utcLabel(zoneId: String): String {
    val offset = runCatching { ZoneId.of(zoneId).rules.getOffset(Instant.now()) }.getOrDefault(ZoneOffset.UTC)
    val s = offset.totalSeconds
    if (s == 0) return "UTC"
    val h = abs(s) / 3600
    val m = (abs(s) % 3600) / 60
    return "UTC" + (if (s > 0) "+" else "-") + h + if (m > 0) ":%02d".format(m) else ""
}

/** "+5h 30m" / "−3h" / "same time" relative to [here]. */
fun relativeOffset(there: ZonedDateTime, here: ZonedDateTime): String {
    val diff = there.offset.totalSeconds - here.offset.totalSeconds
    if (diff == 0) return "same time"
    val h = abs(diff) / 3600
    val m = (abs(diff) % 3600) / 60
    val sign = if (diff > 0) "+" else "−"
    return sign + (if (h > 0) "${h}h" else "") + (if (h > 0 && m > 0) " " else "") + (if (m > 0) "${m}m" else "")
}

/** "tomorrow" / "yesterday" / null when it's the same calendar day. */
fun dayShift(there: LocalDate, here: LocalDate): String? = when {
    there.isAfter(here) -> "tomorrow"
    there.isBefore(here) -> "yesterday"
    else -> null
}

/** Up to four small clocks under the main one, in the clock font, scaled with the text size. */
@Composable
fun WorldClocksRow(settings: LauncherSettings, look: HomeLook) {
    val zones = settings.worldClocks
    if (zones.isEmpty()) return
    val now = rememberNow(minuteOnly = true)
    val here = now.atZone(ZoneId.systemDefault())
    val pattern = DateTimeFormatter.ofPattern(if (settings.use24h) "HH:mm" else "h:mm a")
    val perRow = if (zones.size <= 3) zones.size else 2
    val timeSize = (26 * look.clockSize * look.textSize.coerceAtMost(1.4f)).sp
    val labelSize = (12 * look.textSize.coerceAtMost(1.3f)).sp

    // A lifted panel: tonal ground with a faint top highlight, a fine rim and a soft shadow,
    // so the cities read as their own layer between the clock and the apps.
    val onLight = !look.color.isLight()
    val shape = RoundedCornerShape(22.dp)
    val ink = if (onLight) Color.Black else Color.White
    val ground = Brush.verticalGradient(listOf(ink.copy(alpha = 0.10f), ink.copy(alpha = 0.045f)))
    val rule = look.color.copy(alpha = 0.14f)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .shadow(elevation = 14.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(ground)
            .border(1.dp, Brush.verticalGradient(listOf(ink.copy(alpha = 0.16f), ink.copy(alpha = 0.05f))), shape)
    ) {
        zones.chunked(perRow).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(rule))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { colIndex, id ->
                    if (colIndex > 0) Box(Modifier.padding(vertical = 12.dp).width(1.dp).fillMaxHeight().background(rule))
                    val zone = runCatching { ZoneId.of(id) }.getOrNull()
                    val there = zone?.let { here.withZoneSameInstant(it) }
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 11.dp)) {
                        Text(
                            cityName(id),
                            color = look.color.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = labelSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            there?.format(pattern)?.lowercase() ?: "--:--",
                            color = look.color,
                            fontFamily = look.clockFamily,
                            fontWeight = look.clockWeight,
                            fontSize = timeSize,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                        if (there != null) {
                            Text(
                                listOfNotNull(dayShift(there.toLocalDate(), here.toLocalDate()), relativeOffset(there, here))
                                    .joinToString(" · "),
                                color = look.color.copy(alpha = 0.62f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = labelSize,
                                maxLines = 1,
                            )
                        }
                    }
                }
                repeat(perRow - row.size) {
                    Box(Modifier.padding(vertical = 12.dp).width(1.dp).fillMaxHeight().background(rule))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Searchable list of cities; popular ones first. */
@Composable
fun CityPickerDialog(existing: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val shown = remember(q, existing) {
        val pool = if (q.isEmpty()) popularZones else allCityZones.filter {
            cityName(it).contains(q, ignoreCase = true) || it.contains(q, ignoreCase = true)
        }
        pool.filter { it !in existing && runCatching { ZoneId.of(it) }.isSuccess }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a city") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search a city, e.g. London") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (q.isEmpty()) "Popular cities" else "${shown.size} matches",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it }) { id ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable(role = Role.Button, onClickLabel = "Add ${cityName(id)}") { onPick(id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cityName(id), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    id.substringBefore('/') + "  ·  " + utcLabel(id),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                            }
                        }
                    }
                }
                if (q.isNotEmpty() && shown.isEmpty()) {
                    Text(
                        "No city called “$q”. Try the nearest big city in the same time zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
