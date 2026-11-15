package com.aks.gallerysorter.classify

import kotlin.math.sqrt

/**
 * Incremental face clustering (spec 6.3, "Faces"): a new face embedding joins the most similar
 * existing person if their cosine similarity is at least [threshold]; otherwise it starts a new person.
 */
class FaceClusterer(private val threshold: Float = 0.6f) {

    /**
     * Returns the id of the nearest person in [centroids] whose cosine similarity to [embedding] is
     * ≥ [threshold], or null when there is none (the caller creates a new person). Centroids need not
     * be normalised; ones whose dimension differs from [embedding] are ignored.
     */
    fun assign(embedding: FloatArray, centroids: Map<Long, FloatArray>): Long? {
        var bestId: Long? = null
        var bestSim = Float.NEGATIVE_INFINITY
        for ((id, centroid) in centroids) {
            if (centroid.size != embedding.size) continue
            val sim = cosine(embedding, centroid)
            if (sim > bestSim) {
                bestSim = sim
                bestId = id
            }
        }
        return if (bestId != null && bestSim >= threshold) bestId else null
    }
}

/** Cosine similarity of two equal-length vectors; 0 if either is all zeros. */
fun cosine(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "dimension mismatch: ${a.size} vs ${b.size}" }
    var dot = 0.0
    var na = 0.0
    var nb = 0.0
    for (i in a.indices) {
        dot += a[i] * b[i]
        na += a[i] * a[i]
        nb += b[i] * b[i]
    }
    if (na == 0.0 || nb == 0.0) return 0f
    return (dot / (sqrt(na) * sqrt(nb))).toFloat()
}

/** Returns [v] scaled to unit length (a copy); an all-zero vector is returned unchanged. */
fun l2Normalize(v: FloatArray): FloatArray {
    var sq = 0.0
    for (x in v) sq += x * x
    if (sq == 0.0) return v.copyOf()
    val inv = (1.0 / sqrt(sq)).toFloat()
    return FloatArray(v.size) { v[it] * inv }
}
