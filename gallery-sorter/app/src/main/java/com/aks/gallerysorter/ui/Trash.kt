package com.aks.gallerysorter.ui

import android.content.ContentUris
import android.content.IntentSender
import android.provider.MediaStore
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The system trash confirmation for [ids] (MediaStore.createTrashRequest, undoable for ~30 days).
 * Never deletes by itself: the caller launches the returned sender and the user confirms.
 */
suspend fun trashRequest(graph: AppGraph, ids: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
    val uris = ids.mapNotNull { graph.db.mediaDao().get(it) }.map { row ->
        val base = if (row.kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ContentUris.withAppendedId(base, row.id)
    }
    if (uris.isEmpty()) null else MediaStore.createTrashRequest(graph.appContext.contentResolver, uris, true).intentSender
}
