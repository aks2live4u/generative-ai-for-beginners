package com.prettycountdown.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prettycountdown.data.EventRepository
import com.prettycountdown.util.CountdownMath
import com.prettycountdown.util.SmartNotifications
import com.prettycountdown.widget.WidgetUpdater
import java.time.Instant
import java.time.ZoneId

/**
 * Runs every 15 minutes (the shortest interval WorkManager allows) to keep the
 * "alive" widget feeling fresh: refreshes every home-screen widget, fires
 * milestone notifications once per day-change, and rolls recurring yearly
 * events forward once their date has passed.
 */
class DailyMaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = EventRepository.getInstance(applicationContext)
        val now = System.currentTimeMillis()

        repository.getAllEventsOnce().forEach { event ->
            var updated = event
            val daysRemaining = CountdownMath.daysRemaining(event.targetDateTime, now).toInt()

            if (event.lastNotifiedDaysRemaining != daysRemaining) {
                SmartNotifications.messageFor(event, daysRemaining)?.let { (title, body) ->
                    NotificationHelper.notifyMilestone(applicationContext, event, title, body)
                }
                updated = updated.copy(lastNotifiedDaysRemaining = daysRemaining)
            }

            if (event.isRecurringYearly && event.targetDateTime < now) {
                val nextTarget = Instant.ofEpochMilli(event.targetDateTime)
                    .atZone(ZoneId.systemDefault())
                    .plusYears(1)
                    .toInstant()
                    .toEpochMilli()
                updated = updated.copy(targetDateTime = nextTarget, startDateTime = now, lastNotifiedDaysRemaining = null)
            }

            if (updated != event) repository.updateEvent(updated)
        }

        WidgetUpdater.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_maintenance"
    }
}
