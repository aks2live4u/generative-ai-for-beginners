package com.aichiefofstaff.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun SearchScreen() {
    val container = LocalAppContainer.current
    val viewModel: SearchViewModel = viewModel(factory = viewModelFactory {
        initializer {
            SearchViewModel(
                container.taskRepository,
                container.projectRepository,
                container.chatRepository,
                container.inboxRepository
            )
        }
    })
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search tasks, projects, chats, inbox…") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.isBlank()) {
                item { Text("Search everything you've captured.", style = MaterialTheme.typography.bodyMedium) }
            } else if (results.isEmpty) {
                item { Text("No results for \"$query\".", style = MaterialTheme.typography.bodyMedium) }
            } else {
                if (results.tasks.isNotEmpty()) {
                    item { SectionLabel("Tasks") }
                    items(results.tasks, key = { "task_${it.id}" }) { ResultRow(it.title, "Task") }
                }
                if (results.projects.isNotEmpty()) {
                    item { SectionLabel("Projects") }
                    items(results.projects, key = { "project_${it.id}" }) { ResultRow(it.name, "Project") }
                }
                if (results.messages.isNotEmpty()) {
                    item { SectionLabel("AI conversations") }
                    items(results.messages, key = { "message_${it.id}" }) { ResultRow(it.content, "Message") }
                }
                if (results.inboxItems.isNotEmpty()) {
                    item { SectionLabel("Inbox") }
                    items(results.inboxItems, key = { "inbox_${it.id}" }) { ResultRow(it.content, "Inbox") }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ResultRow(text: String, category: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(text, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(category, style = MaterialTheme.typography.labelSmall)
        }
    }
}
