package com.aks.gallerysorter.ui.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.LockedPanel
import com.aks.gallerysorter.ui.SecureScreen
import com.aks.gallerysorter.ui.ThumbCell
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.grid.ConfirmDialog
import com.aks.gallerysorter.ui.label

/** The vault: Standard and High-security tabs, each unlocked separately. Always FLAG_SECURE. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(onOpen: (VaultTier, Int) -> Unit, onBack: () -> Unit) = SecureScreen {
    val vm = graphViewModel { VaultViewModel(it) }
    val graph = LocalContext.current.graph
    var tab by rememberSaveable { mutableStateOf(VaultTier.STANDARD) }
    val unlocked by vm.unlocked.collectAsState()
    val selection by vm.selection.collectAsState()
    val busy by vm.busy.collectAsState()
    val rows by vm.rows.getValue(tab).collectAsState()
    var confirmUnhide by rememberSaveable { mutableStateOf(false) }
    val open = tab in unlocked
    BackHandler(enabled = selection.isNotEmpty()) { vm.clearSelection() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selection.isEmpty()) "Vault" else "${selection.size} selected") },
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
            if (selection.isNotEmpty() && open) {
                BottomAppBar {
                    TextButton(onClick = {
                        if (vm.anySensitive(tab, selection)) confirmUnhide = true else vm.unhide(tab, selection.toList())
                    }) { Text("Unhide") }
                    TextButton(onClick = { vm.moveTier(tab, selection.toList()) }) {
                        Text("Move to ${VaultViewModel.other(tab).label.lowercase()}")
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab.ordinal) {
                VaultTier.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t; vm.clearSelection() }, text = { Text(t.label) })
                }
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    !vm.exists(tab) -> Text(
                        "The ${tab.label.lowercase()} vault isn't set up yet. Set it up in Settings.",
                        Modifier.align(Alignment.Center).padding(24.dp),
                    )
                    !open -> LockedPanel("${tab.label} vault is locked", tier = tab)
                    else -> {
                        val priv = graph.lockState.privateHandle(tab)
                        LazyVerticalGrid(GridCells.Adaptive(96.dp), Modifier.fillMaxSize()) {
                            itemsIndexed(rows, key = { _, r -> r.id }) { index, row ->
                                ThumbCell(
                                    key = row.id, selected = row.id in selection, isVideo = row.kind == MediaKind.VIDEO,
                                    onClick = { if (selection.isEmpty()) onOpen(tab, index) else vm.toggle(row.id) },
                                    onLongClick = { vm.toggle(row.id) },
                                    load = { priv?.let { graph.thumbnails.vault(row, it) } },
                                )
                            }
                        }
                        if (rows.isEmpty()) Text("Nothing hidden here", Modifier.align(Alignment.Center))
                    }
                }
                busy?.let { text ->
                    Column(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                        LinearProgressIndicator()
                        Text(text, Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    if (confirmUnhide) {
        ConfirmDialog(
            title = "Unhide sensitive items?",
            text = "Restored files are visible to other gallery apps again, and cloud backup (such as Google Photos) may upload them.",
            confirm = "Unhide",
            onConfirm = { confirmUnhide = false; vm.unhide(tab, selection.toList()) },
            onDismiss = { confirmUnhide = false },
        )
    }
}
