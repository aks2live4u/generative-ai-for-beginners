package com.accounting.engine.ui.pnl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.engine.domain.ProfitAndLossStatement
import com.accounting.engine.repository.AccountingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfitAndLossViewModel(repository: AccountingRepository) : ViewModel() {
    val profitAndLoss: StateFlow<ProfitAndLossStatement?> = repository.profitAndLoss
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
