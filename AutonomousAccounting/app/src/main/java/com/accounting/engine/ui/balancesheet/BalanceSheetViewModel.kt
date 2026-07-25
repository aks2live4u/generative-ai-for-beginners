package com.accounting.engine.ui.balancesheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.engine.domain.BalanceSheet
import com.accounting.engine.repository.AccountingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BalanceSheetViewModel(repository: AccountingRepository) : ViewModel() {
    val balanceSheet: StateFlow<BalanceSheet?> = repository.balanceSheet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
