package com.accounting.engine.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index("parentAccountId"), Index(value = ["name"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val parentAccountId: String? = null,
    val isSystemAccount: Boolean = false,
    val createdAt: Long
)
