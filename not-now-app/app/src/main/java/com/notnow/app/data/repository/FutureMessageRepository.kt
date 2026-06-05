package com.notnow.app.data.repository

import com.notnow.app.data.dao.FutureMessageDao
import com.notnow.app.data.entity.FutureMessage
import kotlinx.coroutines.flow.Flow

class FutureMessageRepository(private val dao: FutureMessageDao) {

    fun getActiveMessages(): Flow<List<FutureMessage>> = dao.getActiveMessages()

    suspend fun getRandomMessage(): FutureMessage? = dao.getRandomMessage()

    suspend fun add(message: String): Long = dao.insert(FutureMessage(message = message.trim()))

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun seedDefaults() {
        val defaults = listOf(
            "Finish the chapter first.",
            "You wanted investments, not another package.",
            "This urge will pass.",
            "Come back tomorrow and decide again.",
            "Sleep on it. Always sleep on it.",
            "Your future self is watching this decision."
        )
        defaults.forEach { dao.insert(FutureMessage(message = it)) }
    }
}
