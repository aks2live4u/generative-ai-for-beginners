package com.notnow.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.InteractionType
import com.notnow.app.data.entity.UsageRecord
import com.notnow.app.data.repository.ShoppingVaultRepository
import com.notnow.app.data.repository.UsageRecordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class DashboardState(
    val protectedHours: Double = 0.0,
    val itemsSaved: Int = 0,
    val peakTriggerTime: String = "—",
    val mostAttemptedApp: String = "—",
    val emergencyUnlocks: Int = 0,
    val records: List<UsageRecord> = emptyList()
)

class WeeklyDashboardViewModel(
    private val usageRepo: UsageRecordRepository,
    private val vaultRepo: ShoppingVaultRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            usageRepo.getWeeklyRecords().collect { records ->
                val protectedSeconds = records.filter { it.interactionType == InteractionType.BLOCKED }
                    .sumOf { it.delayApplied.toLong() }
                val protectedHours = protectedSeconds / 3600.0

                val emergencyUnlocks = records.count { it.interactionType == InteractionType.BYPASSED }

                val appCounts = records.groupBy { it.appName }.mapValues { it.value.size }
                val topApp = appCounts.maxByOrNull { it.value }?.key ?: "—"

                val hourCounts = records.groupBy {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    cal.get(java.util.Calendar.HOUR_OF_DAY)
                }.mapValues { it.value.size }
                val peakHour = hourCounts.maxByOrNull { it.value }?.key
                val peakTime = peakHour?.let { h ->
                    val ampm = if (h < 12) "AM" else "PM"
                    val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
                    "$displayHour:00 $ampm"
                } ?: "—"

                val itemsSaved = vaultRepo.countSavedThisWeek()

                _state.value = DashboardState(
                    protectedHours = protectedHours,
                    itemsSaved = itemsSaved,
                    peakTriggerTime = peakTime,
                    mostAttemptedApp = topApp,
                    emergencyUnlocks = emergencyUnlocks,
                    records = records.takeLast(50)
                )
            }
        }
    }
}

class WeeklyDashboardViewModelFactory(
    private val usageRepo: UsageRecordRepository,
    private val vaultRepo: ShoppingVaultRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return WeeklyDashboardViewModel(usageRepo, vaultRepo) as T
    }
}
