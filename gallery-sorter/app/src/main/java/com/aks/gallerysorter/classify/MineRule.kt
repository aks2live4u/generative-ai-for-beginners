package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category

/**
 * Sensitive-image ownership: own-camera original and/or the "me" person appears -> My nudes
 * (with its sub-categories); anything else is [Category.OTHER_ADULT]. A null [mePersonId] means
 * no face is "me", so OF_ME cannot apply.
 */
fun mineCategories(
    isOwnCameraOriginal: Boolean,
    facePersonIds: Set<Long>,
    mePersonId: Long?,
): Set<Category> {
    val ofMe = mePersonId != null && mePersonId in facePersonIds
    if (!isOwnCameraOriginal && !ofMe) return setOf(Category.OTHER_ADULT)
    return buildSet {
        add(Category.MY_NUDES)
        if (isOwnCameraOriginal) add(Category.MY_NUDES_CAMERA)
        if (ofMe) add(Category.MY_NUDES_OF_ME)
    }
}
