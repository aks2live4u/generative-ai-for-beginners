package com.aks.gallerysorter.ui

import android.app.Activity
import android.content.ContentUris
import android.content.IntentSender
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** At most this many items per system trash request, so a large request stays under the binder limit. */
const val TRASH_CHUNK = 500

/**
 * The system trash confirmations for [ids] (MediaStore.createTrashRequest, undoable for ~30
 * days), one per [TRASH_CHUNK] items. Never deletes by itself: the caller launches the returned
 * senders one after another ([rememberTrashLauncher]) and the user confirms each.
 */
suspend fun trashRequests(graph: AppGraph, ids: List<Long>): List<IntentSender> = withContext(Dispatchers.IO) {
    val uris = ids.mapNotNull { graph.db.mediaDao().get(it) }.map { row ->
        val base = if (row.kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ContentUris.withAppendedId(base, row.id)
    }
    uris.chunked(TRASH_CHUNK).map { MediaStore.createTrashRequest(graph.appContext.contentResolver, it, true).intentSender }
}

/**
 * Shows the system trash dialogs for a list of senders sequentially: the next one opens only
 * after the previous was confirmed; a cancel stops the rest. [onResult] gets (confirmed, total).
 */
@Composable
fun rememberTrashLauncher(onResult: (confirmed: Int, total: Int) -> Unit): (List<IntentSender>) -> Unit {
    val queue = remember { ArrayDeque<IntentSender>() }
    val progress = remember { IntArray(2) } // confirmed, total
    val holder = remember { arrayOfNulls<ActivityResultLauncher<IntentSenderRequest>>(1) }
    val report by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) progress[0]++ else queue.clear()
        val next = queue.removeFirstOrNull()
        if (next != null) holder[0]?.launch(IntentSenderRequest.Builder(next).build()) else report(progress[0], progress[1])
    }
    holder[0] = launcher
    return remember(launcher) {
        { senders ->
            if (senders.isNotEmpty()) {
                queue.clear()
                queue.addAll(senders.drop(1))
                progress[0] = 0
                progress[1] = senders.size
                launcher.launch(IntentSenderRequest.Builder(senders.first()).build())
            }
        }
    }
}

/** The snackbar text after [rememberTrashLauncher] finished, or null if nothing was confirmed. */
fun trashNotice(confirmed: Int, total: Int): String? = when {
    confirmed == 0 -> null
    confirmed == total -> "Moved to trash"
    else -> "Some items moved to trash; the rest were kept"
}
