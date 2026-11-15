package com.aks.gallerysorter.ui.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.AlbumRow
import com.aks.gallerysorter.ui.label

/** Confirm a hide of [count] items, choosing the tier ([initial] = the category's default). */
@Composable
fun HideDialog(count: Int, initial: VaultTier, exists: (VaultTier) -> Boolean, onHide: (VaultTier) -> Unit, onDismiss: () -> Unit) {
    var tier by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hide $count item${if (count == 1) "" else "s"}?") },
        text = {
            Column {
                Text("Each file is encrypted into the vault, checked, then removed from the gallery.")
                VaultTier.entries.forEach { t ->
                    Row(Modifier.fillMaxWidth().clickable { tier = t }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tier == t, onClick = { tier = t })
                        Text(t.label + if (exists(t)) "" else " (not set up)")
                    }
                }
                if (tier == VaultTier.HIGH_SECURITY) {
                    Text("High-security items are deleted for good if the app is uninstalled.", Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(enabled = exists(tier), onClick = { onHide(tier) }) { Text("Hide") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Pick an existing album or name a new one. */
@Composable
fun AlbumDialog(albums: List<AlbumRow>, onPick: (albumId: Long?, newName: String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to album") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                albums.forEach { a ->
                    Text(a.name, Modifier.fillMaxWidth().clickable { onPick(a.id, null) }.padding(vertical = 12.dp))
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("New album") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onPick(null, name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
