package com.aks.gallerysorter.ui.dupes

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.SecureWhile
import com.aks.gallerysorter.ui.ThumbCell
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.trashRequest
import kotlinx.coroutines.launch

/**
 * Duplicate groups side by side; the kept items are outlined ("Keep"), tap toggles. "Delete N
 * suggested" opens the system trash dialog for everything not kept; nothing is deleted without it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val vm = graphViewModel { g -> DuplicatesViewModel({ g.db }, g.lockState) }
    val state by vm.state.collectAsState()
    val unlocked by graph.lockState.includeLocked.collectAsState()
    val scope = rememberCoroutineScope()
    SecureWhile(unlocked)
    val trash = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) graph.notify("Moved to trash")
    }
    val toDelete = state.groups.sumOf { g -> g.items.count { it.media.id !in g.keep } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (toDelete > 0 && state.missingKeep.isEmpty()) {
                        TextButton(onClick = {
                            scope.launch { trashRequest(graph, vm.toDelete())?.let { trash.launch(IntentSenderRequest.Builder(it).build()) } }
                        }) { Text("Delete $toDelete suggested") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.loaded && state.groups.isEmpty()) Text("No duplicates found", Modifier.align(Alignment.Center))
            if (state.missingKeep.isNotEmpty()) {
                Text(
                    "Keep at least one copy in each group to delete the rest",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.groups, key = { it.id }) { g ->
                    Column(Modifier.padding(8.dp)) {
                        Text("${g.items.size} copies · tap to choose what to keep", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            g.items.forEach { item ->
                                val keep = item.media.id in g.keep
                                Column(Modifier.width(140.dp)) {
                                    ThumbCell(
                                        key = item.media.id, selected = keep, isVideo = item.media.kind == MediaKind.VIDEO,
                                        onClick = { vm.toggleKeep(g.id, item.media.id) }, onLongClick = { vm.toggleKeep(g.id, item.media.id) },
                                        load = { graph.thumbnails.media(item.media) },
                                    )
                                    Text(if (keep) "Keep" else "Delete", style = MaterialTheme.typography.labelMedium)
                                    Text("${item.media.width}×${item.media.height}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
