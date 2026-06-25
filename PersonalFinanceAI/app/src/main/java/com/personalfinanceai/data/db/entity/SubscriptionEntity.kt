package com.personalfinanceai.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personalfinanceai.core.model.Subscription
import java.time.LocalDate

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val serviceName: String,
    val renewalDateEpochDay: Long,
    val monthlyCostMinor: Long,
    val lastUsedDateEpochDay: Long? = null
)

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    serviceName = serviceName,
    renewalDate = LocalDate.ofEpochDay(renewalDateEpochDay),
    monthlyCost = monthlyCostMinor / 100.0,
    lastUsedDate = lastUsedDateEpochDay?.let { LocalDate.ofEpochDay(it) }
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    serviceName = serviceName,
    renewalDateEpochDay = renewalDate.toEpochDay(),
    monthlyCostMinor = Math.round(monthlyCost * 100),
    lastUsedDateEpochDay = lastUsedDate?.toEpochDay()
)
