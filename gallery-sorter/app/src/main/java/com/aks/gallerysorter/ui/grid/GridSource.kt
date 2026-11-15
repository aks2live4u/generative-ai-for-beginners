package com.aks.gallerysorter.ui.grid

import androidx.paging.PagingSource
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.ui.requiresUnlock

/** What a grid (and its viewer) shows: a category or a user album. Encoded in routes as `c:NAME` / `a:ID`. */
sealed interface GridSource {
    val requiresUnlock: Boolean
    fun paging(graph: AppGraph, includeLocked: Boolean): PagingSource<Int, MediaRow>
    suspend fun ids(graph: AppGraph, includeLocked: Boolean): List<Long>
    fun encode(): String

    data class OfCategory(val category: Category) : GridSource {
        override val requiresUnlock get() = category.requiresUnlock
        override fun paging(graph: AppGraph, includeLocked: Boolean) = graph.db.labelDao().itemsIn(category, includeLocked)
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.labelDao().idsIn(category, includeLocked)
        override fun encode() = "c:${category.name}"
    }

    data class OfAlbum(val albumId: Long) : GridSource {
        override val requiresUnlock get() = false
        override fun paging(graph: AppGraph, includeLocked: Boolean) = graph.db.albumDao().items(albumId, includeLocked)
        override suspend fun ids(graph: AppGraph, includeLocked: Boolean) = graph.db.albumDao().ids(albumId, includeLocked)
        override fun encode() = "a:$albumId"
    }

    companion object {
        fun decode(s: String): GridSource = when {
            s.startsWith("c:") -> OfCategory(Category.valueOf(s.removePrefix("c:")))
            s.startsWith("a:") -> OfAlbum(s.removePrefix("a:").toLong())
            else -> throw IllegalArgumentException("bad grid source")
        }
    }
}
