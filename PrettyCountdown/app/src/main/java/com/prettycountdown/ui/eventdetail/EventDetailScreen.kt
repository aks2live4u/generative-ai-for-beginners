package com.prettycountdown.ui.eventdetail

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.model.ChecklistItem
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.Event
import com.prettycountdown.ui.components.FlipCountdownClock
import com.prettycountdown.ui.components.rememberNowState
import com.prettycountdown.util.CountdownMath
import com.prettycountdown.util.SmartNotifications
import com.prettycountdown.widget.CountdownWidgetReceiver
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: EventDetailViewModel = viewModel(factory = EventDetailViewModel.factory(context, eventId))
    val event by viewModel.event.collectAsStateWithLifecycle()
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val memberIds by viewModel.memberCollectionIds.collectAsStateWithLifecycle()
    val now by rememberNowState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val ev = event
    if (ev == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(eventId) {
        SettingsRepository.getInstance(context).setLastViewedEvent(eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ev.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { onEdit(eventId) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroCountdown(ev, now) }
            item { InfoRow(ev) }
            item { TimelineSection(ev, now) }
            item { MilestonesSection(ev, now) }
            item {
                OutlinedButton(
                    onClick = {
                        val widgetManager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, CountdownWidgetReceiver::class.java)
                        if (widgetManager.isRequestPinAppWidgetSupported) {
                            widgetManager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(context, "Add a Pretty Countdown widget from your launcher's widget picker.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Widgets, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Home Screen")
                }
            }
            item {
                NotesSection(initialNotes = ev.notes, onSave = viewModel::updateNotes)
            }
            item {
                ChecklistSection(
                    items = checklist,
                    onToggle = viewModel::toggleChecklistItem,
                    onDelete = viewModel::deleteChecklistItem,
                    onAdd = viewModel::addChecklistItem
                )
            }
            if (collections.isNotEmpty()) {
                item {
                    CollectionsSection(
                        collections = collections,
                        memberIds = memberIds,
                        onToggle = viewModel::toggleCollection
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete \"${ev.name}\"?") },
            text = { Text("This will permanently remove the event, its notes and checklist.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.deleteEvent()
                        onDeleted()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HeroCountdown(event: Event, now: Long) {
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val background = Color(palette.background)
    val onBackground = Color(palette.onBackground)
    val accent = Color(palette.primary)
    val hasPhoto = event.photoUri != null
    val foreground = if (hasPhoto) Color.White else onBackground
    val highlight = if (hasPhoto) Color.White else accent

    val breakdown = CountdownMath.breakdown(event.targetDateTime, now)
    val progress = CountdownMath.progressFraction(event.startDateTime, event.targetDateTime, now)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "hero-progress")

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = background)) {
        Box {
            if (hasPhoto) {
                Image(
                    painter = rememberAsyncImagePainter(event.photoUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
                Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black.copy(alpha = 0.35f)))
            }
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "${event.category.emoji}  ${event.category.displayName}",
                    color = foreground,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (breakdown.isPast) {
                    Text("🎉 It's happening!", color = highlight, style = MaterialTheme.typography.displaySmall)
                } else {
                    FlipCountdownClock(breakdown = breakdown, accentColor = highlight, labelColor = foreground.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = highlight,
                    trackColor = highlight.copy(alpha = 0.18f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(animatedProgress * 100).roundToInt()}% of the way there",
                    color = foreground.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(event: Event) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(CountdownMath.formatDate(event.targetDateTime, event.hasTime), style = MaterialTheme.typography.bodyLarge)
        if (!event.location.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(event.location, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (event.isRecurringYearly) {
            Text("🔁 Repeats every year", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimelineSection(event: Event, now: Long) {
    val palette = ColorPalettes.byKey(event.colorPaletteKey)
    val accent = Color(palette.primary)
    val progress = CountdownMath.progressFraction(event.startDateTime, event.targetDateTime, now)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "timeline-progress")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Timeline", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Started", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CountdownMath.formatShortDate(event.startDateTime), style = MaterialTheme.typography.bodyMedium)
                }
                Text("${(animatedProgress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, color = accent)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CountdownMath.formatShortDate(event.targetDateTime), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MilestonesSection(event: Event, now: Long) {
    val daysRemaining = CountdownMath.daysRemaining(event.targetDateTime, now).toInt()
    val totalDays = CountdownMath.daysRemaining(event.targetDateTime, event.startDateTime).toInt()
    val milestones = SmartNotifications.milestones.filter { it <= totalDays }.sortedDescending()
    if (milestones.isEmpty()) return

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Milestones", style = MaterialTheme.typography.titleMedium)
            milestones.forEach { milestone ->
                val reached = daysRemaining <= milestone
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (reached) "✅" else "⬜", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        if (milestone == 0) "Event day" else "$milestone Days Left",
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (reached) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reached) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesSection(initialNotes: String, onSave: (String) -> Unit) {
    var text by remember(initialNotes) { mutableStateOf(initialNotes) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Notes", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Things to pack, plans, reminders...") },
                minLines = 3
            )
            TextButton(onClick = { onSave(text) }, modifier = Modifier.align(Alignment.End)) {
                Text("Save Notes")
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    items: List<ChecklistItem>,
    onToggle: (ChecklistItem) -> Unit,
    onDelete: (ChecklistItem) -> Unit,
    onAdd: (String) -> Unit,
) {
    var newItemText by remember { mutableStateOf("") }
    val progress = if (items.isEmpty()) 0 else (items.count { it.isDone } * 100 / items.size)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Checklist", style = MaterialTheme.typography.titleMedium)
                if (items.isNotEmpty()) {
                    Text("$progress% Ready", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.isDone, onCheckedChange = { onToggle(item) })
                    Text(
                        item.text,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a task") },
                    singleLine = true
                )
                IconButton(onClick = {
                    onAdd(newItemText)
                    newItemText = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add task")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionsSection(
    collections: List<com.prettycountdown.data.model.EventCollection>,
    memberIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Collections", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                collections.forEach { collection ->
                    FilterChip(
                        selected = collection.id in memberIds,
                        onClick = { onToggle(collection.id) },
                        label = { Text("${collection.emoji} ${collection.name}") }
                    )
                }
            }
        }
    }
}
