package com.prettycountdown.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.model.Event
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(repository: EventRepository) : ViewModel() {

    val events: StateFlow<List<Event>> = repository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { NotificationsViewModel(EventRepository.getInstance(context.applicationContext)) }
        }
    }
}
