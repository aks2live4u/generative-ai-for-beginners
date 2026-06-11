package com.prettycountdown.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prettycountdown.data.model.Event
import com.prettycountdown.ui.components.rememberNowState
import com.prettycountdown.util.CountdownMath
import com.prettycountdown.util.SmartNotifications

/** A preview of every milestone notification still ahead, grouped by event. */
@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.factory(context))
    val events by viewModel.events.collectAsStateWithLifecycle()
    val now by rememberNowState()

    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }) }) { padding ->
        val upcoming = events.mapNotNull { event ->
            val daysRemaining = CountdownMath.daysRemaining(event.targetDateTime, now).toInt()
            val milestones = SmartNotifications.milestones.filter { it in 0..daysRemaining }.sortedDescending()
            if (milestones.isEmpty()) null else event to milestones
        }

        if (upcoming.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No milestone alerts yet. Create an event to start getting friendly countdown reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(upcoming) { (event, milestones) ->
                    NotificationGroup(event, milestones)
                }
            }
        }
    }
}

@Composable
private fun NotificationGroup(event: Event, milestones: List<Int>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${event.category.emoji} ${event.name}", style = MaterialTheme.typography.titleMedium)
            milestones.forEach { milestone ->
                val message = SmartNotifications.messageFor(event, milestone) ?: return@forEach
                Text(
                    "${message.first} — ${message.second}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
