package com.prettycountdown.ui.collections

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.model.EventCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(private val repository: EventRepository) : ViewModel() {

    val collections: StateFlow<List<EventCollection>> = repository.collections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createCollection(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createCollection(EventCollection(name = name.trim(), emoji = emoji)) }
    }

    fun deleteCollection(collection: EventCollection) {
        viewModelScope.launch { repository.deleteCollection(collection) }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { CollectionsViewModel(EventRepository.getInstance(context.applicationContext)) }
        }
    }
}
