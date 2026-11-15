package com.aks.gallerysorter.ui

import android.util.Log
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.scan.ScanWorker
import kotlin.coroutines.cancellation.CancellationException

/**
 * Re-runs categorization for [ids]: they go back to PENDING and a scan is enqueued, which
 * re-analyses them with the current settings, people and overrides. Call on IO.
 */
suspend fun recategorize(graph: AppGraph, ids: Collection<Long>) {
    if (ids.isEmpty()) return
    val dao = graph.db.mediaDao()
    for (id in ids) dao.setStatus(id, ScanStatus.PENDING)
    try {
        ScanWorker.enqueueOnChange(graph.appContext)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("Recategorize", "cannot enqueue re-categorize: ${e.javaClass.name}")
    }
}
