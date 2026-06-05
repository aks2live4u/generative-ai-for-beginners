package com.notnow.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.UsageRecord
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.UsageRepository
import com.notnow.app.data.repository.WeeklyStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class DashboardUiState(
    val protectedTimeHours: Float = 0f,
    val mostAttemptedApp: String = "—",
    val emergencyUnlocks: Int = 0,
    val recentRecords: List<UsageRecord> = emptyList(),
    val peakHour: String = "—"
)

class WeeklyDashboardViewModel(
    private val usageRepo: UsageRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    val uiState: StateFlow<DashboardUiState> = combine(
        usageRepo.getRecordsSince(weekAgo),
        prefs.operatingMode // just to trigger recomposition
    ) { records, _ ->
        val peakHour = records
            .groupBy { java.util.Calendar.getInstance().apply { timeInMillis = it.attemptedAt }.get(java.util.Calendar.HOUR_OF_DAY) }
            .maxByOrNull { it.value.size }?.key
        val peakLabel = peakHour?.let {
            val suffix = if (it < 12) "AM" else "PM"
            val h = if (it == 0) 12 else if (it > 12) it - 12 else it
            "$h:00 $suffix"
        } ?: "—"

        DashboardUiState(
            recentRecords = records.take(50),
            peakHour = peakLabel
        )
    }.combine(prefs.operatingMode.take(1)) { state, _ ->
        state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    val protectedTimeHours: StateFlow<Float> = flow {
        val prefs2 = prefs
        prefs2.operatingMode.collect { // trigger
            // emit calculated value
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    class Factory(private val repo: UsageRepository, private val prefs: AppPreferences) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyDashboardViewModel(repo, prefs) as T
    }
}
