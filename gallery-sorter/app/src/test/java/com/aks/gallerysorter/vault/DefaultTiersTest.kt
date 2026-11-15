package com.aks.gallerysorter.vault

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.VaultTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultTiersTest {
    @Test fun builtInDefaults_perCategory() {
        for (c in listOf(Category.MY_NUDES, Category.MY_NUDES_CAMERA, Category.MY_NUDES_OF_ME, Category.IDS)) {
            assertThat(defaultTierFor(listOf(c), emptyMap())).isEqualTo(VaultTier.HIGH_SECURITY)
        }
        for (c in listOf(Category.OTHER_ADULT, Category.BANKING, Category.OTHER_DOCS)) {
            assertThat(defaultTierFor(listOf(c), emptyMap())).isEqualTo(VaultTier.STANDARD)
        }
    }

    @Test fun mixedCategories_highSecurityWins() {
        assertThat(defaultTierFor(listOf(Category.OTHER_ADULT, Category.CAMERA, Category.MY_NUDES_CAMERA), emptyMap()))
            .isEqualTo(VaultTier.HIGH_SECURITY)
    }

    @Test fun userOverride_replacesBuiltInDefault() {
        val overrides = mapOf(Category.MY_NUDES to VaultTier.STANDARD, Category.BANKING to VaultTier.HIGH_SECURITY)
        assertThat(defaultTierFor(listOf(Category.MY_NUDES), overrides)).isEqualTo(VaultTier.STANDARD)
        assertThat(defaultTierFor(listOf(Category.BANKING, Category.OTHER_ADULT), overrides)).isEqualTo(VaultTier.HIGH_SECURITY)
    }

    @Test fun categoriesWithoutTier_fallBackToStandard() {
        assertThat(defaultTierFor(listOf(Category.CAMERA, Category.PETS), emptyMap())).isEqualTo(VaultTier.STANDARD)
        assertThat(defaultTierFor(emptyList(), emptyMap())).isEqualTo(VaultTier.STANDARD)
    }
}
