package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MineRuleTest {
    @Test fun ownCameraNoFace() {
        assertThat(mineCategories(true, emptySet(), 1L))
            .containsExactly(Category.MY_NUDES, Category.MY_NUDES_CAMERA)
    }

    @Test fun meFaceNotOwnCamera() {
        assertThat(mineCategories(false, setOf(1L, 2L), 1L))
            .containsExactly(Category.MY_NUDES, Category.MY_NUDES_OF_ME)
    }

    @Test fun both() {
        assertThat(mineCategories(true, setOf(1L), 1L))
            .containsExactly(Category.MY_NUDES, Category.MY_NUDES_CAMERA, Category.MY_NUDES_OF_ME)
    }

    @Test fun neither() {
        assertThat(mineCategories(false, setOf(2L), 1L)).containsExactly(Category.OTHER_ADULT)
    }

    @Test fun noMePersonMeansOfMeNeverApplies() {
        assertThat(mineCategories(false, setOf(1L), null)).containsExactly(Category.OTHER_ADULT)
    }
}
