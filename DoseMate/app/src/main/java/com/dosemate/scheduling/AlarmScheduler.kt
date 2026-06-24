package com.dosemate.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.data.timesOfDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val MISSED_REQUEST_CODE_OFFSET = 1_000_000
private const val SLOTS_PER_MEDICINE = 16

object AlarmScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    /** Computes the next trigger date/time (epoch millis) for a specific dose-time slot of a medicine, or null if its schedule has ended. */
    fun nextOccurrenceMillisForSlot(medicine: Medicine, slotHour: Int, slotMinute: Int, after: LocalDateTime = LocalDateTime.now()): Long? {
        val zone = ZoneId.systemDefault()
        val endDate = medicine.endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
        val startDate = LocalDate.ofEpochDay(medicine.startDateEpochDay)

        fun candidateAt(date: LocalDate): LocalDateTime = date.atTime(slotHour, slotMinute)

        val next: LocalDateTime? = when (medicine.frequency) {
            Frequency.DAILY -> {
                var date = maxOf(startDate, after.toLocalDate())
                var candidate = candidateAt(date)
                if (!candidate.isAfter(after)) {
                    date = date.plusDays(1)
                    candidate = candidateAt(date)
                }
                candidate
            }

            Frequency.SPECIFIC_DAYS -> {
                val days = medicine.specificDaysCsv.split(",")
                    .filter { it.isNotBlank() }
                    .map { DayOfWeek.of(it.trim().toInt()) }
                    .toSet()
                if (days.isEmpty()) return null
                var date = maxOf(startDate, after.toLocalDate())
                var candidate = candidateAt(date)
                var guard = 0
                while ((!days.contains(date.dayOfWeek)) || !candidate.isAfter(after)) {
                    date = date.plusDays(1)
                    candidate = candidateAt(date)
                    guard++
                    if (guard > 14) return null
                }
                candidate
            }

            Frequency.ALTERNATE_DAYS -> {
                var date = maxOf(startDate, after.toLocalDate())
                fun isOnCycle(d: LocalDate) = (java.time.temporal.ChronoUnit.DAYS.between(startDate, d)) % 2 == 0L
                var candidate = candidateAt(date)
                while (!isOnCycle(date) || !candidate.isAfter(after)) {
                    date = date.plusDays(1)
                    candidate = candidateAt(date)
                }
                candidate
            }

            Frequency.EVERY_X_HOURS -> {
                val intervalHours = medicine.everyXHours.coerceAtLeast(1)
                var candidate = startDate.atTime(slotHour, slotMinute)
                while (!candidate.isAfter(after)) {
                    candidate = candidate.plusHours(intervalHours.toLong())
                }
                candidate
            }

            // As-needed: never auto-schedules a reminder, the user logs doses manually.
            Frequency.SOS -> null
        }

        if (next == null) return null
        if (endDate != null && next.toLocalDate().isAfter(endDate)) return null
        return next.atZone(zone).toInstant().toEpochMilli()
    }

    /** Back-compat single-slot lookup, used by analytics/preview code that only cares about the first dose time. */
    fun nextOccurrenceMillis(medicine: Medicine, after: LocalDateTime = LocalDateTime.now()): Long? {
        val (h, m) = medicine.timesOfDay().first()
        return nextOccurrenceMillisForSlot(medicine, h, m, after)
    }

    private fun slotRequestCode(medicineId: Long, slot: Int) = (medicineId.toInt() * SLOTS_PER_MEDICINE) + slot

    fun scheduleNext(context: Context, medicine: Medicine) {
        if (!medicine.isActive) return
        if (medicine.frequency == Frequency.SOS) return
        medicine.timesOfDay().forEachIndexed { index, (h, m) ->
            scheduleSlot(context, medicine, index, h, m)
        }
    }

    fun scheduleNextForSlot(context: Context, medicine: Medicine, slotIndex: Int) {
        if (!medicine.isActive) return
        val times = medicine.timesOfDay()
        val slot = times.getOrNull(slotIndex) ?: return
        scheduleSlot(context, medicine, slotIndex, slot.first, slot.second)
    }

    private fun scheduleSlot(context: Context, medicine: Medicine, slotIndex: Int, hour: Int, minute: Int) {
        val triggerAt = nextOccurrenceMillisForSlot(medicine, hour, minute) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicine.medicineId)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotRequestCode(medicine.medicineId, slotIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        for (slotIndex in 0 until SLOTS_PER_MEDICINE) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                slotRequestCode(medicine.medicineId, slotIndex),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleMissedCheck(context: Context, logId: Long, medicineId: Long, scheduledMillis: Long, missedAfterMinutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, MissedDoseReceiver::class.java).apply {
            putExtra(EXTRA_LOG_ID, logId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
        }
        val requestCode = (MISSED_REQUEST_CODE_OFFSET + logId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = scheduledMillis + missedAfterMinutes * 60_000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_LOG_ID = "extra_log_id"
    const val EXTRA_SLOT_INDEX = "extra_slot_index"
}
