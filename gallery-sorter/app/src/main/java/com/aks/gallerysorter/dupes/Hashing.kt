package com.aks.gallerysorter.dupes

import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** Streams [input] through SHA-256 and returns lowercase hex. Does not close the stream. */
fun sha256(input: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = ByteArray(64 * 1024)
    while (true) {
        val n = input.read(buf)
        if (n < 0) break
        digest.update(buf, 0, n)
    }
    val hex = "0123456789abcdef"
    val out = StringBuilder(64)
    for (b in digest.digest()) {
        out.append(hex[(b.toInt() shr 4) and 0xF]).append(hex[b.toInt() and 0xF])
    }
    return out.toString()
}

private const val N = 32
private const val K = 8

/** COS_TABLE[u][x] = cos((2x+1) * u * PI / 2N) for the 8 lowest frequencies, computed once. */
private val COS_TABLE: Array<DoubleArray> = Array(K) { u ->
    DoubleArray(N) { x -> cos((2 * x + 1) * u * PI / (2.0 * N)) }
}

/**
 * Perceptual hash of a 32x32 grayscale image (row-major, 1024 values).
 * DCT-II, top-left 8x8 block; bit (u*8+v) is set when that coefficient exceeds the median of the
 * 63 AC coefficients. The DC coefficient is excluded from the median and its bit is 0.
 */
fun pHash(gray32x32: IntArray): Long {
    require(gray32x32.size == N * N) { "expected ${N * N} pixels, got ${gray32x32.size}" }
    // Separable DCT: first along x for the K lowest frequencies, for every row.
    val rows = Array(N) { DoubleArray(K) }
    for (y in 0 until N) {
        for (v in 0 until K) {
            var s = 0.0
            val c = COS_TABLE[v]
            for (x in 0 until N) s += gray32x32[y * N + x] * c[x]
            rows[y][v] = s
        }
    }
    val coef = DoubleArray(K * K)
    for (u in 0 until K) {
        val c = COS_TABLE[u]
        for (v in 0 until K) {
            var s = 0.0
            for (y in 0 until N) s += rows[y][v] * c[y]
            coef[u * K + v] = s
        }
    }
    val ac = coef.copyOfRange(1, K * K).also { it.sort() }
    val median = ac[ac.size / 2]
    var hash = 0L
    for (i in 1 until K * K) {
        if (coef[i] > median) hash = hash or (1L shl i)
    }
    return hash
}

fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
