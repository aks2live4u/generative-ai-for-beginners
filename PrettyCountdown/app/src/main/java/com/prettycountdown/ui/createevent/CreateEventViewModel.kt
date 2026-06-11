package com.prettycountdown.ui.createevent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prettycountdown.data.EventRepository
import com.prettycountdown.data.SettingsRepository
import com.prettycountdown.data.model.ColorPalettes
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCategory
import com.prettycountdown.data.model.WidgetStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class CreateEventUiState(
    val id: Long = 0L,
    val name: String = "",
    val category: EventCategory = EventCategory.CUSTOM,
    val date: LocalDate = LocalDate.now().plusDays(1),
    val time: LocalTime = LocalTime.of(9, 0),
    val hasTime: Boolean = false,
    val location: String = "",
    val photoUri: String? = null,
    val colorPaletteKey: String = ColorPalettes.default.key,
    val countdownFormat: CountdownFormat = CountdownFormat.default,
    val widgetStyle: WidgetStyle = WidgetStyle.default,
    val isRecurringYearly: Boolean = false,
    val notes: String = "",
    val startDateTime: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
) {
    val targetDateTime: Long
        get() {
            val localDateTime = if (hasTime) date.atTime(time) else date.atStartOfDay()
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
}

class CreateEventViewModel(
    private val repository: EventRepository,
    private val settings: SettingsRepository,
    private val eventId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEventUiState(isLoading = eventId != -1L, isEditing = eventId != -1L))
    val state: StateFlow<CreateEventUiState> = _state

    init {
        viewModelScope.launch {
            if (eventId != -1L) {
                repository.getEvent(eventId)?.let { event -> _state.update { fromEvent(event) } }
                _state.update { it.copy(isLoading = false) }
            } else {
                val defaultStyle = settings.defaultWidgetStyle.first()
                val defaultFormat = settings.defaultCountdownFormat.first()
                _state.update { it.copy(widgetStyle = defaultStyle, countdownFormat = defaultFormat) }
            }
        }
    }

    private fun fromEvent(event: Event): CreateEventUiState {
        val zoned = Instant.ofEpochMilli(event.targetDateTime).atZone(ZoneId.systemDefault())
        return CreateEventUiState(
            id = event.id,
            name = event.name,
            category = event.category,
            date = zoned.toLocalDate(),
            time = zoned.toLocalTime(),
            hasTime = event.hasTime,
            location = event.location.orEmpty(),
            photoUri = event.photoUri,
            colorPaletteKey = event.colorPaletteKey,
            countdownFormat = event.countdownFormat,
            widgetStyle = event.widgetStyle,
            isRecurringYearly = event.isRecurringYearly,
            notes = event.notes,
            startDateTime = event.startDateTime,
            isEditing = true,
        )
    }

    fun updateName(name: String) = _state.update { it.copy(name = name) }

    fun updateCategory(category: EventCategory) = _state.update {
        it.copy(category = category, colorPaletteKey = category.defaultPaletteKey)
    }

    fun updateDate(date: LocalDate) = _state.update { it.copy(date = date) }

    fun updateTime(time: LocalTime) = _state.update { it.copy(time = time) }

    fun updateHasTime(hasTime: Boolean) = _state.update { it.copy(hasTime = hasTime) }

    fun updateLocation(location: String) = _state.update { it.copy(location = location) }

    fun updatePhotoUri(uri: String?) = _state.update { it.copy(photoUri = uri, widgetStyle = WidgetStyle.PHOTO) }

    fun updateColorPalette(key: String) = _state.update { it.copy(colorPaletteKey = key) }

    fun updateCountdownFormat(format: CountdownFormat) = _state.update { it.copy(countdownFormat = format) }

    fun updateWidgetStyle(style: WidgetStyle) = _state.update { it.copy(widgetStyle = style) }

    fun updateRecurring(recurring: Boolean) = _state.update { it.copy(isRecurringYearly = recurring) }

    fun updateNotes(notes: String) = _state.update { it.copy(notes = notes) }

    /** Saves the event and returns its id. Suspends until the database write completes. */
    suspend fun save(): Long {
        val s = _state.value
        val event = Event(
            id = s.id,
            name = s.name.ifBlank { "Untitled Event" },
            category = s.category,
            targetDateTime = s.targetDateTime,
            hasTime = s.hasTime,
            location = s.location.ifBlank { null },
            photoUri = s.photoUri,
            colorPaletteKey = s.colorPaletteKey,
            countdownFormat = s.countdownFormat,
            widgetStyle = s.widgetStyle,
            isRecurringYearly = s.isRecurringYearly,
            startDateTime = if (s.isEditing) s.startDateTime else System.currentTimeMillis(),
            notes = s.notes,
        )
        val id = repository.saveEvent(event)
        if (!s.isEditing) settings.incrementEventsCreated()
        return id
    }

    companion object {
        fun factory(context: Context, eventId: Long) = viewModelFactory {
            initializer {
                CreateEventViewModel(
                    EventRepository.getInstance(context.applicationContext),
                    SettingsRepository.getInstance(context.applicationContext),
                    eventId
                )
            }
        }
    }
}
