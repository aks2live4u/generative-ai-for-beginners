package com.aks.gallerysorter.ui.grid

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.LockedPanel
import com.aks.gallerysorter.ui.SecureWhile
import com.aks.gallerysorter.ui.ThumbCell
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.label
import kotlinx.coroutines.launch

private sealed interface GridDialog {
    class Hide(val ids: List<Long>, val tier: VaultTier) : GridDialog
    class Album(val ids: List<Long>) : GridDialog
    class NotThis(val ids: List<Long>) : GridDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(source: GridSource, title: String, onOpen: (index: Int) -> Unit, onBack: () -> Unit) {
    val vm = graphViewModel { GridViewModel(source, it) }
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val locked by vm.locked.collectAsState()
    val unlocked by vm.unlocked.collectAsState()
    val selection by vm.selection.collectAsState()
    val busy by vm.busy.collectAsState()
    val albums by vm.albums.collectAsState()
    val items = vm.items.collectAsLazyPagingItems()
    var dialog by remember { mutableStateOf<GridDialog?>(null) }
    val category = (source as? GridSource.OfCategory)?.category

    // Sensitive thumbnails can appear in any grid while unlocked.
    SecureWhile(source.requiresUnlock || unlocked)
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    val trash = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) graph.notify("Moved to trash")
    }
    fun askHide(ids: List<Long>) = scope.launch { if (ids.isNotEmpty()) dialog = GridDialog.Hide(ids, vm.defaultTier()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selection.isEmpty()) title else "${selection.size} selected") },
                navigationIcon = {
                    if (selection.isEmpty()) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    } else {
                        IconButton(onClick = vm::clearSelection) { Icon(Icons.Filled.Close, "Clear selection") }
                    }
                },
                actions = {
                    if (!locked && selection.isEmpty() && items.itemCount > 0) {
                        TextButton(onClick = { scope.launch { askHide(vm.allIds()) } }) { Text("Hide all (${items.itemCount})") }
                    }
                },
            )
        },
        bottomBar = {
            if (selection.isNotEmpty() && !locked) {
                BottomAppBar {
                    val ids = selection.toList()
                    TextButton(onClick = { askHide(ids) }) { Text("Hide") }
                    TextButton(onClick = { dialog = GridDialog.Album(ids) }) { Text("Album") }
                    TextButton(onClick = {
                        scope.launch { vm.trashRequest(ids)?.let { trash.launch(IntentSenderRequest.Builder(it).build()) } }
                        vm.clearSelection()
                    }) { Text("Delete") }
                    if (category != null) TextButton(onClick = { dialog = GridDialog.NotThis(ids) }) { Text("Not ${category.label}") }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (locked) {
                LockedPanel("$title is locked")
            } else {
                LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.fillMaxSize()) {
                    items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                        val row = items[index]
                        if (row != null) {
                            ThumbCell(
                                key = row.id, selected = row.id in selection, isVideo = row.kind == MediaKind.VIDEO,
                                onClick = { if (selection.isEmpty()) onOpen(index) else vm.toggle(row.id) },
                                onLongClick = { vm.toggle(row.id) },
                                load = { graph.thumbnails.media(row) },
                            )
                        }
                    }
                }
                if (items.itemCount == 0) Text("Nothing here yet", Modifier.align(Alignment.Center))
            }
            busy?.let { text ->
                Column(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    LinearProgressIndicator()
                    Text(text, Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    when (val d = dialog) {
        is GridDialog.Hide -> HideDialog(d.ids.size, d.tier, vm::vaultExists, onHide = { vm.hide(d.ids, it); dialog = null }, onDismiss = { dialog = null })
        is GridDialog.Album -> AlbumDialog(albums, onPick = { id, name -> vm.addToAlbum(d.ids, id, name); dialog = null }, onDismiss = { dialog = null })
        is GridDialog.NotThis -> ConfirmDialog(
            title = "Not ${category?.label}?",
            text = "These ${d.ids.size} items will never be put in this category again.",
            confirm = "Remove",
            onConfirm = { vm.notThisCategory(d.ids); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}
