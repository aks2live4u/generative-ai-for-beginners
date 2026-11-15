package com.aks.gallerysorter.scan

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Watches MediaStore images and videos; a burst of changes enqueues one [ScanWorker] run once things have
 * been quiet for [DEBOUNCE_MS]. Callbacks arrive on the main looper, so [pending] is only touched there.
 */
class MediaObserver(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private val app = context.applicationContext
    private var pending: Job? = null

    fun register() {
        app.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, this)
        app.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, this)
    }

    fun unregister() {
        app.contentResolver.unregisterContentObserver(this)
        pending?.cancel()
    }

    override fun onChange(selfChange: Boolean) {
        pending?.cancel()
        pending = scope.launch {
            delay(DEBOUNCE_MS)
            try {
                ScanWorker.enqueueOnChange(app)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "cannot enqueue scan: ${e.javaClass.name}")
            }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 10_000L
    }
}
