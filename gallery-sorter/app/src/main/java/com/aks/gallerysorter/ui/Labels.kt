package com.aks.gallerysorter.ui

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.core.VaultTier

val Category.label: String
    get() = when (this) {
        Category.MY_NUDES -> "My nudes"
        Category.MY_NUDES_CAMERA -> "My nudes (camera)"
        Category.MY_NUDES_OF_ME -> "Nudes of me"
        Category.OTHER_ADULT -> "Other adult content"
        Category.IDS -> "IDs"
        Category.BANKING -> "Banking"
        Category.OTHER_DOCS -> "Other documents"
        Category.CAMERA -> "Camera"
        Category.SCREENSHOTS -> "Screenshots"
        Category.SCREEN_RECORDINGS -> "Screen recordings"
        Category.WHATSAPP -> "WhatsApp"
        Category.TELEGRAM -> "Telegram"
        Category.OTHER_MESSAGING -> "Other messaging"
        Category.DOWNLOADS -> "Downloads"
        Category.SELFIES -> "Selfies"
        Category.PETS -> "Pets"
        Category.FOOD -> "Food"
        Category.NATURE -> "Nature"
        Category.MEMES -> "Memes"
        Category.QR_CODES -> "QR codes"
        Category.BLURRY -> "Blurry"
        Category.DUPLICATES -> "Duplicates"
        Category.REVIEW -> "Review"
    }

val CategoryGroup.label: String
    get() = when (this) {
        CategoryGroup.SENSITIVE -> "Sensitive"
        CategoryGroup.DOCUMENTS -> "Documents"
        CategoryGroup.SOURCE -> "Sources"
        CategoryGroup.CONTENT -> "Content"
        CategoryGroup.CLEANUP -> "Cleanup"
        CategoryGroup.REVIEW -> "Review"
    }

val VaultTier.label: String
    get() = when (this) {
        VaultTier.STANDARD -> "Standard"
        VaultTier.HIGH_SECURITY -> "High-security"
    }

/** Sensitive/Documents views need an unlocked vault (spec 6.5). */
val Category.requiresUnlock: Boolean get() = this in Category.lockedExcluded || this == Category.REVIEW
