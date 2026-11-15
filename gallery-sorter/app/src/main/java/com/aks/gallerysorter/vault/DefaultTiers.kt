package com.aks.gallerysorter.vault

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.entities.SettingRow

/** Built-in vault tier per category (spec 6.5); the user can change each in Settings. */
val BUILT_IN_TIERS: Map<Category, VaultTier> = mapOf(
    Category.MY_NUDES to VaultTier.HIGH_SECURITY,
    Category.MY_NUDES_CAMERA to VaultTier.HIGH_SECURITY,
    Category.MY_NUDES_OF_ME to VaultTier.HIGH_SECURITY,
    Category.IDS to VaultTier.HIGH_SECURITY,
    Category.OTHER_ADULT to VaultTier.STANDARD,
    Category.BANKING to VaultTier.STANDARD,
    Category.OTHER_DOCS to VaultTier.STANDARD,
)

/**
 * The tier an item with [categories] goes to by default: each category's tier ([overrides] first,
 * then [BUILT_IN_TIERS]); high-security if any of them says so, else standard.
 */
fun defaultTierFor(categories: Collection<Category>, overrides: Map<Category, VaultTier>): VaultTier =
    if (categories.any { (overrides[it] ?: BUILT_IN_TIERS[it]) == VaultTier.HIGH_SECURITY }) {
        VaultTier.HIGH_SECURITY
    } else {
        VaultTier.STANDARD
    }

/** User-chosen default tiers, stored in the index as `tier.<CATEGORY>` = tier name. */
class TierSettings(private val dao: SettingsDao) {
    suspend fun overrides(): Map<Category, VaultTier> = Category.entries.mapNotNull { c ->
        dao.get(key(c))?.let { v -> VaultTier.entries.firstOrNull { it.name == v }?.let { c to it } }
    }.toMap()

    suspend fun set(category: Category, tier: VaultTier) = dao.put(SettingRow(key(category), tier.name))

    private fun key(c: Category) = "tier.${c.name}"
}
