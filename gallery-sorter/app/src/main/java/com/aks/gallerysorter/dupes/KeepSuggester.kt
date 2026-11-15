package com.aks.gallerysorter.dupes

/**
 * Picks the item to keep: highest width x height, then own camera original, then earliest EXIF
 * date (null sorts last), then lowest id.
 */
fun suggestKeep(group: List<DupeCandidate>): DupeCandidate {
    require(group.isNotEmpty()) { "group must not be empty" }
    return group.minWith(
        compareByDescending<DupeCandidate> { it.width.toLong() * it.height.toLong() }
            .thenByDescending { it.isOwnCameraOriginal }
            .thenBy(nullsLast()) { it.exifDateMs }
            .thenBy { it.id }
    )
}
