package com.dosemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dosemate.data.DoseMateRepository
import com.dosemate.data.MedicineLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class HistoryFilter { TODAY, WEEK, MONTH }

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DoseMateRepository(application)
    val filter = MutableStateFlow(HistoryFilter.WEEK)

    val logs = combine(repository.observeAllLogs(), filter) { logs, currentFilter ->
        val today = LocalDate.now().toEpochDay()
        val cutoff = when (currentFilter) {
            HistoryFilter.TODAY -> today
            HistoryFilter.WEEK -> today - 6
            HistoryFilter.MONTH -> today - 29
        }
        logs.filter { it.dateEpochDay in cutoff..today }
            .sortedByDescending { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<MedicineLog>())

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }
}
