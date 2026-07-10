package com.aichiefofstaff.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.ui.components.DepthCard
import com.aichiefofstaff.ui.util.LocalAppContainer

@Composable
fun InboxScreen() {
    val container = LocalAppContainer.current
    val viewModel: InboxViewModel = viewModel(factory = viewModelFactory {
        initializer { InboxViewModel(container.inboxRepository, container.taskRepository) }
    })
    val items by viewModel.items.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Capture a thought…") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.captureText(draft)
                draft = ""
            }) {
                Icon(Icons.Filled.Send, contentDescription = "Capture")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                DepthCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.content, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { viewModel.convertToTask(item) }) {
                                Text("Make task")
                            }
                            IconButton(onClick = { viewModel.delete(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Nothing in your inbox. Capture anything here first — voice notes, ideas, links.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
