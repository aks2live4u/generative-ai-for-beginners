package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aks.gallerysorter.index.entities.SettingRow

@Dao
interface SettingsDao {
    @Upsert suspend fun put(row: SettingRow)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?
}
