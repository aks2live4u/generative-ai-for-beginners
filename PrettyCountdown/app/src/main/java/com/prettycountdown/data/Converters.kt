package com.prettycountdown.data

import androidx.room.TypeConverter
import com.prettycountdown.data.model.CountdownFormat
import com.prettycountdown.data.model.EventCategory
import com.prettycountdown.data.model.WidgetStyle

/** Stores enum-backed columns as their [Enum.name] string. */
class Converters {
    @TypeConverter
    fun fromCategory(value: EventCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): EventCategory =
        runCatching { EventCategory.valueOf(value) }.getOrDefault(EventCategory.default)

    @TypeConverter
    fun fromWidgetStyle(value: WidgetStyle): String = value.name

    @TypeConverter
    fun toWidgetStyle(value: String): WidgetStyle =
        runCatching { WidgetStyle.valueOf(value) }.getOrDefault(WidgetStyle.default)

    @TypeConverter
    fun fromCountdownFormat(value: CountdownFormat): String = value.name

    @TypeConverter
    fun toCountdownFormat(value: String): CountdownFormat =
        runCatching { CountdownFormat.valueOf(value) }.getOrDefault(CountdownFormat.default)
}
