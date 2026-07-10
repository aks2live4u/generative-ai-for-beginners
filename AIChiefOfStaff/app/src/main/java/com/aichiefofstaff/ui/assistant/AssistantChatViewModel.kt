package com.aichiefofstaff.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.MessageEntity
import com.aichiefofstaff.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantChatViewModel(
    private val conversationId: Long,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = chatRepository.observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun sendMessage(text: String) {
        if (text.isBlank() || _isSending.value) return
        _errorMessage.value = null
        _isSending.value = true
        viewModelScope.launch {
            val result = chatRepository.sendMessage(conversationId, text)
            result.onFailure { _errorMessage.value = it.message }
            _isSending.value = false
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
