package com.aks.gallerysorter.core

enum class MediaKind { IMAGE, VIDEO }

enum class ScanStatus { PENDING, DONE, UNREADABLE }

enum class VaultTier { STANDARD, HIGH_SECURITY }

enum class CategoryGroup { SENSITIVE, DOCUMENTS, SOURCE, CONTENT, CLEANUP, REVIEW }

enum class Category(val group: CategoryGroup) {
    MY_NUDES(CategoryGroup.SENSITIVE),
    MY_NUDES_CAMERA(CategoryGroup.SENSITIVE),
    MY_NUDES_OF_ME(CategoryGroup.SENSITIVE),
    OTHER_ADULT(CategoryGroup.SENSITIVE),
    IDS(CategoryGroup.DOCUMENTS),
    BANKING(CategoryGroup.DOCUMENTS),
    OTHER_DOCS(CategoryGroup.DOCUMENTS),
    CAMERA(CategoryGroup.SOURCE),
    SCREENSHOTS(CategoryGroup.SOURCE),
    SCREEN_RECORDINGS(CategoryGroup.SOURCE),
    WHATSAPP(CategoryGroup.SOURCE),
    TELEGRAM(CategoryGroup.SOURCE),
    OTHER_MESSAGING(CategoryGroup.SOURCE),
    DOWNLOADS(CategoryGroup.SOURCE),
    SELFIES(CategoryGroup.CONTENT),
    PETS(CategoryGroup.CONTENT),
    FOOD(CategoryGroup.CONTENT),
    NATURE(CategoryGroup.CONTENT),
    MEMES(CategoryGroup.CONTENT),
    QR_CODES(CategoryGroup.CONTENT),
    BLURRY(CategoryGroup.CLEANUP),
    DUPLICATES(CategoryGroup.CLEANUP),
    REVIEW(CategoryGroup.REVIEW);

    companion object {
        /** Categories hidden from every view while the app is locked. */
        val lockedExcluded: List<Category> =
            entries.filter { it.group == CategoryGroup.SENSITIVE || it.group == CategoryGroup.DOCUMENTS }
    }
}

data class MediaItem(
    val id: Long,
    val path: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val mime: String,
    val width: Int,
    val height: Int,
    val dateTakenMs: Long?,
    val durationMs: Long?,
    val generation: Long,
)

data class CategoryAssignment(val category: Category, val confidence: Float)
