package com.nexus.launcher.system

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FocusState(
    val active: Boolean = false,
    val startedAt: Long = 0L,
    val endsAt: Long = 0L,
    val label: String = "Deep work",
    val allowedPackages: Set<String> = emptySet(),
    val silenceNotifications: Boolean = true,
    val todayMinutes: Int = 0,
    val todaySessions: Int = 0,
)

/**
 * Focus Mode: a countdown that optionally switches Do Not Disturb to priority-only
 * and limits the orbit to a few allowed apps.
 */
class FocusController(private val context: Context, private val scope: CoroutineScope) {
    private val sp = context.getSharedPreferences("nexus_focus", Context.MODE_PRIVATE)
    private val nm = context.getSystemService(NotificationManager::class.java)
    private var timer: Job? = null
    private var previousFilter: Int? = null

    private val _state = MutableStateFlow(
        FocusState(
            allowedPackages = sp.getStringSet("allowed", emptySet()).orEmpty(),
            silenceNotifications = sp.getBoolean("silence", true),
            todayMinutes = if (sp.getString("day", "") == today()) sp.getInt("minutes", 0) else 0,
            todaySessions = if (sp.getString("day", "") == today()) sp.getInt("sessions", 0) else 0,
        ),
    )
    val state: StateFlow<FocusState> = _state.asStateFlow()

    val hasDndAccess: Boolean get() = nm.isNotificationPolicyAccessGranted

    fun start(minutes: Int, label: String = "Deep work") {
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(active = true, startedAt = now, endsAt = now + minutes * 60_000L, label = label)
        if (_state.value.silenceNotifications) silence(true)
        timer?.cancel()
        timer = scope.launch {
            while (System.currentTimeMillis() < _state.value.endsAt) delay(1_000)
            stop(completed = true)
        }
    }

    fun stop(completed: Boolean = false) {
        val s = _state.value
        if (!s.active) return
        timer?.cancel()
        silence(false)
        val focusedMin = ((minOf(System.currentTimeMillis(), s.endsAt) - s.startedAt) / 60_000L).toInt()
        val day = today()
        val baseMin = if (sp.getString("day", "") == day) sp.getInt("minutes", 0) else 0
        val baseSessions = if (sp.getString("day", "") == day) sp.getInt("sessions", 0) else 0
        val minutes = baseMin + focusedMin
        val sessions = baseSessions + if (completed || focusedMin >= 5) 1 else 0
        sp.edit().putString("day", day).putInt("minutes", minutes).putInt("sessions", sessions).apply()
        _state.value = s.copy(active = false, todayMinutes = minutes, todaySessions = sessions)
    }

    fun toggleAllowed(pkg: String) {
        val cur = _state.value.allowedPackages
        val next = if (pkg in cur) cur - pkg else if (cur.size < 4) cur + pkg else cur
        sp.edit().putStringSet("allowed", next).apply()
        _state.value = _state.value.copy(allowedPackages = next)
    }

    fun setSilence(enabled: Boolean) {
        sp.edit().putBoolean("silence", enabled).apply()
        _state.value = _state.value.copy(silenceNotifications = enabled)
        if (_state.value.active) silence(enabled)
    }

    /** True when [pkg] may be opened right now. */
    fun allows(pkg: String): Boolean {
        val s = _state.value
        return !s.active || s.allowedPackages.isEmpty() || pkg in s.allowedPackages
    }

    private fun silence(on: Boolean) {
        if (!hasDndAccess) return
        runCatching {
            if (on) {
                if (previousFilter == null) previousFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                nm.setInterruptionFilter(previousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
                previousFilter = null
            }
        }
    }

    private fun today() = LocalDate.now().toString()
}
