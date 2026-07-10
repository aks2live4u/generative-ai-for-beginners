package com.aichiefofstaff.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichiefofstaff.data.db.TaskStatus
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(task: TaskEntity) {
        viewModelScope.launch { taskRepository.save(task) }
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch {
            if (task.status == TaskStatus.DONE) taskRepository.markIncomplete(task)
            else taskRepository.markComplete(task)
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch { taskRepository.delete(task) }
    }
}
