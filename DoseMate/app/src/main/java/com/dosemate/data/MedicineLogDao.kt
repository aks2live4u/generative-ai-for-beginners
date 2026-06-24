package com.dosemate.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineLogDao {
    @Query("SELECT * FROM medicine_logs WHERE dateEpochDay = :epochDay ORDER BY scheduledEpochMillis")
    fun observeLogsForDay(epochDay: Long): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY scheduledEpochMillis")
    fun observeLogsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY scheduledEpochMillis")
    suspend fun getLogsBetween(startEpochDay: Long, endEpochDay: Long): List<MedicineLog>

    @Query("SELECT * FROM medicine_logs WHERE medicineId = :medicineId ORDER BY scheduledEpochMillis DESC")
    fun observeLogsForMedicine(medicineId: Long): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs ORDER BY scheduledEpochMillis DESC")
    fun observeAllLogs(): Flow<List<MedicineLog>>

    @Query("SELECT * FROM medicine_logs WHERE logId = :id")
    suspend fun getById(id: Long): MedicineLog?

    @Insert
    suspend fun insert(log: MedicineLog): Long

    @Update
    suspend fun update(log: MedicineLog)
}
