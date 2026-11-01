package com.focus.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF0E0E0E)
val Cream = Color(0xFFE8E2D6)
val Muted = Color(0xFF9A958C)
val Sage = Color(0xFFA8C3A0)

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cream,
            onPrimary = Ink,
            secondary = Sage,
            background = Ink,
            surface = Color(0xFF1A1A1A),
            onSurface = Cream,
            onBackground = Cream,
        ),
        content = content,
    )
}

/** Searchable checklist of apps. [keyOf] decides whether selection is by component key or package. */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    initiallySelected: Set<String>,
    keyOf: (AppEntry) -> String,
    maxSelected: Int = Int.MAX_VALUE,
    onDismiss: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }
    val shown = apps.filter { it.label.contains(query, ignoreCase = true) }.distinctBy(keyOf)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (maxSelected != Int.MAX_VALUE) {
                    Text(
                        "${selected.size} / $maxSelected chosen — fewer is calmer",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                LazyColumn(Modifier.height(380.dp).padding(top = 8.dp)) {
                    items(shown, key = { keyOf(it) }) { app ->
                        val key = keyOf(app)
                        val checked = key in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) selected - key
                                    else if (selected.size < maxSelected) selected + key
                                    else selected
                                },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                            Column {
                                Text(app.label)
                                if (app.kind.isDistracting) {
                                    Text(app.kind.tag, color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(selected) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun Swatch(colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val brush = if (colors.size > 1) Brush.verticalGradient(colors) else Brush.linearGradient(listOf(colors[0], colors[0]))
    Box(
        Modifier
            .padding(4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(brush)
            .border(if (selected) 3.dp else 1.dp, if (selected) Cream else Color(0x55FFFFFF), CircleShape)
            .clickable(onClick = onClick)
    )
}
