package com.aichiefofstaff.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.ProjectEntity
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.ProjectRepository
import com.aichiefofstaff.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectDetailViewModel(
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.observeTasksForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { _project.value = projectRepository.getProject(projectId) }
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { taskRepository.save(TaskEntity(title = title.trim(), projectId = projectId)) }
    }
}
