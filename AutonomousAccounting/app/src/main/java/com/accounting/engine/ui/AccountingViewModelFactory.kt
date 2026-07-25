package com.accounting.engine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.accounting.engine.repository.AccountingRepository
import com.accounting.engine.ui.pnl.ProfitAndLossViewModel
import com.accounting.engine.ui.balancesheet.BalanceSheetViewModel
import com.accounting.engine.ui.quickinput.QuickInputViewModel

class AccountingViewModelFactory(private val repository: AccountingRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            QuickInputViewModel::class.java -> QuickInputViewModel(repository) as T
            ProfitAndLossViewModel::class.java -> ProfitAndLossViewModel(repository) as T
            BalanceSheetViewModel::class.java -> BalanceSheetViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
