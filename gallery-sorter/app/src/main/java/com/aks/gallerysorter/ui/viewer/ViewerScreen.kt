package com.aks.gallerysorter.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.ui.LockedPanel
import com.aks.gallerysorter.ui.SecureWhile
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.grid.GridSource
import com.aks.gallerysorter.ui.grid.GridViewModel

/** Full-screen swipe viewer over the same items (and lock filtering) as the grid it was opened from. */
@Composable
fun ViewerScreen(source: GridSource, start: Int, onBack: () -> Unit) {
    val vm = graphViewModel { GridViewModel(source, it) }
    val context = LocalContext.current
    val locked by vm.locked.collectAsState()
    val unlocked by vm.unlocked.collectAsState()
    val items = vm.items.collectAsLazyPagingItems()
    SecureWhile(source.requiresUnlock || unlocked)

    Box(Modifier.fillMaxSize()) {
        when {
            locked -> LockedPanel()
            items.itemCount > 0 -> {
                val pager = rememberPagerState(initialPage = start.coerceIn(0, items.itemCount - 1)) { items.itemCount }
                HorizontalPager(pager, Modifier.fillMaxSize(), key = items.itemKey { it.id }) { page ->
                    val row = items[page] ?: return@HorizontalPager
                    if (row.kind == MediaKind.VIDEO) {
                        VideoPlayer(row.id, active = pager.currentPage == page, source = galleryVideo(row))
                    } else {
                        ImageViewer(row.id) { loadGalleryImage(context, row) }
                    }
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (locked) MaterialTheme.colorScheme.onSurface else Color.White)
        }
    }
}
