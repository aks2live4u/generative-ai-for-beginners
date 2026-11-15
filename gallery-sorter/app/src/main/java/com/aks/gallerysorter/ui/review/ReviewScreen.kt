package com.aks.gallerysorter.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.LockedPanel
import com.aks.gallerysorter.ui.SecureScreen
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.ui.viewer.loadGalleryImage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One uncertain item at a time: swipe right or Confirm, swipe left or Reject. Always FLAG_SECURE. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onBack: () -> Unit) = SecureScreen {
    val vm = graphViewModel { g -> ReviewViewModel({ g.db }, g.lockState) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val graph = context.graph
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.locked) "Review" else "Review (${state.remaining})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val item = state.item
            when {
                !state.loaded -> Unit
                state.locked -> LockedPanel("Review is locked", tier = VaultTier.STANDARD)
                item == null -> Text("Nothing to review", Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val bmp by produceState<Bitmap?>(null, item.row.id) {
                        value = null
                        value = try {
                            if (item.row.kind == MediaKind.VIDEO) {
                                graph.thumbnails.media(item.row)
                            } else {
                                withContext(Dispatchers.IO) { loadGalleryImage(context, item.row) }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                    }
                    var drag = 0f
                    Box(
                        Modifier.weight(1f).fillMaxWidth().pointerInput(item.row.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragEnd = {
                                    when {
                                        drag > SWIPE_PX && item.suggested != null -> vm.confirm()
                                        drag < -SWIPE_PX -> vm.reject()
                                    }
                                },
                            ) { _, amount -> drag += amount }
                        },
                    ) {
                        bmp?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                    }
                    Text(
                        item.suggested?.let { "Is this ${it.label}?" } ?: "No suggested category",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.reject() }, modifier = Modifier.weight(1f)) { Text("Reject") }
                        TextButton(onClick = { vm.skip() }) { Text("Skip") }
                        Button(enabled = item.suggested != null, onClick = { vm.confirm() }, modifier = Modifier.weight(1f)) { Text("Confirm") }
                    }
                }
            }
        }
    }
}

private const val SWIPE_PX = 200f
