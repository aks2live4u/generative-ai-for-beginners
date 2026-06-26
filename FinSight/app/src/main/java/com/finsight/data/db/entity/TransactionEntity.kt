package com.finsight.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finsight.core.model.Category
import com.finsight.core.model.PaymentMethod
import com.finsight.core.model.Transaction
import com.finsight.core.model.TransactionSource
import com.finsight.core.model.TransactionType
import java.time.Instant

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long, // stored as paise to avoid floating point drift
    val dateEpochMillis: Long,
    val merchant: String,
    val category: String,
    val type: String,
    val paymentMethod: String,
    val source: String,
    val rawText: String,
    val notes: String? = null,
    val confidence: Int = 99
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amountMinor / 100.0,
    date = Instant.ofEpochMilli(dateEpochMillis),
    merchant = merchant,
    category = Category.valueOf(category),
    type = TransactionType.valueOf(type),
    paymentMethod = PaymentMethod.valueOf(paymentMethod),
    source = TransactionSource.valueOf(source),
    rawText = rawText,
    notes = notes,
    confidence = confidence
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id ?: 0,
    amountMinor = Math.round(amount * 100),
    dateEpochMillis = date.toEpochMilli(),
    merchant = merchant,
    category = category.name,
    type = type.name,
    paymentMethod = paymentMethod.name,
    source = source.name,
    rawText = rawText,
    notes = notes,
    confidence = confidence
)
