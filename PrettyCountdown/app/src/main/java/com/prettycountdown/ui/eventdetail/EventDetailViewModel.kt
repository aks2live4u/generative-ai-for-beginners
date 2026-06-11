package com.prettycountdown.ui.eventdetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.model.ChecklistItem
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCollection
import com.prettycountdown.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val context: Context,
    private val repository: EventRepository,
    private val eventId: Long,
) : ViewModel() {

    val event: StateFlow<Event?> = repository.observeEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val checklist: StateFlow<List<ChecklistItem>> = repository.observeChecklist(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collections: StateFlow<List<EventCollection>> = repository.collections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memberCollectionIds: StateFlow<Set<Long>> = repository.observeCollectionIdsForEvent(eventId)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val checklistProgress: StateFlow<Float> = checklist.map { items ->
        if (items.isEmpty()) 0f else items.count { it.isDone }.toFloat() / items.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addChecklistItem(ChecklistItem(eventId = eventId, text = text.trim(), position = checklist.value.size))
        }
    }

    fun toggleChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { repository.updateChecklistItem(item.copy(isDone = !item.isDone)) }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { repository.deleteChecklistItem(item) }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch { event.value?.let { repository.updateEvent(it.copy(notes = notes)) } }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            event.value?.let { repository.deleteEvent(it) }
            WidgetUpdater.refreshAll(context)
        }
    }

    fun toggleCollection(collectionId: Long) {
        viewModelScope.launch {
            val updated = memberCollectionIds.value.toMutableSet().apply {
                if (!add(collectionId)) remove(collectionId)
            }
            repository.setEventCollections(eventId, updated)
        }
    }

    companion object {
        fun factory(context: Context, eventId: Long) = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                EventDetailViewModel(appContext, EventRepository.getInstance(appContext), eventId)
            }
        }
    }
}
