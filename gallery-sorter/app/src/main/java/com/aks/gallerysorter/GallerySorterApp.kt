package com.aks.gallerysorter

import android.app.Application
import android.util.Log
import com.aks.gallerysorter.lock.AppLockObserver
import com.aks.gallerysorter.scan.MediaObserver
import com.aks.gallerysorter.scan.ScanWorker
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Builds the [AppGraph], then in order: auto-lock on background, vault partial cleanup (scans
 * and hides wait for it), the MediaStore observer, and the periodic scan once the first is done.
 */
class GallerySorterApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        AppLockObserver.install(graph.lockState)
        graph.watchLock()
        graph.runStartupCleanup()
        MediaObserver(this).register()
        graph.appScope.launch {
            try {
                graph.awaitStartup()
                if (graph.scanSettings.firstScanDone()) ScanWorker.schedulePeriodic(this@GallerySorterApp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("GallerySorterApp", "cannot schedule scans: ${e.javaClass.name}")
            }
        }
    }
}
