package com.aichiefofstaff.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPriority(value: TaskPriority): Int = value.ordinal

    @TypeConverter
    fun toPriority(value: Int): TaskPriority = TaskPriority.entries.getOrElse(value) { TaskPriority.MEDIUM }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromInboxItemType(value: InboxItemType): String = value.name

    @TypeConverter
    fun toInboxItemType(value: String): InboxItemType = InboxItemType.valueOf(value)

    @TypeConverter
    fun fromMessageRole(value: MessageRole): String = value.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole = MessageRole.valueOf(value)
}
