package com.aichiefofstaff.data.repository

import com.aichiefofstaff.data.db.dao.InboxDao
import com.aichiefofstaff.data.db.entity.InboxItemEntity
import kotlinx.coroutines.flow.Flow

class InboxRepository(private val inboxDao: InboxDao) {

    fun observeAll(): Flow<List<InboxItemEntity>> = inboxDao.observeAll()

    fun observeUnprocessed(): Flow<List<InboxItemEntity>> = inboxDao.observeUnprocessed()

    suspend fun search(query: String): List<InboxItemEntity> = inboxDao.search(query)

    suspend fun capture(item: InboxItemEntity): Long = inboxDao.upsert(item)

    suspend fun markProcessed(item: InboxItemEntity) = inboxDao.update(item.copy(processed = true))

    suspend fun delete(item: InboxItemEntity) = inboxDao.delete(item)
}
