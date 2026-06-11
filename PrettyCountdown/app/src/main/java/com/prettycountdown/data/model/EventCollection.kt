package com.prettycountdown.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A user-defined group of events, e.g. "Wedding Planner" containing the
 * Engagement, Bachelor Party, Wedding and Honeymoon events.
 */
@Serializable
@Entity(tableName = "collections")
data class EventCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "⭐",
    val createdAt: Long = System.currentTimeMillis()
)

/** Many-to-many link between [Event] and [EventCollection]. */
@Serializable
@Entity(
    tableName = "event_collection_cross_ref",
    primaryKeys = ["eventId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("collectionId")]
)
data class EventCollectionCrossRef(
    val eventId: Long,
    val collectionId: Long
)
