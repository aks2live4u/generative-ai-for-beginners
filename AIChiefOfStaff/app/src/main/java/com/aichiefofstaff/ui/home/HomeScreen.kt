package com.aichiefofstaff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.ui.util.LocalAppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(onOpenConversation: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory {
        initializer { HomeViewModel(container.taskRepository, container.projectRepository, container.chatRepository) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(label = "Open tasks", value = state.openTaskCount.toString(), modifier = Modifier)
                StatCard(label = "Projects", value = state.projectCount.toString(), modifier = Modifier)
                StatCard(label = "Due today", value = state.dueToday.size.toString(), modifier = Modifier)
            }
        }

        item { SectionHeader("Today's priorities") }
        if (state.dueToday.isEmpty()) {
            item { EmptyRow("Nothing due today. Enjoy the clear runway.") }
        } else {
            items(state.dueToday, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(task.title, fontWeight = FontWeight.Medium)
                        if (task.notes.isNotBlank()) {
                            Text(task.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Recent conversations") }
        if (state.recentConversations.isEmpty()) {
            item { EmptyRow("Tap the mic and ask your AI Chief of Staff anything.") }
        } else {
            items(state.recentConversations, key = { it.id }) { conversation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenConversation(conversation.id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(conversation.title, fontWeight = FontWeight.Medium)
                        Text(
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(conversation.updatedAt)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
