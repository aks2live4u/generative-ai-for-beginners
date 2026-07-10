package com.aichiefofstaff.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aichiefofstaff.data.db.TaskStatus
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.ui.components.DepthCard
import com.aichiefofstaff.ui.components.PriorityPill
import com.aichiefofstaff.ui.util.LocalAppContainer

@Composable
fun TasksScreen() {
    val container = LocalAppContainer.current
    val viewModel: TasksViewModel = viewModel(factory = viewModelFactory {
        initializer { TasksViewModel(container.taskRepository) }
    })
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (tasks.isEmpty()) {
                Text(
                    "No tasks yet. Tap + or say \"remind me to…\"",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { viewModel.delete(task) }
                        )
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        TaskEditDialog(
            initial = null,
            onDismiss = { showNewDialog = false },
            onSave = {
                viewModel.save(it)
                showNewDialog = false
            }
        )
    }

    editingTask?.let { task ->
        TaskEditDialog(
            initial = task,
            onDismiss = { editingTask = null },
            onSave = {
                viewModel.save(it)
                editingTask = null
            }
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    DepthCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null
                )
            }
            PriorityPill(task.priority, modifier = Modifier.padding(end = 4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}
