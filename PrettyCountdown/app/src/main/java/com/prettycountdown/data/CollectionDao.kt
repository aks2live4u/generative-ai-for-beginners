package com.prettycountdown.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCollection
import com.prettycountdown.data.model.EventCollectionCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<EventCollection>>

    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<EventCollection>

    @Query("SELECT * FROM event_collection_cross_ref")
    suspend fun getAllCrossRefsOnce(): List<EventCollectionCrossRef>

    @Insert
    suspend fun insert(collection: EventCollection): Long

    @Insert
    suspend fun insertAll(collections: List<EventCollection>)

    @Insert
    suspend fun insertAllCrossRefs(crossRefs: List<EventCollectionCrossRef>)

    @Delete
    suspend fun delete(collection: EventCollection)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()

    @Insert
    suspend fun link(crossRef: EventCollectionCrossRef)

    @Delete
    suspend fun unlink(crossRef: EventCollectionCrossRef)

    @Query(
        """
        SELECT events.* FROM events
        INNER JOIN event_collection_cross_ref ON events.id = event_collection_cross_ref.eventId
        WHERE event_collection_cross_ref.collectionId = :collectionId
        ORDER BY targetDateTime ASC
        """
    )
    fun observeEventsForCollection(collectionId: Long): Flow<List<Event>>

    @Query("SELECT collectionId FROM event_collection_cross_ref WHERE eventId = :eventId")
    suspend fun getCollectionIdsForEvent(eventId: Long): List<Long>

    @Query("SELECT collectionId FROM event_collection_cross_ref WHERE eventId = :eventId")
    fun observeCollectionIdsForEvent(eventId: Long): Flow<List<Long>>
}
