package com.notnow.app.data.dao

import androidx.room.*
import com.notnow.app.data.entity.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): AppRule?

    @Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<AppRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<AppRule>)

    @Update
    suspend fun update(rule: AppRule)

    @Delete
    suspend fun delete(rule: AppRule)

    @Query("DELETE FROM app_rules")
    suspend fun deleteAll()
}
