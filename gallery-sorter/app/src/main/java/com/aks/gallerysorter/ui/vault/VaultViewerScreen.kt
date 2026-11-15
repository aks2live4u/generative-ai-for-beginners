package com.aks.gallerysorter.ui.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.LockedPanel
import com.aks.gallerysorter.ui.SecureScreen
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.viewer.ImageViewer
import com.aks.gallerysorter.ui.viewer.VideoPlayer
import com.aks.gallerysorter.ui.viewer.loadVaultImage
import com.aks.gallerysorter.ui.viewer.vaultVideo

/** Full-screen viewer for one vault tier; content disappears (players released) the moment it locks. */
@Composable
fun VaultViewerScreen(tier: VaultTier, start: Int, onBack: () -> Unit) = SecureScreen {
    val vm = graphViewModel { VaultViewModel(it) }
    val graph = LocalContext.current.graph
    val unlocked by vm.unlocked.collectAsState()
    val rows by vm.rows.getValue(tier).collectAsState()
    val priv = if (tier in unlocked) graph.lockState.privateHandle(tier) else null

    Box(Modifier.fillMaxSize()) {
        when {
            priv == null -> LockedPanel(tier = tier)
            rows.isNotEmpty() -> {
                val pager = rememberPagerState(initialPage = start.coerceIn(0, rows.size - 1)) { rows.size }
                HorizontalPager(pager, Modifier.fillMaxSize(), key = { rows[it].id }) { page ->
                    val row = rows[page]
                    if (row.kind == MediaKind.VIDEO) {
                        VideoPlayer(row.id, active = pager.currentPage == page, source = vaultVideo(priv, row, graph.vaultStore))
                    } else {
                        ImageViewer(row.id) { loadVaultImage(graph.vaultStore, priv, row) }
                    }
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (priv == null) Color.Unspecified else Color.White)
        }
    }
}
