package com.aks.gallerysorter.classify

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlurDetectorTest {
    private val w = 64
    private val h = 64

    /** 8×8-pixel squares alternating 0 and 255. */
    private fun checkerboard(): IntArray =
        IntArray(w * h) { i -> if (((i % w) / 8 + (i / w) / 8) % 2 == 0) 0 else 255 }

    /** Box blur with the given radius, clamping at the edges. */
    private fun boxBlur(src: IntArray, radius: Int): IntArray = IntArray(w * h) { i ->
        val x = i % w
        val y = i / w
        var sum = 0
        var n = 0
        for (dy in -radius..radius) for (dx in -radius..radius) {
            val xx = (x + dx).coerceIn(0, w - 1)
            val yy = (y + dy).coerceIn(0, h - 1)
            sum += src[yy * w + xx]
            n++
        }
        sum / n
    }

    @Test fun sharpCheckerboardIsAboveThreshold() {
        assertThat(blurScore(checkerboard(), w, h)).isGreaterThan(BLUR_THRESHOLD)
    }

    @Test fun blurredCheckerboardScoresLowerThanSharp() {
        val sharp = blurScore(checkerboard(), w, h)
        val blurred = blurScore(boxBlur(checkerboard(), 3), w, h)
        assertThat(blurred).isLessThan(sharp)
    }

    @Test fun smoothGradientIsBelowThreshold() {
        // A linear ramp has no second derivative, like a strongly defocused photo.
        assertThat(blurScore(IntArray(w * h) { i -> (i % w) * 4 }, w, h)).isLessThan(BLUR_THRESHOLD)
    }

    @Test fun flatImageScoresZero() {
        assertThat(blurScore(IntArray(w * h) { 128 }, w, h)).isEqualTo(0.0)
    }

    @Test fun imageTooSmallForTheKernelScoresZero() {
        assertThat(blurScore(intArrayOf(0, 255, 0, 255), 2, 2)).isEqualTo(0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sizeMismatchIsRejected() {
        blurScore(IntArray(10), 4, 4)
    }
}
