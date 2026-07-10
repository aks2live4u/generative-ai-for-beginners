package com.aichiefofstaff.ui.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import com.aichiefofstaff.data.db.entity.TaskEntity
import com.aichiefofstaff.data.repository.ChatRepository
import com.aichiefofstaff.data.repository.InboxRepository
import com.aichiefofstaff.data.repository.TaskRepository
import com.aichiefofstaff.voice.IntentClassifier
import com.aichiefofstaff.voice.VoiceIntent
import com.aichiefofstaff.voice.VoiceRecognizerManager
import com.aichiefofstaff.voice.VoiceState
import kotlinx.coroutines.flow.StateFlow

sealed class VoiceOutcome {
    data class TaskCreated(val title: String) : VoiceOutcome()
    data class NoteCaptured(val content: String) : VoiceOutcome()
    data class RoutedToAssistant(val conversationId: Long) : VoiceOutcome()
    data class Failed(val message: String) : VoiceOutcome()
}

class VoiceCaptureViewModel(
    appContext: Context,
    private val taskRepository: TaskRepository,
    private val inboxRepository: InboxRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val recognizer = VoiceRecognizerManager(appContext)
    val voiceState: StateFlow<VoiceState> = recognizer.state

    fun isAvailable(): Boolean = recognizer.isAvailable()

    fun startListening() = recognizer.startListening()

    fun cancel() {
        recognizer.stopListening()
        recognizer.reset()
    }

    suspend fun handleRecognizedText(text: String): VoiceOutcome {
        if (text.isBlank()) return VoiceOutcome.Failed("Didn't catch that.")

        return when (val intent = IntentClassifier.classify(text)) {
            is VoiceIntent.CreateTask -> {
                taskRepository.save(TaskEntity(title = intent.title))
                VoiceOutcome.TaskCreated(intent.title)
            }
            is VoiceIntent.CreateNote -> {
                inboxRepository.capture(InboxItemEntity(content = intent.content))
                VoiceOutcome.NoteCaptured(intent.content)
            }
            is VoiceIntent.AskAssistant -> {
                val conversationId = chatRepository.createConversation(
                    title = intent.text.take(40)
                )
                val result = chatRepository.sendMessage(conversationId, intent.text)
                result.fold(
                    onSuccess = { VoiceOutcome.RoutedToAssistant(conversationId) },
                    onFailure = { VoiceOutcome.Failed(it.message ?: "AI request failed.") }
                )
            }
        }
    }

    override fun onCleared() {
        recognizer.stopListening()
        super.onCleared()
    }
}
