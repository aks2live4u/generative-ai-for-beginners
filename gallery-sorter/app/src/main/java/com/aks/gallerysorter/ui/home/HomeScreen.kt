package com.aks.gallerysorter.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.scan.ScanWorker
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.grid.ConfirmDialog
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.ui.unlock
import kotlinx.coroutines.launch

/** Where Home navigates; routes are owned by AppNav. */
class HomeActions(
    val category: (Category) -> Unit,
    val album: (Long) -> Unit,
    val people: () -> Unit,
    val places: () -> Unit,
    val duplicates: () -> Unit,
    val review: () -> Unit,
    val vault: () -> Unit,
    val settings: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(actions: HomeActions) {
    val graph = LocalContext.current.graph
    val context = LocalContext.current
    val vm = graphViewModel { HomeViewModel(it.db.labelDao(), it.db.mediaDao(), it.db.peopleDao(), it.db.dupeDao(), it.scanSettings, it.lockState) }
    val state by vm.state.collectAsState()
    val albums by remember { graph.db.albumDao().albums() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var chargerPrompt by rememberSaveable { mutableStateOf(false) }

    fun startScan() = scope.launch {
        if (graph.scanNeedsCharger()) {
            chargerPrompt = true
        } else {
            graph.startScan()
            graph.notify("Scan started")
        }
    }
    /** Locked destinations unlock first, then open. */
    fun guarded(locked: Boolean, go: () -> Unit) {
        if (!locked) go() else scope.launch { if (unlock(context)) go() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery Sorter") },
                actions = {
                    TextButton(onClick = { startScan() }) { Text("Scan") }
                    IconButton(onClick = actions.settings) { Icon(Icons.Filled.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(GridCells.Adaptive(112.dp), Modifier.padding(padding).fillMaxSize()) {
            header("Browse")
            item { EntryTile("People", "${state.people}", onClick = actions.people) }
            item { EntryTile("Places", "${state.cities} places · ${state.trips} trips", onClick = actions.places) }
            item { EntryTile("Duplicates", "${state.duplicateGroups} groups", onClick = actions.duplicates) }
            item { EntryTile("Review", "${state.review}", locked = !state.unlocked) { guarded(!state.unlocked, actions.review) } }
            item { EntryTile("Vault", "", locked = !state.unlocked, onClick = actions.vault) }
            if (albums.isNotEmpty()) {
                header("Albums")
                items(albums, key = { "album${it.id}" }) { a -> EntryTile(a.name, "", onClick = { actions.album(a.id) }) }
            }
            for (group in state.groups) {
                header(group.group.label)
                items(group.tiles, key = { it.category.name }) { tile ->
                    CategoryTileView(tile) { guarded(tile.locked) { actions.category(tile.category) } }
                }
            }
        }
    }

    if (chargerPrompt) {
        ConfirmDialog(
            title = "Waiting for the charger",
            text = "The first full scan is set to run only while charging. Plug in and it starts by itself, or start it now.",
            confirm = "Start anyway",
            onConfirm = { chargerPrompt = false; graph.startScan(); graph.notify("Scan started") },
            onDismiss = {
                chargerPrompt = false
                graph.appScope.launch { runCatching { ScanWorker.enqueueOnChange(graph.appContext) } }
            },
        )
    }
}

private fun LazyGridScope.header(text: String) {
    item(span = { GridItemSpan(maxLineSpan) }, key = "h$text") {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp))
    }
}

@Composable
private fun EntryTile(title: String, subtitle: String, locked: Boolean = false, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(4.dp).fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (locked) Icon(Icons.Filled.Lock, contentDescription = "Locked")
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A locked tile shows only the lock and the count, never a thumbnail. */
@Composable
private fun CategoryTileView(tile: CategoryTile, onClick: () -> Unit) {
    val graph = LocalContext.current.graph
    val previewId = tile.previewIds.firstOrNull()
    val bmp by produceState<Bitmap?>(null, previewId, tile.locked) {
        value = if (tile.locked || previewId == null) null else graph.db.mediaDao().get(previewId)?.let { graph.thumbnails.media(it) }
    }
    Card(onClick = onClick, modifier = Modifier.padding(4.dp).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val b = bmp
            if (tile.locked) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.align(Alignment.Center))
            } else if (b != null) {
                Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(tile.category.label, style = MaterialTheme.typography.titleSmall)
            Text("${tile.count}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
