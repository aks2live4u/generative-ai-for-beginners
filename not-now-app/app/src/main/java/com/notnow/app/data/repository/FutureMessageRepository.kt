package com.notnow.app.data.repository

import com.notnow.app.data.dao.FutureMessageDao
import com.notnow.app.data.entity.FutureMessage
import kotlinx.coroutines.flow.Flow

class FutureMessageRepository(private val dao: FutureMessageDao) {

    val allMessages: Flow<List<FutureMessage>> = dao.observeAll()

    suspend fun getRandomMessage(): FutureMessage? = dao.getRandom()

    suspend fun add(message: String) = dao.insert(FutureMessage(message = message))

    suspend fun delete(message: FutureMessage) = dao.delete(message)

    suspend fun recordShown(id: Long) = dao.incrementShowCount(id)
}
