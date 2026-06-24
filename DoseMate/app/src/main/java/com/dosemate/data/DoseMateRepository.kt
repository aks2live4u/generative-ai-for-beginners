package com.dosemate.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class DoseMateRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val medicineDao = db.medicineDao()
    private val logDao = db.medicineLogDao()

    fun observeMedicines(): Flow<List<Medicine>> = medicineDao.observeAll()

    suspend fun getActiveMedicines(): List<Medicine> = medicineDao.getActiveMedicines()

    suspend fun getMedicine(id: Long): Medicine? = medicineDao.getById(id)

    suspend fun addMedicine(medicine: Medicine): Long = medicineDao.insert(medicine)

    suspend fun updateMedicine(medicine: Medicine) = medicineDao.update(medicine)

    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.delete(medicine)

    /** Deducts [amount] units of stock for a medicine, if it tracks quantity. Returns the updated medicine. */
    suspend fun decrementStock(medicineId: Long, amount: Int): Medicine? {
        val medicine = medicineDao.getById(medicineId) ?: return null
        val current = medicine.quantityAvailable ?: return medicine
        val updated = medicine.copy(quantityAvailable = (current - amount).coerceAtLeast(0))
        medicineDao.update(updated)
        return updated
    }

    fun observeLogsForDay(epochDay: Long): Flow<List<MedicineLog>> = logDao.observeLogsForDay(epochDay)

    fun observeLogsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<MedicineLog>> =
        logDao.observeLogsBetween(startEpochDay, endEpochDay)

    suspend fun getLogsBetween(startEpochDay: Long, endEpochDay: Long): List<MedicineLog> =
        logDao.getLogsBetween(startEpochDay, endEpochDay)

    fun observeLogsForMedicine(medicineId: Long): Flow<List<MedicineLog>> =
        logDao.observeLogsForMedicine(medicineId)

    fun observeAllLogs(): Flow<List<MedicineLog>> = logDao.observeAllLogs()

    suspend fun getLog(id: Long): MedicineLog? = logDao.getById(id)

    suspend fun insertLog(log: MedicineLog): Long = logDao.insert(log)

    suspend fun updateLog(log: MedicineLog) = logDao.update(log)
}
