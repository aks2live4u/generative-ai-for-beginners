package com.aks.gallerysorter.dupes

import com.google.common.truth.Truth.assertThat
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import org.junit.Test

class HashingTest {
    @Test fun sha256OfAbc() {
        assertThat(sha256("abc".byteInputStream()))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test fun sha256StreamsLargeInput() {
        val data = ByteArray(300_000) { (it % 251).toByte() }
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }
        assertThat(sha256(data.inputStream())).isEqualTo(expected)
    }

    @Test fun hammingCountsDifferingBits() {
        assertThat(hamming(0L, 0L)).isEqualTo(0)
        assertThat(hamming(0b1011L, 0b0001L)).isEqualTo(2)
        assertThat(hamming(-1L, 0L)).isEqualTo(64)
    }

    // Synthetic scene: DC 128 plus the 8x8 lowest DCT basis functions with seeded amplitudes of
    // 8..12 grey levels. Exactly 31 AC terms are positive, 31 negative and one (the median) is 0,
    // so the pHash bits are well separated from the median and robust to small pixel noise.
    private val amplitudes: Array<DoubleArray> = run {
        val rnd = Random(7)
        val signs = (List(31) { 1 } + List(31) { -1 } + listOf(0)).shuffled(rnd)
        Array(8) { i -> DoubleArray(8) { j -> if (i == 0 && j == 0) 0.0 else {
            val k = i * 8 + j - 1
            signs[k] * (8 + rnd.nextInt(5)).toDouble()
        } } }
    }

    private fun scene(u: Double, v: Double): Double {
        var s = 128.0
        for (i in 0 until 8) for (j in 0 until 8) s += amplitudes[i][j] * cos(PI * i * u) * cos(PI * j * v)
        return s.coerceIn(0.0, 255.0)
    }

    private fun render(size: Int, f: (Double, Double) -> Double) =
        IntArray(size * size) { f((it % size + 0.5) / size, (it / size + 0.5) / size).toInt() }

    private fun boxDownsample2x(src: IntArray, size: Int): IntArray {
        val out = size / 2
        return IntArray(out * out) {
            val x = (it % out) * 2
            val y = (it / out) * 2
            (src[y * size + x] + src[y * size + x + 1] + src[(y + 1) * size + x] +
                src[(y + 1) * size + x + 1]) / 4
        }
    }

    private fun addNoise(img: IntArray, seed: Long): IntArray {
        val r = Random(seed)
        return IntArray(img.size) { (img[it] + r.nextInt(17) - 8).coerceIn(0, 255) }
    }

    @Test fun downscaledNoisyCopyIsWithin4Bits() {
        val original = pHash(render(32, ::scene))
        val copy = pHash(addNoise(boxDownsample2x(render(64, ::scene), 64), seed = 42))
        assertThat(hamming(original, copy)).isAtMost(4)
    }

    @Test fun differentImageIsMoreThan10BitsAway() {
        val original = pHash(render(32, ::scene))
        val checker = pHash(IntArray(32 * 32) { if ((it % 32 / 4 + it / 32 / 4) % 2 == 0) 230 else 25 })
        assertThat(hamming(original, checker)).isGreaterThan(10)
    }

    @Test fun dcBitIsAlwaysZero() {
        assertThat(pHash(render(32, ::scene)) and 1L).isEqualTo(0L)
    }
}
