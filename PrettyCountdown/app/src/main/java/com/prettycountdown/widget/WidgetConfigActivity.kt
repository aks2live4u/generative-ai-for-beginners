package com.prettycountdown.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.ui.theme.PrettyCountdownTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lets the user pick which event (and optionally which style/format) a new widget should show. */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            PrettyCountdownTheme {
                WidgetConfigScreen(
                    onSave = { eventId, style, format -> saveAndFinish(eventId, style, format) },
                )
            }
        }
    }

    private fun saveAndFinish(eventId: Long, style: WidgetStyle?, format: CountdownFormat?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                prefs[WidgetPrefKeys.EVENT_ID] = eventId
                if (style != null) prefs[WidgetPrefKeys.STYLE_OVERRIDE] = style.name else prefs.remove(WidgetPrefKeys.STYLE_OVERRIDE)
                if (format != null) prefs[WidgetPrefKeys.FORMAT_OVERRIDE] = format.name else prefs.remove(WidgetPrefKeys.FORMAT_OVERRIDE)
            }
            CountdownGlanceWidget().update(this@WidgetConfigActivity, glanceId)
            SettingsRepository.getInstance(this@WidgetConfigActivity).setLastViewedEvent(eventId)

            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigScreen(onSave: (Long, WidgetStyle?, CountdownFormat?) -> Unit) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var selectedEventId by remember { mutableStateOf<Long?>(null) }
    var styleOverride by remember { mutableStateOf<WidgetStyle?>(null) }
    var formatOverride by remember { mutableStateOf<CountdownFormat?>(null) }

    LaunchedEffect(Unit) {
        val repository = EventRepository.getInstance(context)
        val all = repository.getAllEventsOnce().sortedBy { it.targetDateTime }
        events = all
        val lastViewed = SettingsRepository.getInstance(context).lastViewedEventId.first()
        selectedEventId = (lastViewed?.takeIf { id -> all.any { it.id == id } } ?: all.firstOrNull()?.id)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configure Widget") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Choose an event", style = MaterialTheme.typography.titleMedium)
            if (events.isEmpty()) {
                Text("Create an event in Pretty Countdown first.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(events, key = { it.id }) { event ->
                        Card(
                            onClick = { selectedEventId = event.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (event.id == selectedEventId) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${event.category.emoji}  ${event.name}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                Text("Widget style (optional override)", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = styleOverride == null, onClick = { styleOverride = null }, label = { Text("Use event's style") })
                    WidgetStyle.entries.forEach { style ->
                        FilterChip(
                            selected = styleOverride == style,
                            onClick = { styleOverride = style },
                            label = { Text("${style.emoji} ${style.displayName}") },
                        )
                    }
                }

                Text("Countdown format (optional override)", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = formatOverride == null, onClick = { formatOverride = null }, label = { Text("Use event's format") })
                    CountdownFormat.entries.forEach { format ->
                        FilterChip(
                            selected = formatOverride == format,
                            onClick = { formatOverride = format },
                            label = { Text(format.displayName) },
                        )
                    }
                }

                Button(
                    onClick = { selectedEventId?.let { onSave(it, styleOverride, formatOverride) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add Widget") }
            }
        }
    }
}
