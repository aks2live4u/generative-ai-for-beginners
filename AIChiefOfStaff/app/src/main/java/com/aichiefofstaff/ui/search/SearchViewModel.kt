package com.aichiefofstaff.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import com.aichiefofstaff.data.db.entity.MessageEntity
import com.aichiefofstaff.data.db.entity.ProjectEntity
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.ChatRepository
import com.aichiefofstaff.data.repository.InboxRepository
import com.aichiefofstaff.data.repository.ProjectRepository
import com.aichiefofstaff.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class SearchResults(
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val inboxItems: List<InboxItemEntity> = emptyList()
) {
    val isEmpty: Boolean
        get() = tasks.isEmpty() && projects.isEmpty() && messages.isEmpty() && inboxItems.isEmpty()
}

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val inboxRepository: InboxRepository
) : ViewModel() {

    val query = MutableStateFlow("")

    val results: StateFlow<SearchResults> = query
        .debounce(250)
        .flatMapLatest { q ->
            flow {
                if (q.isBlank()) {
                    emit(SearchResults())
                } else {
                    emit(
                        SearchResults(
                            tasks = taskRepository.search(q),
                            projects = projectRepository.search(q),
                            messages = chatRepository.searchMessages(q),
                            inboxItems = inboxRepository.search(q)
                        )
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun onQueryChange(text: String) {
        query.value = text
    }
}
