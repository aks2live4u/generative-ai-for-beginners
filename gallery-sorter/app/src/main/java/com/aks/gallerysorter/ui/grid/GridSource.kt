package com.aks.gallerysorter.ui.grid

import androidx.paging.PagingSource
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.dao.cityIds
import com.aks.gallerysorter.index.dao.cityItems
import com.aks.gallerysorter.index.dao.ids
import com.aks.gallerysorter.index.dao.idsOf
import com.aks.gallerysorter.index.dao.items
import com.aks.gallerysorter.index.dao.itemsOf
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.ui.requiresUnlock
import java.util.Base64

/**
 * What a grid (and its viewer) shows. Encoded in routes as `c:NAME` (category), `a:ID` (album),
 * `p:ID` (person), `t:START_MS` (trip) or `y:BASE64URL` (city). Every source applies the lock filter.
 * [pager] runs on IO and returns the paging-source factory (a trip first looks up its item ids).
 */
sealed interface GridSource {
    val requiresUnlock: Boolean get() = false
    suspend fun pager(graph: AppGraph, includeLocked: Boolean): () -> PagingSource<Int, MediaRow>
    suspend fun ids(graph: AppGraph, includeLocked: Boolean): List<Long>
    fun encode(): String

    data class OfCategory(val category: Category) : GridSource {
        override val requiresUnlock get() = category.requiresUnlock
        override suspend fun pager(graph: AppGraph, includeLocked: Boolean) = { graph.db.labelDao().itemsIn(category, includeLocked) }
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.labelDao().idsIn(category, includeLocked)
        override fun encode() = "c:${category.name}"
    }

    data class OfAlbum(val albumId: Long) : GridSource {
        override suspend fun pager(graph: AppGraph, includeLocked: Boolean) = { graph.db.albumDao().items(albumId, includeLocked) }
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.albumDao().ids(albumId, includeLocked)
        override fun encode() = "a:$albumId"
    }

    data class OfPerson(val personId: Long) : GridSource {
        override suspend fun pager(graph: AppGraph, includeLocked: Boolean) = { graph.db.peopleDao().items(personId, includeLocked) }
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.peopleDao().ids(personId, includeLocked)
        override fun encode() = "p:$personId"
    }

    data class OfCity(val city: String) : GridSource {
        override suspend fun pager(graph: AppGraph, includeLocked: Boolean) = { graph.db.mediaDao().cityItems(city, includeLocked) }
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.mediaDao().cityIds(city, includeLocked)
        override fun encode() = "y:" + Base64.getUrlEncoder().withoutPadding().encodeToString(city.toByteArray())
    }

    /** A trip from the last scan, identified by its start time (trips are recomputed after each scan). */
    data class OfTrip(val startMs: Long) : GridSource {
        private suspend fun tripIds(graph: AppGraph) = graph.scanSettings.trips().firstOrNull { it.startMs == startMs }?.itemIds.orEmpty()
        override suspend fun pager(graph: AppGraph, includeLocked: Boolean): () -> PagingSource<Int, MediaRow> {
            val ids = tripIds(graph)
            return { graph.db.mediaDao().itemsOf(ids, includeLocked) }
        }
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.mediaDao().idsOf(tripIds(graph), includeLocked)
        override fun encode() = "t:$startMs"
    }

    companion object {
        fun decode(s: String): GridSource {
            val v = s.substringAfter(':')
            return when (s.substringBefore(':')) {
                "c" -> OfCategory(Category.valueOf(v))
                "a" -> OfAlbum(v.toLong())
                "p" -> OfPerson(v.toLong())
                "t" -> OfTrip(v.toLong())
                "y" -> OfCity(String(Base64.getUrlDecoder().decode(v)))
                else -> throw IllegalArgumentException("bad grid source")
            }
        }
    }
}
