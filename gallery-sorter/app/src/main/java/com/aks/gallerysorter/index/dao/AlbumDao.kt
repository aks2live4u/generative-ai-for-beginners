package com.aks.gallerysorter.index.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aks.gallerysorter.index.entities.AlbumItemRow
import com.aks.gallerysorter.index.entities.AlbumRow
import com.aks.gallerysorter.index.entities.MediaRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AlbumDao {
    @Insert abstract suspend fun insert(album: AlbumRow): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addItems(items: List<AlbumItemRow>)

    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE")
    abstract fun albums(): Flow<List<AlbumRow>>

    @Query("SELECT * FROM albums WHERE id = :id")
    abstract suspend fun get(id: Long): AlbumRow?

    /** An album's items, newest first; while locked, Sensitive/Documents/Review items are left out. */
    fun items(albumId: Long, includeLocked: Boolean): PagingSource<Int, MediaRow> =
        itemsQuery(albumId, if (includeLocked) 1 else 0, LabelDao.LOCKED_HIDDEN.map { it.name })

    @Query(
        "SELECT m.* FROM media m JOIN album_items a ON a.mediaId = m.id " +
            "WHERE a.albumId = :albumId AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC",
    )
    protected abstract fun itemsQuery(albumId: Long, includeLocked: Int, hidden: List<String>): PagingSource<Int, MediaRow>

    @Query(
        "SELECT m.id FROM media m JOIN album_items a ON a.mediaId = m.id " +
            "WHERE a.albumId = :albumId AND $LOCK_FILTER ORDER BY m.dateTakenMs DESC, m.id DESC",
    )
    protected abstract suspend fun idsQuery(albumId: Long, includeLocked: Int, hidden: List<String>): List<Long>

    suspend fun ids(albumId: Long, includeLocked: Boolean): List<Long> =
        idsQuery(albumId, if (includeLocked) 1 else 0, LabelDao.LOCKED_HIDDEN.map { it.name })
}
