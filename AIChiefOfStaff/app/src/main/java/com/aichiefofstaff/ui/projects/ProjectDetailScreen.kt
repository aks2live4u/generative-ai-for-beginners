package com.aichiefofstaff.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.ui.util.LocalAppContainer

@Composable
fun ProjectDetailScreen(projectId: Long) {
    val container = LocalAppContainer.current
    val viewModel: ProjectDetailViewModel = viewModel(factory = viewModelFactory {
        initializer { ProjectDetailViewModel(projectId, container.projectRepository, container.taskRepository) }
    })
    val project by viewModel.project.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(project?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
        if (!project?.description.isNullOrBlank()) {
            Text(project?.description.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Add a task to this project") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.addTask(draft)
                draft = ""
            }) {
                Icon(Icons.Filled.Send, contentDescription = "Add task")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(task.title, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
