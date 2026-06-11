package com.prettycountdown.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single countdown event. This is the core unit of the app - everything else
 * (widgets, notifications, collections) is built around an [Event].
 */
@Serializable
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: EventCategory = EventCategory.CUSTOM,
    /** Epoch millis of the moment being counted down to. */
    val targetDateTime: Long,
    /** Whether [targetDateTime] carries a meaningful time-of-day component. */
    val hasTime: Boolean = false,
    val location: String? = null,
    /** Content URI of a user-picked photo, or null. */
    val photoUri: String? = null,
    val colorPaletteKey: String = ColorPalettes.default.key,
    val countdownFormat: CountdownFormat = CountdownFormat.default,
    val widgetStyle: WidgetStyle = WidgetStyle.default,
    val isRecurringYearly: Boolean = false,
    /** Epoch millis the countdown "started" - used for progress bars & percentage complete. */
    val startDateTime: Long = System.currentTimeMillis(),
    val notes: String = "",
    /** The last "days remaining" value a milestone notification was sent for, if any. */
    val lastNotifiedDaysRemaining: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
