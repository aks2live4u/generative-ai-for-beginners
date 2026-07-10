package com.aichiefofstaff.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.InboxRepository
import com.aichiefofstaff.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val items: StateFlow<List<InboxItemEntity>> = inboxRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun captureText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { inboxRepository.capture(InboxItemEntity(content = text.trim())) }
    }

    fun convertToTask(item: InboxItemEntity) {
        viewModelScope.launch {
            taskRepository.save(TaskEntity(title = item.content))
            inboxRepository.markProcessed(item)
        }
    }

    fun delete(item: InboxItemEntity) {
        viewModelScope.launch { inboxRepository.delete(item) }
    }
}
