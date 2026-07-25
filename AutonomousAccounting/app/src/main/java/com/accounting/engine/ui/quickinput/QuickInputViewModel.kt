package com.accounting.engine.ui.quickinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accounting.engine.domain.TransactionInput
import com.accounting.engine.repository.AccountingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SubmitFeedback {
    object Idle : SubmitFeedback()
    data class Posted(val description: String) : SubmitFeedback()
    object Unrecognized : SubmitFeedback()
    data class Error(val message: String) : SubmitFeedback()
}

data class QuickInputUiState(
    val text: String = "",
    val livePreview: TransactionInput? = null,
    val feedback: SubmitFeedback = SubmitFeedback.Idle
)

class QuickInputViewModel(private val repository: AccountingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickInputUiState())
    val uiState: StateFlow<QuickInputUiState> = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update {
            it.copy(text = text, livePreview = repository.preview(text), feedback = SubmitFeedback.Idle)
        }
    }

    fun submit() {
        val text = _uiState.value.text
        if (text.isBlank()) return

        viewModelScope.launch {
            when (val result = repository.submitTransaction(text)) {
                is AccountingRepository.SubmitResult.Success ->
                    _uiState.value = QuickInputUiState(feedback = SubmitFeedback.Posted(text))
                is AccountingRepository.SubmitResult.UnrecognizedInput ->
                    _uiState.update { it.copy(feedback = SubmitFeedback.Unrecognized) }
                is AccountingRepository.SubmitResult.Failure ->
                    _uiState.update { it.copy(feedback = SubmitFeedback.Error(result.message)) }
            }
        }
    }
}
