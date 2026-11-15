package com.aks.gallerysorter.dupes

import com.aks.gallerysorter.core.MediaKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeepSuggesterTest {
    private fun c(
        id: Long, w: Int = 1600, h: Int = 1200, camera: Boolean = false, exif: Long? = null,
    ) = DupeCandidate(id, MediaKind.IMAGE, 1000, null, null, null, emptyList(), w, h, camera, exif)

    @Test fun picksHighestResolution() {
        val keep = suggestKeep(listOf(c(1, 1600, 1200, camera = true), c(2, 4000, 3000)))
        assertThat(keep.id).isEqualTo(2)
    }

    @Test fun cameraOriginalWinsTies() {
        assertThat(suggestKeep(listOf(c(1), c(2, camera = true), c(3))).id).isEqualTo(2)
    }

    @Test fun earliestExifWinsNextAndNullSortsLast() {
        assertThat(suggestKeep(listOf(c(1, exif = null), c(2, exif = 500), c(3, exif = 100))).id)
            .isEqualTo(3)
    }

    @Test fun lowestIdIsFinalTieBreak() {
        assertThat(suggestKeep(listOf(c(7), c(3), c(5))).id).isEqualTo(3)
        assertThat(suggestKeep(listOf(c(7, exif = null), c(9, exif = null))).id).isEqualTo(7)
    }
}
