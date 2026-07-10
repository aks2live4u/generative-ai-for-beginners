package com.aichiefofstaff.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.ui.components.DepthCard
import com.aichiefofstaff.ui.components.GlassStatTile
import com.aichiefofstaff.ui.components.GradientHero
import com.aichiefofstaff.ui.components.PriorityPill
import com.aichiefofstaff.ui.components.priorityColor
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GradientHero(title = greeting(), subtitle = "Here's where things stand") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassStatTile("Open", state.openTaskCount.toString(), modifier = Modifier.width(90.dp))
                    GlassStatTile("Projects", state.projectCount.toString(), modifier = Modifier.width(90.dp))
                    GlassStatTile("Due today", state.dueToday.size.toString(), modifier = Modifier.width(90.dp))
                }
            }
        }

        item { SectionHeader("Today's priorities") }
        if (state.dueToday.isEmpty()) {
            item { EmptyRow("Nothing due today. Enjoy the clear runway.") }
        } else {
            items(state.dueToday, key = { it.id }) { task ->
                DepthCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentBar(color = priorityColor(task.priority))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(task.title, fontWeight = FontWeight.Medium)
                            if (task.notes.isNotBlank()) {
                                Text(task.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        PriorityPill(task.priority)
                    }
                }
            }
        }

        item { SectionHeader("Recent conversations") }
        if (state.recentConversations.isEmpty()) {
            item { EmptyRow("Tap the mic and ask your AI Chief of Staff anything.") }
        } else {
            items(state.recentConversations, key = { it.id }) { conversation ->
                DepthCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenConversation(conversation.id) }) {
                    Column(Modifier.padding(14.dp)) {
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
private fun AccentBar(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = 36.dp)
            .background(color, RoundedCornerShape(4.dp))
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
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
