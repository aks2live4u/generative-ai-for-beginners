package com.aichiefofstaff.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.ConversationEntity
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.ChatRepository
import com.aichiefofstaff.data.repository.ProjectRepository
import com.aichiefofstaff.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val dueToday: List<TaskEntity> = emptyList(),
    val openTaskCount: Int = 0,
    val projectCount: Int = 0,
    val recentConversations: List<ConversationEntity> = emptyList()
)

class HomeViewModel(
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    chatRepository: ChatRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.observeDueToday(),
        taskRepository.observeOpenTasks(),
        projectRepository.observeActive(),
        chatRepository.observeConversations()
    ) { dueToday, open, projects, conversations ->
        HomeUiState(
            dueToday = dueToday,
            openTaskCount = open.size,
            projectCount = projects.size,
            recentConversations = conversations.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
