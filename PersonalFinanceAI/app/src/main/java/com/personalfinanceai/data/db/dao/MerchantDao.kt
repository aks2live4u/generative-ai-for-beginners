package com.personalfinanceai.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personalfinanceai.data.db.entity.MerchantEntity

@Dao
interface MerchantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(merchant: MerchantEntity)

    @Query("SELECT * FROM merchants ORDER BY transactionCount DESC")
    suspend fun getAll(): List<MerchantEntity>

    @Query("SELECT * FROM merchants WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MerchantEntity?
}
