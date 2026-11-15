package com.aks.gallerysorter.dupes

import com.aks.gallerysorter.core.MediaKind
import java.util.IdentityHashMap

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
 * duration within 500 ms and all 3 frame hashes within 4 bits) items. Only same-kind items are
 * compared. Groups are not transitive: candidates are first linked through any chain of matches,
 * then each candidate group keeps only the members that match its [suggestKeep] directly; the
 * rest are regrouped the same way, or dropped when nothing matches. Groups of one are omitted.
 * Groups are ordered by their first member's position in [items]; members keep input order.
 */
fun groupDuplicates(items: List<DupeCandidate>): List<List<DupeCandidate>> {
    val out = ArrayList<List<DupeCandidate>>()
    val work = ArrayDeque(candidateGroups(items))
    while (work.isNotEmpty()) {
        val group = work.removeFirst()
        val keep = suggestKeep(group)
        val (members, rest) = group.partition { it === keep || matchesKeep(keep, it) }
        if (members.size > 1) out += members
        if (rest.size > 1) work.addAll(candidateGroups(rest))
    }
    val position = IdentityHashMap<DupeCandidate, Int>(items.size).apply { items.forEachIndexed { i, it -> put(it, i) } }
    return out.sortedBy { position.getValue(it.first()) }
}

/** A direct match with the group's keep: same sha256, or within the near-match rule for its kind. */
private fun matchesKeep(keep: DupeCandidate, x: DupeCandidate): Boolean {
    if (keep.kind != x.kind) return false
    if (keep.sha256 != null && keep.sha256 == x.sha256) return true
    return when (keep.kind) {
        MediaKind.IMAGE -> keep.phash != null && x.phash != null && hamming(keep.phash, x.phash) <= MAX_HAMMING
        MediaKind.VIDEO -> videoEligible(keep) && videoEligible(x) && videosMatch(keep, x)
    }
}

private fun videoEligible(v: DupeCandidate) = v.durationMs != null && v.framePhashes.size == VIDEO_FRAMES

/** Transitive candidate groups (union-find over every match), in input order; singletons omitted. */
private fun candidateGroups(items: List<DupeCandidate>): List<List<DupeCandidate>> {
    val uf = UnionFind(items.size)

    // Exact matches.
    val bySha = HashMap<Pair<MediaKind, String>, Int>()
    for ((i, it) in items.withIndex()) {
        val sha = it.sha256 ?: continue
        val first = bySha.putIfAbsent(it.kind to sha, i)
        if (first != null) uf.union(first, i)
    }

    // Equal full hashes are matches outright: collapse them to one representative before the band
    // pass, so thousands of identical hashes don't make one huge bucket.
    val imageRep = HashMap<Long, Int>()
    val videoRep = HashMap<Pair<Long, List<Long>>, Int>()
    for ((i, it) in items.withIndex()) {
        when (it.kind) {
            MediaKind.IMAGE -> it.phash?.let { h -> imageRep.putIfAbsent(h, i)?.let { r -> uf.union(r, i) } }
            MediaKind.VIDEO -> if (videoEligible(it)) {
                videoRep.putIfAbsent(it.durationMs!! to it.framePhashes, i)?.let { r -> uf.union(r, i) }
            }
        }
    }

    // Near matches between representatives: images on phash, videos on the first frame's phash.
    bandMatch(
        imageRep.values.sorted().toIntArray(),
        key = { items[it].phash!! },
        verify = { a, b -> hamming(items[a].phash!!, items[b].phash!!) <= MAX_HAMMING },
        uf = uf,
    )
    bandMatch(
        videoRep.values.sorted().toIntArray(),
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
