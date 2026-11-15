package com.aks.gallerysorter.scan

import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeltaTest {
    private fun item(id: Long, gen: Long) = MediaItem(
        id = id, path = "/p/$id.jpg", kind = MediaKind.IMAGE, sizeBytes = 1, mime = "image/jpeg",
        width = 1, height = 1, dateTakenMs = null, durationMs = null, generation = gen,
    )

    @Test
    fun newItem_isAdded() {
        val d = computeDelta(mapOf(1L to 5L), listOf(item(1, 5), item(2, 7)))
        assertThat(d.added.map { it.id }).containsExactly(2L)
        assertThat(d.changed).isEmpty()
        assertThat(d.removedIds).isEmpty()
    }

    @Test
    fun higherGeneration_isChanged() {
        val d = computeDelta(mapOf(1L to 5L), listOf(item(1, 6)))
        assertThat(d.changed.map { it.id }).containsExactly(1L)
        assertThat(d.added).isEmpty()
        assertThat(d.removedIds).isEmpty()
    }

    @Test
    fun missingFromCurrent_isRemoved() {
        val d = computeDelta(mapOf(1L to 5L, 2L to 5L), listOf(item(1, 5)))
        assertThat(d.removedIds).containsExactly(2L)
        assertThat(d.added).isEmpty()
        assertThat(d.changed).isEmpty()
    }

    @Test
    fun unchanged_isIgnored() {
        val d = computeDelta(mapOf(1L to 5L, 2L to 9L), listOf(item(1, 5), item(2, 9)))
        assertThat(d.added).isEmpty()
        assertThat(d.changed).isEmpty()
        assertThat(d.removedIds).isEmpty()
    }

    @Test
    fun lowerOrEqualGeneration_isNotChanged_andEmptyKnownAddsAll() {
        assertThat(computeDelta(mapOf(1L to 5L), listOf(item(1, 3))).changed).isEmpty()
        assertThat(computeDelta(emptyMap(), listOf(item(1, 1), item(2, 1))).added).hasSize(2)
    }
}
