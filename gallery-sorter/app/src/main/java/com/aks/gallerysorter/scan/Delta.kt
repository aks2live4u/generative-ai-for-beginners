package com.aks.gallerysorter.scan

import com.aks.gallerysorter.core.MediaItem

data class Delta(
    val added: List<MediaItem>,
    val changed: List<MediaItem>,
    val removedIds: List<Long>,
)

/** Compares the indexed `known` (id -> generation) against the live MediaStore listing. */
fun computeDelta(known: Map<Long, Long>, current: List<MediaItem>): Delta {
    val added = ArrayList<MediaItem>()
    val changed = ArrayList<MediaItem>()
    val seen = HashSet<Long>(current.size)
    for (item in current) {
        seen += item.id
        val knownGen = known[item.id]
        when {
            knownGen == null -> added += item
            item.generation > knownGen -> changed += item
        }
    }
    val removed = known.keys.filter { it !in seen }
    return Delta(added, changed, removed)
}
