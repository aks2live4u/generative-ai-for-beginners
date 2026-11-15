package com.aks.gallerysorter.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aks.gallerysorter.R
import com.aks.gallerysorter.graph
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Foreground service (type dataSync) that runs a scan with a neutral progress notification, so vivo's
 * battery manager doesn't kill a long first scan (spec 6.2). Started from the UI and by [ScanWorker].
 */
class ScanService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var lastPercent = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(0), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (job?.isActive != true) {
            job = scope.launch {
                try {
                    runScan(this@ScanService) { done, total -> onProgress(done, total) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "scan failed: ${e.javaClass.name}")
                } finally {
                    ServiceCompat.stopForeground(this@ScanService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    /** Android 15 caps dataSync services at 6 h a day; stop cleanly, the scan resumes on the next run. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        job?.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun onProgress(done: Int, total: Int) {
        val percent = if (total == 0) 100 else done * 100 / total
        if (percent == lastPercent) return
        lastPercent = percent
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(percent))
    }

    /** Neutral text only: no thumbnails, no large icon, no item names. */
    private fun notification(percent: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(getString(R.string.scan_progress, percent))
        .setProgress(100, percent, false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .build()

    companion object {
        const val CHANNEL_ID = "scan"
        private const val NOTIFICATION_ID = 1001

        /** One scan at a time per process, whether in this service or inline in [ScanWorker]. */
        private val runLock = Mutex()

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ScanService::class.java))
        }

        /** Runs one scan (after startup cleanup); after it completes the first-scan flag is set and the periodic job rescheduled. */
        suspend fun runScan(context: Context, progress: (done: Int, total: Int) -> Unit) = runLock.withLock {
            val graph = context.graph
            graph.awaitStartup() // vault partials are cleaned up before any scan (and its auto-hide) runs
            graph.newScanPipeline().use { it.runIncremental(progress) }
            graph.scanSettings.markFirstScanDone()
            ScanWorker.schedulePeriodic(context)
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, context.getString(R.string.scan_channel_name), NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
