package com.aks.gallerysorter.index.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.PersonRow
import kotlinx.coroutines.flow.Flow

/** A person with the number of visible items they appear in and one of those items as the cover. */
data class PersonSummary(val id: Long, val name: String?, val isMe: Boolean, val n: Int, val coverId: Long)

@Dao
interface PeopleDao {
    @Insert suspend fun insertPerson(person: PersonRow): Long
    @Update suspend fun updatePerson(person: PersonRow)
    @Insert suspend fun insertFaces(faces: List<FaceRow>)

    @Query("SELECT * FROM people ORDER BY id")
    fun people(): Flow<List<PersonRow>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun person(id: Long): PersonRow?

    @Query("SELECT * FROM faces WHERE mediaId = :mediaId")
    suspend fun facesFor(mediaId: Long): List<FaceRow>

    @Query("SELECT * FROM faces")
    suspend fun allFaces(): List<FaceRow>

    @Query("UPDATE faces SET personId = :personId WHERE id = :faceId")
    suspend fun assign(faceId: Long, personId: Long?)

    @Query("DELETE FROM faces WHERE mediaId = :mediaId")
    suspend fun clearFaces(mediaId: Long)

    @Query("SELECT DISTINCT mediaId FROM faces WHERE personId IN (:personIds)")
    suspend fun mediaIdsOf(personIds: List<Long>): List<Long>

    @Query("UPDATE people SET isMe = 0 WHERE isMe = 1")
    suspend fun clearMe()

    @Query("UPDATE people SET isMe = 1 WHERE id = :id")
    suspend fun setMeFlag(id: Long)

    @Query("UPDATE faces SET personId = :into WHERE personId = :from")
    suspend fun moveFaces(from: Long, into: Long)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePerson(id: Long)

    /** Only one person is "Me". */
    @Transaction
    suspend fun setMe(id: Long) {
        clearMe()
        setMeFlag(id)
    }

    /** Moves every face of [from] onto [into] and deletes [from]; [into] becomes "Me" if [from] was. */
    @Transaction
    suspend fun merge(from: Long, into: Long) {
        if (from == into) return
        val wasMe = person(from)?.isMe == true
        moveFaces(from, into)
        deletePerson(from)
        if (wasMe) setMe(into)
    }

    @Query(
        "SELECT p.id, p.name, p.isMe, COUNT(DISTINCT f.mediaId) AS n, MAX(f.mediaId) AS coverId " +
            "FROM people p JOIN faces f ON f.personId = p.id JOIN media m ON m.id = f.mediaId " +
            "WHERE $LOCK_FILTER GROUP BY p.id ORDER BY p.isMe DESC, n DESC, p.id",
    )
    fun peopleQuery(includeLocked: Int, hidden: List<String>): Flow<List<PersonSummary>>

    @Query(
        "SELECT m.* FROM media m WHERE EXISTS (SELECT 1 FROM faces f WHERE f.mediaId = m.id AND f.personId = :personId) " +
            "AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC",
    )
    fun itemsQuery(personId: Long, includeLocked: Int, hidden: List<String>): PagingSource<Int, MediaRow>

    @Query(
        "SELECT m.id FROM media m WHERE EXISTS (SELECT 1 FROM faces f WHERE f.mediaId = m.id AND f.personId = :personId) " +
            "AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC",
    )
    suspend fun idsQuery(personId: Long, includeLocked: Int, hidden: List<String>): List<Long>
}

/** People with at least one visible item; while locked, items of Sensitive/Documents/Review are not counted or used as covers. */
fun PeopleDao.visiblePeople(includeLocked: Boolean) = peopleQuery(includeLocked.bit(), HIDDEN_NAMES)

fun PeopleDao.items(personId: Long, includeLocked: Boolean) = itemsQuery(personId, includeLocked.bit(), HIDDEN_NAMES)

suspend fun PeopleDao.ids(personId: Long, includeLocked: Boolean) = idsQuery(personId, includeLocked.bit(), HIDDEN_NAMES)
