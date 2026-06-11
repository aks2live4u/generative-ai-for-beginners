package com.prettycountdown.ui.createevent

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.prettycountdown.data.model.ColorPalette
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.EventCategory
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.util.AISuggestions
import com.prettycountdown.util.CountdownMath
import com.prettycountdown.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    eventId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: CreateEventViewModel = viewModel(factory = CreateEventViewModel.factory(context, eventId))
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.updatePhotoUri(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Event" else "New Event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.save()
                            WidgetUpdater.refreshAll(context)
                            onDone()
                        }
                    }) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            NameSection(state.name, state.category, viewModel::updateName)
            CategorySection(state.category, viewModel::updateCategory)
            DateTimeSection(state, viewModel::updateDate, viewModel::updateTime, viewModel::updateHasTime)
            LocationSection(state.location, viewModel::updateLocation)
            PhotoSection(
                photoUri = state.photoUri,
                onPick = { photoPicker.launch("image/*") },
                onClear = { viewModel.updatePhotoUri(null) }
            )
            ColorThemeSection(state.category, state.colorPaletteKey, viewModel::updateColorPalette)
            CountdownFormatSection(state.countdownFormat, viewModel::updateCountdownFormat)
            WidgetStyleSection(state.widgetStyle, viewModel::updateWidgetStyle)
            RecurringSection(state.isRecurringYearly, viewModel::updateRecurring)
            NotesSection(state.notes, viewModel::updateNotes)
            Box(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun NameSection(name: String, category: EventCategory, onNameChange: (String) -> Unit) {
    var suggestionIndex by rememberSaveable { mutableStateOf(0) }
    val suggestions = remember(category) { AISuggestions.nameSuggestions(category) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Event Name")
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Trip to Japan") },
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            AssistChipRow(
                text = suggestions[suggestionIndex % suggestions.size],
                onClick = { onNameChange(suggestions[suggestionIndex % suggestions.size]) },
                onShuffle = { suggestionIndex++ }
            )
        }
    }
}

@Composable
private fun AssistChipRow(text: String, onClick: () -> Unit, onShuffle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        IconButton(onClick = onShuffle) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "More suggestions")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(selected: EventCategory, onSelect: (EventCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Category")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EventCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    label = { Text("${category.emoji} ${category.displayName}") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeSection(
    state: CreateEventUiState,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onHasTimeChange: (Boolean) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Date")
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(CountdownMath.formatDate(state.targetDateTime, false))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Add a time", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.hasTime, onCheckedChange = onHasTimeChange)
        }
        if (state.hasTime) {
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.time.toString())
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = state.time.hour, initialMinute = state.time.minute)
        Dialog(onDismissRequest = { showTimePicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Card(modifier = Modifier.padding(24.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(location: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Location (optional)")
        OutlinedTextField(
            value = location,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Tokyo, Japan") },
            singleLine = true
        )
    }
}

@Composable
private fun PhotoSection(photoUri: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Photo (optional)")
        if (photoUri != null) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(photoUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White)
                }
            }
        } else {
            OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Text("  Choose Photo")
            }
        }
    }
}

@Composable
private fun ColorThemeSection(category: EventCategory, selectedKey: String, onSelect: (String) -> Unit) {
    val suggested = remember(category) { AISuggestions.paletteSuggestions(category) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Color Theme")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (suggested + ColorPalettes.all.filter { p -> suggested.none { it.key == p.key } }).forEach { palette ->
                ColorSwatch(palette = palette, selected = palette.key == selectedKey, onClick = { onSelect(palette.key) })
            }
        }
    }
}

@Composable
private fun ColorSwatch(palette: ColorPalette, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(palette.primary))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(modifier = Modifier.size(14.dp).background(Color.White, CircleShape))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountdownFormatSection(selected: CountdownFormat, onSelect: (CountdownFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Countdown Format")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownFormat.entries.forEach { format ->
                FilterChip(
                    selected = format == selected,
                    onClick = { onSelect(format) },
                    label = { Text(format.displayName) }
                )
            }
        }
    }
}

@Composable
private fun WidgetStyleSection(selected: WidgetStyle, onSelect: (WidgetStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Widget Style")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(WidgetStyle.entries.toList()) { style ->
                val isSelected = style == selected
                Card(
                    onClick = { onSelect(style) },
                    modifier = Modifier.width(140.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(style.emoji, style = MaterialTheme.typography.headlineMedium)
                        Text(style.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            style.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringSection(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            SectionTitle("Repeats every year")
            Text(
                "For birthdays, anniversaries and festivals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NotesSection(notes: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Notes")
        OutlinedTextField(
            value = notes,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Things to pack, plans, reminders...") },
            minLines = 3
        )
    }
}
