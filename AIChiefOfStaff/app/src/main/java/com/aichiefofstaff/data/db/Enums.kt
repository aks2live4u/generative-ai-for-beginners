package com.aichiefofstaff.data.db

enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

enum class InboxItemType { TEXT, VOICE_NOTE, IMAGE, DOCUMENT, SCREENSHOT, LINK }

enum class MessageRole { USER, ASSISTANT, SYSTEM }
