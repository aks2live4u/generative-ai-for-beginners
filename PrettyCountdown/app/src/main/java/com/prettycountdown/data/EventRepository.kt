package com.prettycountdown.data

import android.content.Context
import com.prettycountdown.data.model.BackupData
import com.prettycountdown.data.model.ChecklistItem
import com.prettycountdown.data.model.Event
import com.prettycountdown.data.model.EventCollection
import com.prettycountdown.data.model.EventCollectionCrossRef
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to local storage. The whole app is local-first: there is no
 * account, no login and no network sync - everything lives in the on-device Room
 * database created here.
 */
class EventRepository(private val db: AppDatabase) {

    val events: Flow<List<Event>> = db.eventDao().observeAll()
    val collections: Flow<List<EventCollection>> = db.collectionDao().observeAll()
    val eventCount: Flow<Int> = db.eventDao().observeCount()

    fun observeEvent(id: Long): Flow<Event?> = db.eventDao().observeById(id)

    suspend fun getEvent(id: Long): Event? = db.eventDao().getById(id)

    suspend fun getAllEventsOnce(): List<Event> = db.eventDao().getAll()

    /** Inserts a new event or updates an existing one, returning its id. */
    suspend fun saveEvent(event: Event): Long {
        return if (event.id == 0L) {
            db.eventDao().insert(event)
        } else {
            db.eventDao().update(event)
            event.id
        }
    }

    suspend fun updateEvent(event: Event) = db.eventDao().update(event)

    suspend fun deleteEvent(event: Event) = db.eventDao().delete(event)

    fun observeChecklist(eventId: Long): Flow<List<ChecklistItem>> =
        db.checklistDao().observeForEvent(eventId)

    suspend fun addChecklistItem(item: ChecklistItem) = db.checklistDao().insert(item)

    suspend fun updateChecklistItem(item: ChecklistItem) = db.checklistDao().update(item)

    suspend fun deleteChecklistItem(item: ChecklistItem) = db.checklistDao().delete(item)

    suspend fun createCollection(collection: EventCollection): Long =
        db.collectionDao().insert(collection)

    suspend fun deleteCollection(collection: EventCollection) =
        db.collectionDao().delete(collection)

    fun observeEventsForCollection(collectionId: Long): Flow<List<Event>> =
        db.collectionDao().observeEventsForCollection(collectionId)

    fun observeCollectionIdsForEvent(eventId: Long): Flow<List<Long>> =
        db.collectionDao().observeCollectionIdsForEvent(eventId)

    suspend fun setEventCollections(eventId: Long, collectionIds: Set<Long>) {
        val current = db.collectionDao().getCollectionIdsForEvent(eventId).toSet()
        (current - collectionIds).forEach { db.collectionDao().unlink(EventCollectionCrossRef(eventId, it)) }
        (collectionIds - current).forEach { db.collectionDao().link(EventCollectionCrossRef(eventId, it)) }
    }

    /** Snapshots everything for the Settings screen's "Export Backup" action. */
    suspend fun exportBackup(): BackupData = BackupData(
        events = db.eventDao().getAll(),
        checklistItems = db.checklistDao().getAllOnce(),
        collections = db.collectionDao().getAllOnce(),
        collectionMemberships = db.collectionDao().getAllCrossRefsOnce(),
    )

    /** Replaces all local data with the contents of [data]. Used by "Restore Backup". */
    suspend fun restoreBackup(data: BackupData) {
        db.eventDao().deleteAll()
        db.collectionDao().deleteAll()
        db.eventDao().insertAll(data.events)
        db.checklistDao().insertAll(data.checklistItems)
        db.collectionDao().insertAll(data.collections)
        db.collectionDao().insertAllCrossRefs(data.collectionMemberships)
    }

    companion object {
        @Volatile
        private var instance: EventRepository? = null

        fun getInstance(context: Context): EventRepository =
            instance ?: synchronized(this) {
                instance ?: EventRepository(AppDatabase.getInstance(context)).also { instance = it }
            }
    }
}
