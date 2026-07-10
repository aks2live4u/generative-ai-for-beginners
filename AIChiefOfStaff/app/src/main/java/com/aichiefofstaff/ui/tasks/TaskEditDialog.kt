package com.aichiefofstaff.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aichiefofstaff.data.db.TaskPriority
import com.aichiefofstaff.data.db.entity.TaskEntity
import java.util.Calendar

@Composable
fun TaskEditDialog(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var priority by remember { mutableStateOf(initial?.priority ?: TaskPriority.MEDIUM) }
    var dueAt by remember { mutableStateOf(initial?.dueAt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }

                Text("Due")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = dueAt == null, onClick = { dueAt = null }, label = { Text("None") })
                    FilterChip(
                        selected = false,
                        onClick = { dueAt = endOfDayOffset(0) },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { dueAt = endOfDayOffset(1) },
                        label = { Text("Tomorrow") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { dueAt = endOfDayOffset(7) },
                        label = { Text("Next week") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        (initial ?: TaskEntity(title = title)).copy(
                            title = title,
                            notes = notes,
                            priority = priority,
                            dueAt = dueAt
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun endOfDayOffset(daysFromNow: Int): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromNow)
        set(Calendar.HOUR_OF_DAY, 17)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis
