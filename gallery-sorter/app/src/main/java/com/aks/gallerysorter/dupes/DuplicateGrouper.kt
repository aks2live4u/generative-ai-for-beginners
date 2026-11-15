package com.aks.gallerysorter.dupes

import com.aks.gallerysorter.core.MediaKind

data class DupeCandidate(
    val id: Long,
    val kind: MediaKind,
    val sizeBytes: Long,
    val sha256: String?,
    val phash: Long?,
    val durationMs: Long?,
    val framePhashes: List<Long>,
    val width: Int,
    val height: Int,
    val isOwnCameraOriginal: Boolean,
    val exifDateMs: Long?,
)

private const val MAX_HAMMING = 4
private const val MAX_DURATION_DIFF_MS = 500L
private const val VIDEO_FRAMES = 3

/** Band widths; they sum to 64. Two hashes within 4 bits differ in at most 4 of 5 bands. */
private val BAND_WIDTHS = intArrayOf(13, 13, 13, 13, 12)

private class UnionFind(n: Int) {
    private val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) r = parent[r]
        var c = x
        while (parent[c] != r) { val nx = parent[c]; parent[c] = r; c = nx }
        return r
    }
    fun union(a: Int, b: Int) {
        val ra = find(a); val rb = find(b)
        if (ra != rb) parent[maxOf(ra, rb)] = minOf(ra, rb)
    }
}

/**
 * Groups exact (same sha256) and visually identical (pHash within 4 bits; videos also need
 * duration within 500 ms and all 3 frame hashes within 4 bits) items, with transitive closure.
 * Only same-kind items are compared. Groups of one are omitted. Groups are ordered by their
 * first member's position in [items]; members keep input order.
 */
fun groupDuplicates(items: List<DupeCandidate>): List<List<DupeCandidate>> {
    val uf = UnionFind(items.size)

    // Exact matches.
    val bySha = HashMap<Pair<MediaKind, String>, Int>()
    for ((i, it) in items.withIndex()) {
        val sha = it.sha256 ?: continue
        val first = bySha.putIfAbsent(it.kind to sha, i)
        if (first != null) uf.union(first, i)
    }

    // Near matches: images on phash, videos on the first frame's phash.
    val imageIdx = ArrayList<Int>()
    val videoIdx = ArrayList<Int>()
    for ((i, it) in items.withIndex()) {
        when (it.kind) {
            MediaKind.IMAGE -> if (it.phash != null) imageIdx.add(i)
            MediaKind.VIDEO ->
                if (it.durationMs != null && it.framePhashes.size == VIDEO_FRAMES) videoIdx.add(i)
        }
    }
    bandMatch(
        imageIdx.toIntArray(),
        key = { items[it].phash!! },
        verify = { a, b -> hamming(items[a].phash!!, items[b].phash!!) <= MAX_HAMMING },
        uf = uf,
    )
    bandMatch(
        videoIdx.toIntArray(),
        key = { items[it].framePhashes[0] },
        verify = { a, b -> videosMatch(items[a], items[b]) },
        uf = uf,
    )

    val groups = LinkedHashMap<Int, MutableList<DupeCandidate>>()
    for (i in items.indices) groups.getOrPut(uf.find(i)) { ArrayList(2) }.add(items[i])
    return groups.values.filter { it.size > 1 }
}

private fun videosMatch(a: DupeCandidate, b: DupeCandidate): Boolean {
    if (kotlin.math.abs(a.durationMs!! - b.durationMs!!) > MAX_DURATION_DIFF_MS) return false
    for (f in 0 until VIDEO_FRAMES) {
        if (hamming(a.framePhashes[f], b.framePhashes[f]) > MAX_HAMMING) return false
    }
    return true
}

/**
 * For each band, sorts candidates by that band's bits and verifies pairs within equal-valued runs.
 * Sorting packed (bandValue, position) longs avoids per-bucket allocation.
 */
private inline fun bandMatch(
    idx: IntArray,
    key: (Int) -> Long,
    verify: (Int, Int) -> Boolean,
    uf: UnionFind,
) {
    if (idx.size < 2) return
    val keys = LongArray(idx.size) { key(idx[it]) }
    val packed = LongArray(idx.size)
    var shift = 0
    for (width in BAND_WIDTHS) {
        val mask = (1L shl width) - 1
        for (p in idx.indices) packed[p] = (((keys[p] ushr shift) and mask) shl 32) or p.toLong()
        packed.sort()
        var start = 0
        while (start < packed.size) {
            val band = packed[start] ushr 32
            var end = start + 1
            while (end < packed.size && (packed[end] ushr 32) == band) end++
            for (x in start until end) {
                val a = idx[(packed[x] and 0xFFFFFFFFL).toInt()]
                for (y in x + 1 until end) {
                    val b = idx[(packed[y] and 0xFFFFFFFFL).toInt()]
                    if (uf.find(a) != uf.find(b) && verify(a, b)) uf.union(a, b)
                }
            }
            start = end
        }
        shift += width
    }
}
