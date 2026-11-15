package com.aks.gallerysorter.classify

import com.aks.gallerysorter.classify.ml.NsfwScore
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.index.entities.OverrideRow
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategorizerTest {
    private fun signals(
        explicit: Float? = null,
        sources: Set<Category> = emptySet(),
        ownCamera: Boolean = false,
        doc: CategoryAssignment? = null,
        labels: Map<String, Float> = emptyMap(),
        hasQr: Boolean = false,
        blur: Double? = null,
        faces: List<Long> = emptyList(),
        front: Boolean? = null,
        largeFace: Boolean = false,
    ) = Signals(explicit?.let { NsfwScore(it, 0f) }, sources, ownCamera, doc, labels, hasQr, blur, faces, front, largeFace)

    private fun run(s: Signals, me: Long? = 1L, o: List<OverrideRow> = emptyList()) =
        categorize(s, me, o).associate { it.category to it.confidence }

    @Test fun nsfwBelowReviewBandIsNothing() {
        assertThat(run(signals(explicit = 0.4f, ownCamera = true))).isEmpty()
    }

    @Test fun ownCameraExplicit() {
        assertThat(run(signals(explicit = 0.95f, ownCamera = true)))
            .containsExactly(Category.MY_NUDES, 0.95f, Category.MY_NUDES_CAMERA, 0.95f)
    }

    @Test fun whatsappExplicitWithoutMeIsOtherAdult() {
        assertThat(run(signals(explicit = 0.95f, sources = setOf(Category.WHATSAPP), faces = listOf(2L))))
            .containsExactly(Category.OTHER_ADULT, 0.95f, Category.WHATSAPP, 1.0f)
    }

    @Test fun reviewBandAddsReviewAndTentativeCategory() {
        val r = run(signals(explicit = 0.6f))
        assertThat(r).containsExactly(Category.OTHER_ADULT, 0.6f, Category.REVIEW, 0.6f)
    }

    @Test fun docReviewBand() {
        val r = run(signals(doc = CategoryAssignment(Category.BANKING, 0.6f)))
        assertThat(r).containsExactly(Category.BANKING, 0.6f, Category.REVIEW, 0.6f)
        assertThat(run(signals(doc = CategoryAssignment(Category.IDS, 0.9f))))
            .containsExactly(Category.IDS, 0.9f)
    }

    @Test fun reviewUsesMaxTriggeringConfidence() {
        val r = run(signals(explicit = 0.55f, doc = CategoryAssignment(Category.BANKING, 0.7f)))
        assertThat(r[Category.REVIEW]).isEqualTo(0.7f)
    }

    @Test fun excludeOverrideRemovesCategory() {
        val r = run(signals(explicit = 0.95f), o = listOf(OverrideRow(1, Category.OTHER_ADULT, true)))
        assertThat(r).doesNotContainKey(Category.OTHER_ADULT)
    }

    @Test fun excludingTheReviewTriggerDropsReview() {
        val r = run(signals(explicit = 0.6f), o = listOf(OverrideRow(1, Category.OTHER_ADULT, true)))
        assertThat(r).isEmpty()
    }

    @Test fun includeOverrideForcesAtOneAndDropsReview() {
        val r = run(signals(explicit = 0.6f), o = listOf(OverrideRow(1, Category.OTHER_ADULT, false)))
        assertThat(r).containsExactly(Category.OTHER_ADULT, 1.0f)
    }

    @Test fun includedMineSubOverrideImpliesMyNudes() {
        val r = run(signals(), o = listOf(OverrideRow(1, Category.MY_NUDES_OF_ME, false)))
        assertThat(r).containsExactly(Category.MY_NUDES_OF_ME, 1.0f, Category.MY_NUDES, 1.0f)
    }

    @Test fun dogPhotoIsPets() {
        assertThat(run(signals(labels = mapOf("Dog" to 0.9f, "Cat" to 0.75f, "Sky" to 0.5f))))
            .containsExactly(Category.PETS, 0.9f)
    }

    @Test fun foodAndNature() {
        val r = run(signals(labels = mapOf("food" to 0.8f, "Mountain" to 0.71f, "Tree" to 0.9f, "Dish" to 0.69f)))
        assertThat(r).containsExactly(Category.FOOD, 0.8f, Category.NATURE, 0.9f)
    }

    @Test fun selfies() {
        assertThat(run(signals(front = true))).containsExactly(Category.SELFIES, 1.0f)
        assertThat(run(signals(largeFace = true))).containsExactly(Category.SELFIES, 1.0f)
        assertThat(run(signals(front = false))).isEmpty()
    }

    @Test fun memeNeedsTextNonCameraAndPosterOrCartoon() {
        val labels = mapOf("Poster" to 0.8f, "Text" to 0.9f)
        assertThat(run(signals(labels = labels))).containsExactly(Category.MEMES, 0.8f)
        assertThat(run(signals(labels = mapOf("Poster" to 0.8f)))).isEmpty()
        assertThat(run(signals(labels = labels, sources = setOf(Category.CAMERA))))
            .containsExactly(Category.CAMERA, 1.0f)
        assertThat(run(signals(labels = mapOf("Cartoon" to 0.6f, "Text" to 0.9f)))).isEmpty()
    }

    @Test fun qrAndBlur() {
        assertThat(run(signals(hasQr = true, blur = 59.9)))
            .containsExactly(Category.QR_CODES, 1.0f, Category.BLURRY, 1.0f)
        assertThat(run(signals(blur = 60.0))).isEmpty()
    }

    @Test fun noDuplicateCategories() {
        val list = categorize(
            signals(explicit = 0.95f, ownCamera = true, sources = setOf(Category.CAMERA)),
            1L, listOf(OverrideRow(1, Category.MY_NUDES, false)),
        )
        assertThat(list.map { it.category }).containsNoDuplicates()
    }
}
