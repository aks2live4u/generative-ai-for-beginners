package com.aks.gallerysorter.ui.people

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.index.dao.PersonSummary
import com.aks.gallerysorter.ui.SecureWhile
import com.aks.gallerysorter.ui.ThumbCell
import com.aks.gallerysorter.ui.graphViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val PersonSummary.title: String get() = name ?: if (isMe) "Me" else "Unnamed"

private sealed interface PeopleDialog {
    class Rename(val person: PersonSummary) : PeopleDialog
    class Merge(val people: List<PersonSummary>) : PeopleDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(onOpen: (personId: Long) -> Unit, onBack: () -> Unit) {
    val vm = graphViewModel { PeopleViewModel(it) }
    val graph = LocalContext.current.graph
    val people by vm.people.collectAsState()
    val selection by vm.selection.collectAsState()
    val unlocked by graph.lockState.includeLocked.collectAsState()
    var dialog by remember { mutableStateOf<PeopleDialog?>(null) }
    // Covers can be Sensitive items while unlocked.
    SecureWhile(unlocked)
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }
    val selected = people.orEmpty().filter { it.id in selection }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selection.isEmpty()) "People" else "${selection.size} selected") },
                navigationIcon = {
                    if (selection.isEmpty()) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    } else {
                        IconButton(onClick = vm::clearSelection) { Icon(Icons.Filled.Close, "Clear selection") }
                    }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                BottomAppBar {
                    if (selected.size == 1) {
                        TextButton(onClick = { dialog = PeopleDialog.Rename(selected.single()) }) { Text("Name") }
                        if (!selected.single().isMe) TextButton(onClick = { vm.markMe(selected.single().id) }) { Text("This is me") }
                    } else {
                        TextButton(onClick = { dialog = PeopleDialog.Merge(selected) }) { Text("Merge") }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val list = people
            when {
                list == null -> Unit
                list.isEmpty() -> Text("No faces found yet", Modifier.align(Alignment.Center))
                else -> LazyVerticalGrid(GridCells.Adaptive(112.dp), Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { p ->
                        Column(Modifier.padding(4.dp)) {
                            // FaceRow stores no face box, so the cover is the item's thumbnail.
                            ThumbCell(
                                key = p.id to p.coverId, selected = p.id in selection, isVideo = false,
                                onClick = { if (selection.isEmpty()) onOpen(p.id) else vm.toggle(p.id) },
                                onLongClick = { vm.toggle(p.id) },
                                load = { withContext(Dispatchers.IO) { graph.db.mediaDao().get(p.coverId) }?.let { graph.thumbnails.media(it) } },
                            )
                            Text(p.title, style = MaterialTheme.typography.titleSmall)
                            Text("${p.n}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        is PeopleDialog.Rename -> RenameDialog(d.person, onRename = { vm.rename(d.person.id, it); dialog = null }, onDismiss = { dialog = null })
        is PeopleDialog.Merge -> MergeDialog(d.people, onMerge = { into -> vm.merge(d.people.map { it.id }.toSet(), into); dialog = null }, onDismiss = { dialog = null })
        null -> Unit
    }
}

@Composable
private fun RenameDialog(person: PersonSummary, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(person.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onRename(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Merge the selected people into the one tapped (its name and "Me" are kept). */
@Composable
private fun MergeDialog(people: List<PersonSummary>, onMerge: (into: Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge ${people.size} people into…") },
        text = {
            Column {
                people.forEach { p ->
                    Text("${p.title} (${p.n})", Modifier.fillMaxWidth().clickable { onMerge(p.id) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
