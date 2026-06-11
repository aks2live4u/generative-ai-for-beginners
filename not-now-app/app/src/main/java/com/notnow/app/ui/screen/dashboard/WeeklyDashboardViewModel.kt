package com.notnow.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.UsageRecord
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.UsageRepository
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

data class HourCount(val label: String, val count: Int)

data class DashboardUiState(
    val recentRecords: List<UsageRecord> = emptyList(),
    val peakHour: String = "—",
    val peakHourCount: Int = 0,
    val hourlyBreakdown: List<HourCount> = emptyList()
)

class WeeklyDashboardViewModel(
    private val usageRepo: UsageRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    val uiState: StateFlow<DashboardUiState> = usageRepo.getRecordsSince(weekAgo)
        .map { records ->
            fun hourLabel(hour: Int): String {
                val suffix = if (hour < 12) "AM" else "PM"
                val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                return "$h:00 $suffix"
            }

            val countsByHour = records.groupingBy {
                java.util.Calendar.getInstance()
                    .apply { timeInMillis = it.attemptedAt }
                    .get(java.util.Calendar.HOUR_OF_DAY)
            }.eachCount()

            val peakEntry = countsByHour.maxByOrNull { it.value }
            val hourlyBreakdown = countsByHour.entries
                .sortedByDescending { it.value }
                .map { HourCount(hourLabel(it.key), it.value) }

            DashboardUiState(
                recentRecords = records,
                peakHour = peakEntry?.key?.let { hourLabel(it) } ?: "—",
                peakHourCount = peakEntry?.value ?: 0,
                hourlyBreakdown = hourlyBreakdown
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    class Factory(private val repo: UsageRepository, private val prefs: AppPreferences) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeeklyDashboardViewModel(repo, prefs) as T
    }
}
