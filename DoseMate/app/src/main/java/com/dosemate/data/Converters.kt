package com.dosemate.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun frequencyToString(value: Frequency): String = value.name

    @TypeConverter
    fun stringToFrequency(value: String): Frequency = Frequency.valueOf(value)

    @TypeConverter
    fun statusToString(value: LogStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): LogStatus = LogStatus.valueOf(value)
}
