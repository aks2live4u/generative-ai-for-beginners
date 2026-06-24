package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.analytics.AdherenceCalculator
import com.dosemate.analytics.AdherenceStats
import com.dosemate.analytics.InsightsEngine
import com.dosemate.analytics.MedicineStats
import com.dosemate.analytics.TrendPoint
import com.dosemate.data.DoseMateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsUiState(
    val stats: AdherenceStats = AdherenceStats(100, 0, 0, 0, 0, 0),
    val timingTrend: List<TrendPoint> = emptyList(),
    val adherenceTrend: List<TrendPoint> = emptyList(),
    val insights: List<String> = emptyList(),
    val perMedicine: List<MedicineStats> = emptyList()
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)

    val uiState = repository.observeAllLogs().map { logs ->
        val monthCutoff = LocalDate.now().toEpochDay() - 29
        val monthLogs = logs.filter { it.dateEpochDay >= monthCutoff }
        AnalyticsUiState(
            stats = AdherenceCalculator.computeStats(monthLogs),
            timingTrend = AdherenceCalculator.timingTrend(monthLogs),
            adherenceTrend = AdherenceCalculator.weeklyAdherenceTrend(logs),
            insights = InsightsEngine.generate(monthLogs),
            perMedicine = monthLogs.groupBy { it.medicineName }
                .map { (name, medicineLogs) -> AdherenceCalculator.medicineStats(name, medicineLogs) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
