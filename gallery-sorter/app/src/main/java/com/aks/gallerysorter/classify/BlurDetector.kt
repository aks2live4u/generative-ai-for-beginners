package com.aks.gallerysorter.classify

/** Items whose [blurScore] is below this are treated as blurry (spec 6.3). */
const val BLUR_THRESHOLD = 60.0

/**
 * Variance of the Laplacian of a greyscale image (spec 6.3, "Blurry"); low values mean few sharp edges.
 *
 * [gray] holds [w]×[h] luminance values in 0..255, row-major. The 4-neighbour Laplacian kernel
 * (0 1 0 / 1 -4 1 / 0 1 0) is applied to every interior pixel. Images smaller than 3×3 score 0.
 */
fun blurScore(gray: IntArray, w: Int, h: Int): Double {
    require(w >= 0 && h >= 0 && gray.size == w * h) { "gray has ${gray.size} values, expected $w×$h" }
    if (w < 3 || h < 3) return 0.0
    var sum = 0.0
    var sumSq = 0.0
    for (y in 1 until h - 1) {
        val row = y * w
        for (x in 1 until w - 1) {
            val i = row + x
            val lap = (gray[i - 1] + gray[i + 1] + gray[i - w] + gray[i + w] - 4 * gray[i]).toDouble()
            sum += lap
            sumSq += lap * lap
        }
    }
    val n = ((w - 2) * (h - 2)).toDouble()
    val mean = sum / n
    return sumSq / n - mean * mean
}
