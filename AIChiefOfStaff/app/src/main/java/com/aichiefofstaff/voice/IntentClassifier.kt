package com.aichiefofstaff.voice

sealed class VoiceIntent {
    data class CreateTask(val title: String) : VoiceIntent()
    data class CreateNote(val content: String) : VoiceIntent()
    data class AskAssistant(val text: String) : VoiceIntent()
}

private val TASK_TRIGGERS = listOf(
    "remind me to ", "remind me tomorrow to ", "add a task to ", "add task ",
    "create a task to ", "create task ", "i need to "
)

private val NOTE_TRIGGERS = listOf(
    "i have an idea", "note that ", "take a note", "jot down "
)

/**
 * Cheap on-device heuristic so common phrasing ("remind me to...") never
 * needs a network round-trip. Anything else is routed to the AI Assistant,
 * which is free to create tasks/notes itself from natural language.
 */
object IntentClassifier {

    fun classify(rawText: String): VoiceIntent {
        val text = rawText.trim()
        val lower = text.lowercase()

        TASK_TRIGGERS.firstOrNull { lower.startsWith(it) }?.let { trigger ->
            val title = text.substring(trigger.length).trim().removeSuffix(".")
            if (title.isNotBlank()) return VoiceIntent.CreateTask(title.replaceFirstChar { it.uppercase() })
        }

        NOTE_TRIGGERS.firstOrNull { lower.startsWith(it) }?.let { trigger ->
            val content = text.substring(trigger.length).trim().removeSuffix(".")
            return VoiceIntent.CreateNote(content.ifBlank { text })
        }

        return VoiceIntent.AskAssistant(text)
    }
}
