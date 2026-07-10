package com.aichiefofstaff.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.entity.ProjectEntity
import com.aichiefofstaff.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(private val projectRepository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            projectRepository.save(ProjectEntity(name = name.trim(), description = description.trim()))
        }
    }

    fun archive(project: ProjectEntity) {
        viewModelScope.launch { projectRepository.archive(project) }
    }
}
