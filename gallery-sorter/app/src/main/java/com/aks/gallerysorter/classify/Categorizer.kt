package com.aks.gallerysorter.classify

import com.aks.gallerysorter.classify.ml.NsfwScore
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.CategoryGroup
import com.aks.gallerysorter.index.entities.OverrideRow

/** Everything the categorizer needs; [largeSingleFace] (one face > 20% of the area) is pre-computed by the caller. */
data class Signals(
    val nsfw: NsfwScore?,
    val sources: Set<Category>,
    val ownCamera: Boolean,
    val doc: CategoryAssignment?,
    val labels: Map<String, Float>,
    val hasQr: Boolean,
    val blur: Double?,
    val faces: List<Long>,
    val frontCamera: Boolean?,
    val largeSingleFace: Boolean,
)

private const val SENSITIVE_MIN = 0.85f
private const val REVIEW_MIN = 0.50f
private const val LABEL_MIN = 0.7f
private const val MEME_CONFIDENCE = 0.8f

private val PET_LABELS = setOf("dog", "cat", "pet")
private val FOOD_LABELS = setOf("food", "dish", "cuisine")
private val NATURE_LABELS = setOf("plant", "sky", "mountain", "beach", "flower", "tree")
private val MEME_LABELS = setOf("poster", "cartoon")

private fun Map<String, Float>.best(names: Set<String>): Float? =
    entries.filter { it.key.lowercase() in names && it.value >= LABEL_MIN }.maxOfOrNull { it.value }

fun categorize(s: Signals, mePersonId: Long?, overrides: List<OverrideRow>): List<CategoryAssignment> {
    val out = LinkedHashMap<Category, Float>()
    var reviewConfidence: Float? = null
    val reviewTriggers = mutableSetOf<Category>()

    fun triggerReview(confidence: Float, by: Collection<Category>) {
        reviewConfidence = maxOf(reviewConfidence ?: confidence, confidence)
        reviewTriggers += by
    }

    // Sensitive (NSFW) rule.
    val explicit = s.nsfw?.explicit
    if (explicit != null && explicit >= REVIEW_MIN) {
        val mine = mineCategories(s.ownCamera, s.faces.toSet(), mePersonId)
        mine.forEach { out[it] = explicit }
        if (explicit < SENSITIVE_MIN) triggerReview(explicit, mine)
    }

    // Documents.
    s.doc?.let { doc ->
        out[doc.category] = doc.confidence
        if (doc.confidence >= REVIEW_MIN && doc.confidence < SENSITIVE_MIN) {
            triggerReview(doc.confidence, listOf(doc.category))
        }
    }

    // Source categories.
    s.sources.forEach { out[it] = 1.0f }

    // Content.
    if (s.frontCamera == true || s.largeSingleFace) out[Category.SELFIES] = 1.0f
    s.labels.best(PET_LABELS)?.let { out[Category.PETS] = it }
    s.labels.best(FOOD_LABELS)?.let { out[Category.FOOD] = it }
    s.labels.best(NATURE_LABELS)?.let { out[Category.NATURE] = it }
    // hasText is derived: an OCR document hit, or a "Text" scene label.
    val hasText = s.doc != null || s.labels.keys.any { it.lowercase() == "text" }
    if (hasText && Category.CAMERA !in s.sources && s.labels.best(MEME_LABELS) != null) {
        out[Category.MEMES] = MEME_CONFIDENCE
    }
    if (s.hasQr) out[Category.QR_CODES] = 1.0f

    // Cleanup.
    if (s.blur != null && s.blur < BLUR_THRESHOLD) out[Category.BLURRY] = 1.0f

    // Overrides: excluded removes, included forces at 1.0.
    var userForcedSensitiveOrDoc = false
    var reviewOverride: OverrideRow? = null
    for (o in overrides) {
        when {
            o.category == Category.REVIEW -> reviewOverride = o
            o.isExcluded -> out.remove(o.category)
            else -> {
                out[o.category] = 1.0f
                if (o.category == Category.MY_NUDES_CAMERA || o.category == Category.MY_NUDES_OF_ME) {
                    out[Category.MY_NUDES] = 1.0f
                }
                if (o.category.group == CategoryGroup.SENSITIVE || o.category.group == CategoryGroup.DOCUMENTS) {
                    userForcedSensitiveOrDoc = true
                }
            }
        }
    }

    // REVIEW: dropped once the user resolved what triggered it.
    val excluded = overrides.filter { it.isExcluded }.map { it.category }.toSet()
    val resolved = userForcedSensitiveOrDoc ||
        (reviewTriggers.isNotEmpty() && excluded.containsAll(reviewTriggers))
    val review = reviewConfidence
    if (review != null && !resolved) out[Category.REVIEW] = review
    reviewOverride?.let { if (it.isExcluded) out.remove(Category.REVIEW) else out[Category.REVIEW] = 1.0f }

    return out.map { CategoryAssignment(it.key, it.value) }
}
