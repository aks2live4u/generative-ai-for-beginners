package com.notnow.app.data.repository

import com.notnow.app.data.dao.BlockedWebsiteDao
import com.notnow.app.data.entity.BlockedWebsite
import kotlinx.coroutines.flow.Flow

class BlockedWebsiteRepository(private val dao: BlockedWebsiteDao) {
    val allSites: Flow<List<BlockedWebsite>> = dao.observeAll()

    suspend fun add(site: BlockedWebsite) = dao.insert(site)

    suspend fun delete(site: BlockedWebsite) = dao.delete(site)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
