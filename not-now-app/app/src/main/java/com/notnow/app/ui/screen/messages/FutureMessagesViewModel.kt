package com.notnow.app.ui.screen.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.FutureMessage
import com.notnow.app.data.repository.FutureMessageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FutureMessagesViewModel(private val repo: FutureMessageRepository) : ViewModel() {

    val messages: StateFlow<List<FutureMessage>> = repo.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMessage(text: String) = viewModelScope.launch {
        if (text.isNotBlank()) repo.add(text.trim())
    }

    fun delete(message: FutureMessage) = viewModelScope.launch {
        repo.delete(message)
    }

    class Factory(private val repo: FutureMessageRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FutureMessagesViewModel(repo) as T
    }
}
