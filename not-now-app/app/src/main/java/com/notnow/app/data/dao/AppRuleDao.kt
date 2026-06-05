package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    fun observeAllEnabled(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun findByPackage(pkg: String): AppRule?

    @Query("SELECT * FROM app_rules WHERE blockedInFocusMode = 1 AND isEnabled = 1")
    suspend fun getFocusModeBlocked(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE blockedAtNight = 1 AND isEnabled = 1")
    suspend fun getNightBlocked(): List<AppRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<AppRule>)

    @Delete
    suspend fun delete(rule: AppRule)

    @Query("UPDATE app_rules SET isEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)
}
