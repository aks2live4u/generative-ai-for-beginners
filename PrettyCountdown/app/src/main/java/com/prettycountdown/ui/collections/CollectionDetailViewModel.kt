package com.prettycountdown.ui.collections

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CollectionDetailViewModel(repository: EventRepository, collectionId: Long) : ViewModel() {

    val collection: StateFlow<EventCollection?> = repository.collections
        .map { collections -> collections.find { it.id == collectionId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<Event>> = repository.observeEventsForCollection(collectionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(context: Context, collectionId: Long) = viewModelFactory {
            initializer { CollectionDetailViewModel(EventRepository.getInstance(context.applicationContext), collectionId) }
        }
    }
}
