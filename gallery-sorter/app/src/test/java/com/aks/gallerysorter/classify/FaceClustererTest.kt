package com.aks.gallerysorter.classify

import com.google.common.truth.Truth.assertThat
import kotlin.math.sqrt
import org.junit.Test

class FaceClustererTest {
    private val dim = 128

    private fun basis(i: Int) = FloatArray(dim).also { it[i] = 1f }

    /** Unit vector with cosine [cos] to basis(0), the rest of its length along basis(1). */
    private fun atCosine(cos: Float): FloatArray =
        FloatArray(dim).also { it[0] = cos; it[1] = sqrt(1f - cos * cos) }

    @Test fun cosineNinetyJoinsTheExistingPerson() {
        val centroids = mapOf(7L to basis(0))
        assertThat(FaceClusterer().assign(atCosine(0.9f), centroids)).isEqualTo(7L)
    }

    @Test fun cosineThirtyIsANewPerson() {
        val centroids = mapOf(7L to basis(0))
        assertThat(FaceClusterer().assign(atCosine(0.3f), centroids)).isNull()
    }

    @Test fun picksTheNearestOfSeveralPeople() {
        val centroids = mapOf(1L to basis(0), 2L to atCosine(0.95f), 3L to basis(2))
        // Person 2's own vector has cosine 1 with itself but only 0.95 with person 1.
        assertThat(FaceClusterer().assign(atCosine(0.95f), centroids)).isEqualTo(2L)
        assertThat(FaceClusterer().assign(basis(2), centroids)).isEqualTo(3L)
    }

    @Test fun unnormalisedCentroidsAreComparedByDirection() {
        val centroids = mapOf(5L to FloatArray(dim).also { it[0] = 4f }) // e.g. a running sum
        assertThat(FaceClusterer().assign(atCosine(0.9f), centroids)).isEqualTo(5L)
    }

    @Test fun exactlyAtThresholdJoins() {
        assertThat(FaceClusterer(threshold = 0.6f).assign(atCosine(0.6f), mapOf(1L to basis(0))))
            .isEqualTo(1L)
    }

    @Test fun noPeopleYetMeansNewPerson() {
        assertThat(FaceClusterer().assign(basis(0), emptyMap())).isNull()
    }

    @Test fun mismatchedDimensionsAreSkipped() {
        assertThat(FaceClusterer().assign(basis(0), mapOf(1L to FloatArray(192).also { it[0] = 1f })))
            .isNull()
    }

    @Test fun l2NormaliseGivesUnitLength() {
        val v = l2Normalize(floatArrayOf(3f, 4f))
        assertThat(v[0]).isWithin(1e-6f).of(0.6f)
        assertThat(v[1]).isWithin(1e-6f).of(0.8f)
    }

    @Test fun l2NormaliseLeavesZeroVectorAlone() {
        assertThat(l2Normalize(floatArrayOf(0f, 0f)).toList()).containsExactly(0f, 0f).inOrder()
    }
}
