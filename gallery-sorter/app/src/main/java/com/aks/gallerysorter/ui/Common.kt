package com.aks.gallerysorter.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A ViewModel built from the app graph, scoped to the current navigation destination. */
@Composable
inline fun <reified VM : ViewModel> graphViewModel(crossinline create: (AppGraph) -> VM): VM {
    val graph = LocalContext.current.graph
    return viewModel(factory = viewModelFactory { initializer { create(graph) } })
}

tailrec fun Context.fragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.fragmentActivity()
    else -> null
}

/**
 * Unlocks [tier] (or the first vault that exists) through the BiometricGate. Locked categories
 * open once any tier is unlocked. Must run on the main thread.
 */
suspend fun unlock(context: Context, tier: VaultTier? = null): Boolean {
    val graph = context.graph
    val activity = context.fragmentActivity() ?: return false
    // Key files and the (lazy) index are touched on IO; only the prompt itself runs on main.
    val created = graph.createdTiers()
    val target = tier ?: VaultTier.entries.firstOrNull { it in created }
    if (target == null || target !in created) {
        graph.notify("Set up a vault PIN first")
        return false
    }
    val gate = withContext(Dispatchers.IO) { graph.biometricGate }
    return gate.prompt(activity, target) != null
}

/** Shown instead of a locked screen's content. */
@Composable
fun LockedPanel(text: String = "Locked", tier: VaultTier? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null)
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        Button(onClick = { scope.launch { unlock(context, tier) } }) { Text("Unlock") }
    }
}

/** One square grid cell; [load] supplies the thumbnail (null → blank). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThumbCell(
    key: Any,
    selected: Boolean,
    isVideo: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    load: suspend () -> Bitmap?,
) {
    val bmp by produceState<Bitmap?>(null, key) {
        value = null // no stale thumbnail for a new key
        value = load()
    }
    Box(
        Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        bmp?.let {
            Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (isVideo) Icon(Icons.Filled.PlayArrow, null, Modifier.align(Alignment.Center))
        if (selected) Icon(Icons.Filled.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(4.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

/** Sets FLAG_SECURE while composed and [secure] is true, without re-creating the caller's content. */
@Composable
fun SecureWhile(secure: Boolean) {
    if (secure) SecureScreen {}
}
