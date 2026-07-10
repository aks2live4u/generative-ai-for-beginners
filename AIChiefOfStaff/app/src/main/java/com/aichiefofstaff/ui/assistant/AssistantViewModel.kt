package com.aichiefofstaff.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.ConversationEntity
import com.aichiefofstaff.data.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createConversation(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createConversation(title.ifBlank { "New conversation" })
            onCreated(id)
        }
    }
}
