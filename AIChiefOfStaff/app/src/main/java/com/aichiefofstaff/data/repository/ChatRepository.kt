package com.aichiefofstaff.data.repository

import com.aichiefofstaff.data.db.MessageRole
import com.aichiefofstaff.data.db.dao.ConversationDao
import com.aichiefofstaff.data.db.dao.MessageDao
import com.aichiefofstaff.data.db.entity.ConversationEntity
import com.aichiefofstaff.data.db.entity.MessageEntity
import com.aichiefofstaff.data.network.ChatCompletionRequest
import com.aichiefofstaff.data.network.ChatMessageDto
import com.aichiefofstaff.data.network.OpenAiApi
import com.aichiefofstaff.data.network.OpenAiErrorResponse
import com.aichiefofstaff.data.prefs.SecurePrefs
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

private const val SYSTEM_PROMPT =
    "You are a personal AI Chief of Staff. Be concise, direct, and practical. " +
        "Help the user think through priorities, decisions, tasks, and communications."
private const val MAX_HISTORY_MESSAGES = 20

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val openAiApi: OpenAiApi,
    private val securePrefs: SecurePrefs
) {

    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    suspend fun searchMessages(query: String): List<MessageEntity> = messageDao.search(query)

    suspend fun createConversation(title: String, projectId: Long? = null): Long =
        conversationDao.upsert(ConversationEntity(title = title, projectId = projectId))

    suspend fun sendMessage(conversationId: Long, userText: String): Result<String> {
        messageDao.insert(
            MessageEntity(conversationId = conversationId, role = MessageRole.USER, content = userText)
        )
        conversationDao.getById(conversationId)?.let {
            conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }

        val apiKey = securePrefs.openAiApiKey
        if (apiKey.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException("Add your OpenAI API key in Settings to use the AI Chief of Staff.")
            )
        }

        return try {
            val recentHistory = messageDao.getForConversation(conversationId).takeLast(MAX_HISTORY_MESSAGES)
            val apiMessages = listOf(ChatMessageDto("system", SYSTEM_PROMPT)) +
                recentHistory.map { ChatMessageDto(it.role.name.lowercase(), it.content) }

            val response = openAiApi.createChatCompletion(
                bearerToken = "Bearer $apiKey",
                request = ChatCompletionRequest(messages = apiMessages)
            )

            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                    ?: return Result.failure(IllegalStateException("The AI did not return a response."))
                messageDao.insert(
                    MessageEntity(conversationId = conversationId, role = MessageRole.ASSISTANT, content = reply)
                )
                Result.success(reply)
            } else {
                val errorBody = response.errorBody()?.string()
                val parsed = errorBody?.let { runCatching { Gson().fromJson(it, OpenAiErrorResponse::class.java) }.getOrNull() }
                val message = parsed?.error?.message ?: "OpenAI request failed (HTTP ${response.code()})."
                Result.failure(IllegalStateException(message))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
