package com.aks.gallerysorter.index.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.index.entities.MediaRow
import kotlinx.coroutines.flow.Flow

data class IdGeneration(val id: Long, val generation: Long)

data class CityCount(val city: String, val n: Int)

@Dao
interface MediaDao {
    @Upsert suspend fun upsert(row: MediaRow)
    @Upsert suspend fun upsertAll(rows: List<MediaRow>)

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun get(id: Long): MediaRow?

    @Query("SELECT * FROM media WHERE status = :status ORDER BY id LIMIT :limit")
    suspend fun byStatus(status: ScanStatus, limit: Int): List<MediaRow>

    @Query("UPDATE media SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: ScanStatus)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT id, generation FROM media")
    suspend fun generations(): List<IdGeneration>

    @Query("SELECT id FROM media")
    suspend fun allIds(): List<Long>

    @Query("SELECT COUNT(*) FROM media")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT city) FROM media WHERE city IS NOT NULL")
    fun cityCount(): Flow<Int>

    /** Camera-folder images (Settings → "My past phones" reads their EXIF models). */
    @Query("SELECT * FROM media WHERE kind = 'IMAGE' AND path LIKE '%/DCIM/Camera/%' ORDER BY dateTakenMs DESC")
    suspend fun cameraImages(): List<MediaRow>

    @Query("SELECT m.city AS city, COUNT(*) AS n FROM media m WHERE m.city IS NOT NULL AND $LOCK_FILTER GROUP BY m.city ORDER BY n DESC, m.city")
    fun citiesQuery(includeLocked: Int, hidden: List<String>): Flow<List<CityCount>>

    @Query("SELECT m.* FROM media m WHERE m.city = :city AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC")
    fun cityItemsQuery(city: String, includeLocked: Int, hidden: List<String>): PagingSource<Int, MediaRow>

    @Query("SELECT m.id FROM media m WHERE m.city = :city AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC")
    suspend fun cityIdsQuery(city: String, includeLocked: Int, hidden: List<String>): List<Long>

    @Query("SELECT m.* FROM media m WHERE m.id IN (:ids) AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC")
    fun itemsOfQuery(ids: List<Long>, includeLocked: Int, hidden: List<String>): PagingSource<Int, MediaRow>

    @Query("SELECT m.id FROM media m WHERE m.id IN (:ids) AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC")
    suspend fun idsOfQuery(ids: List<Long>, includeLocked: Int, hidden: List<String>): List<Long>
}

/** Cities by item count (most first); while locked, Sensitive/Documents/Review items are not counted. */
fun MediaDao.cities(includeLocked: Boolean) = citiesQuery(includeLocked.bit(), HIDDEN_NAMES)

fun MediaDao.cityItems(city: String, includeLocked: Boolean) = cityItemsQuery(city, includeLocked.bit(), HIDDEN_NAMES)

suspend fun MediaDao.cityIds(city: String, includeLocked: Boolean) = cityIdsQuery(city, includeLocked.bit(), HIDDEN_NAMES)

/** The visible ones of [ids] (e.g. a trip's items), newest first. */
fun MediaDao.itemsOf(ids: List<Long>, includeLocked: Boolean) = itemsOfQuery(ids, includeLocked.bit(), HIDDEN_NAMES)

suspend fun MediaDao.idsOf(ids: List<Long>, includeLocked: Boolean) = idsOfQuery(ids, includeLocked.bit(), HIDDEN_NAMES)
