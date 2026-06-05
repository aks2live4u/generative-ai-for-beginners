package com.notnow.app.ui.screen.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.FutureMessage
import com.notnow.app.data.repository.FutureMessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FutureMessagesViewModel(private val repo: FutureMessageRepository) : ViewModel() {

    val messages: StateFlow<List<FutureMessage>> = repo.getActiveMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(message: String) = viewModelScope.launch {
        if (message.isNotBlank()) repo.add(message)
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

class FutureMessagesViewModelFactory(private val repo: FutureMessageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return FutureMessagesViewModel(repo) as T
    }
}
