package com.aks.gallerysorter.index.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.PersonRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {
    @Insert suspend fun insertPerson(person: PersonRow): Long
    @Update suspend fun updatePerson(person: PersonRow)
    @Insert suspend fun insertFaces(faces: List<FaceRow>)

    @Query("SELECT * FROM people ORDER BY id")
    fun people(): Flow<List<PersonRow>>

    @Query("SELECT * FROM faces WHERE mediaId = :mediaId")
    suspend fun facesFor(mediaId: Long): List<FaceRow>

    @Query("SELECT * FROM faces")
    suspend fun allFaces(): List<FaceRow>

    @Query("UPDATE faces SET personId = :personId WHERE id = :faceId")
    suspend fun assign(faceId: Long, personId: Long?)

    @Query("DELETE FROM faces WHERE mediaId = :mediaId")
    suspend fun clearFaces(mediaId: Long)
}
