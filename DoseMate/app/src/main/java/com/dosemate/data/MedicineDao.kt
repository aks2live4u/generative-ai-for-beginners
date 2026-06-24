package com.dosemate.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY reminderHour, reminderMinute")
    fun observeAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY reminderHour, reminderMinute")
    suspend fun getActiveMedicines(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE medicineId = :id")
    suspend fun getById(id: Long): Medicine?

    @Insert
    suspend fun insert(medicine: Medicine): Long

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)
}
