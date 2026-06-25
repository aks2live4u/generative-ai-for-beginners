package com.finsight.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finsight.core.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val name: String,
    val targetAmountMinor: Long,
    val savedAmountMinor: Long
)

fun GoalEntity.toDomain(): Goal = Goal(name, targetAmountMinor / 100.0, savedAmountMinor / 100.0)

fun Goal.toEntity(): GoalEntity = GoalEntity(name, Math.round(targetAmount * 100), Math.round(savedAmount * 100))
