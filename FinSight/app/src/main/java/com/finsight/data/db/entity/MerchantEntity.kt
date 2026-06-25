package com.finsight.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finsight.core.model.Category
import com.finsight.core.model.Merchant

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val name: String,
    val category: String,
    val transactionCount: Int = 0
)

fun MerchantEntity.toDomain(): Merchant = Merchant(name, Category.valueOf(category), transactionCount)

fun Merchant.toEntity(): MerchantEntity = MerchantEntity(name, category.name, transactionCount)
