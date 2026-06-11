package com.notnow.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.UsageRecord
import com.notnow.app.data.preferences.AppPreferences
import com.notnow.app.data.repository.UsageRepository
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

data class DashboardUiState(
    val recentRecords: List<UsageRecord> = emptyList(),
    val peakHour: String = "—",
    val peakHourCount: Int = 0
)

class WeeklyDashboardViewModel(
    private val usageRepo: UsageRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    val uiState: StateFlow<DashboardUiState> = usageRepo.getRecordsSince(weekAgo)
        .map { records ->
            val peakEntry = records
                .groupBy {
                    java.util.Calendar.getInstance()
                        .apply { timeInMillis = it.attemptedAt }
                        .get(java.util.Calendar.HOUR_OF_DAY)
                }
                .maxByOrNull { it.value.size }
            val peakLabel = peakEntry?.key?.let {
                val suffix = if (it < 12) "AM" else "PM"
                val h = if (it == 0) 12 else if (it > 12) it - 12 else it
                "$h:00 $suffix"
            } ?: "—"
            DashboardUiState(
                recentRecords = records,
                peakHour = peakLabel,
                peakHourCount = peakEntry?.value?.size ?: 0
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
