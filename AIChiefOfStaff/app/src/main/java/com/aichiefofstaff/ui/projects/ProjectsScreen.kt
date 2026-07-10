package com.aichiefofstaff.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.aichiefofstaff.data.db.entity.ProjectEntity
import com.aichiefofstaff.ui.components.DepthCard
import com.aichiefofstaff.ui.util.LocalAppContainer

@Composable
fun ProjectsScreen(onOpenProject: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ProjectsViewModel = viewModel(factory = viewModelFactory {
        initializer { ProjectsViewModel(container.projectRepository) }
    })
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add project")
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Text("No projects yet. Tap + to start one.", modifier = Modifier.padding(padding).padding(24.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectRow(
                        project = project,
                        onClick = { onOpenProject(project.id) },
                        onArchive = { viewModel.archive(project) }
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("New project") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(name, description)
                    showNewDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectRow(
    project: ProjectEntity,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    DepthCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                Text(project.name, fontWeight = FontWeight.Medium)
                if (project.description.isNotBlank()) {
                    Text(project.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Filled.Archive, contentDescription = "Archive project")
            }
        }
    }
}
