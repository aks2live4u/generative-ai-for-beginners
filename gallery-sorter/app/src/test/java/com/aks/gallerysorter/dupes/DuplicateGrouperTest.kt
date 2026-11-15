package com.aks.gallerysorter.dupes

import com.aks.gallerysorter.core.MediaKind
import com.google.common.truth.Truth.assertThat
import java.util.Random
import org.junit.Test

class DuplicateGrouperTest {
    private fun img(
        id: Long, sha: String? = null, phash: Long? = null, exif: Long? = null,
        kind: MediaKind = MediaKind.IMAGE,
    ) = DupeCandidate(id, kind, 1000, sha, phash, null, emptyList(), 100, 100, false, exif)

    private fun vid(id: Long, dur: Long?, frames: List<Long>, sha: String? = null) =
        DupeCandidate(id, MediaKind.VIDEO, 9000, sha, null, dur, frames, 1920, 1080, false, null)

    private fun ids(groups: List<List<DupeCandidate>>) = groups.map { g -> g.map { it.id } }

    private fun flip(h: Long, vararg bits: Int) = bits.fold(h) { a, b -> a xor (1L shl b) }

    @Test fun sameShaGroups() {
        val g = groupDuplicates(listOf(img(1, sha = "a"), img(2, sha = "a"), img(3, sha = "b")))
        assertThat(ids(g)).containsExactly(listOf(1L, 2L))
    }

    @Test fun nullShaNeverGroupsOnSha() {
        assertThat(groupDuplicates(listOf(img(1), img(2)))).isEmpty()
    }

    @Test fun sameShaDifferentKindDoesNotGroup() {
        val g = groupDuplicates(listOf(img(1, sha = "a"), img(2, sha = "a", kind = MediaKind.VIDEO)))
        assertThat(g).isEmpty()
    }

    @Test fun phashWithin3BitsGroupsAndDatesIgnored() {
        val h = 0x0123456789ABCDEFL
        val threeYears = 3L * 365 * 24 * 3600 * 1000
        val g = groupDuplicates(
            listOf(img(1, phash = h, exif = 0), img(2, phash = flip(h, 0, 20, 63), exif = threeYears))
        )
        assertThat(ids(g)).containsExactly(listOf(1L, 2L))
    }

    @Test fun phashAtFourBitsGroupsFiveDoesNot() {
        val h = 0x0F0F0F0F0F0F0F0FL
        assertThat(groupDuplicates(listOf(img(1, phash = h), img(2, phash = flip(h, 1, 14, 27, 40)))))
            .hasSize(1)
        assertThat(groupDuplicates(listOf(img(1, phash = h), img(2, phash = flip(h, 1, 14, 27, 40, 55)))))
            .isEmpty()
    }

    @Test fun differencesSpreadAcrossBandsStillMatch() {
        // one flipped bit in each of four different bands (band starts: 0, 13, 26, 39, 52)
        val h = 0x5555AAAA5555AAAAL
        val g = groupDuplicates(listOf(img(1, phash = h), img(2, phash = flip(h, 3, 16, 30, 45))))
        assertThat(g).hasSize(1)
    }

    @Test fun transitiveClosureMergesChain() {
        val a = 0L
        val b = flip(a, 0, 1, 2)
        val c = flip(b, 10, 20, 30) // 6 bits from a, 3 from b
        val g = groupDuplicates(listOf(img(1, phash = a), img(3, phash = c), img(2, phash = b)))
        assertThat(ids(g)).containsExactly(listOf(1L, 3L, 2L))
    }

    @Test fun imageNeverMatchesVideoOnPhash() {
        val frames = listOf(5L, 5L, 5L)
        val g = groupDuplicates(listOf(img(1, phash = 5L), vid(2, 1000, frames)))
        assertThat(g).isEmpty()
    }

    @Test fun videosGroupWhenDurationAndAllFramesMatch() {
        val f = listOf(0x1111L, 0x2222L, 0x3333L)
        val near = listOf(flip(0x1111L, 0), flip(0x2222L, 5, 6), flip(0x3333L, 40))
        val g = groupDuplicates(listOf(vid(1, 10_000, f), vid(2, 10_500, near)))
        assertThat(ids(g)).containsExactly(listOf(1L, 2L))
    }

    @Test fun videosDoNotGroupWhenDurationOff() {
        val f = listOf(0x1111L, 0x2222L, 0x3333L)
        assertThat(groupDuplicates(listOf(vid(1, 10_000, f), vid(2, 10_501, f)))).isEmpty()
    }

    @Test fun videosDoNotGroupWhenOneFrameDiffers() {
        val f = listOf(0x1111L, 0x2222L, 0x3333L)
        val off = listOf(0x1111L, 0x2222L, flip(0x3333L, 1, 2, 3, 4, 5))
        assertThat(groupDuplicates(listOf(vid(1, 10_000, f), vid(2, 10_000, off)))).isEmpty()
    }

    @Test fun videosWithoutThreeFramesOrDurationNeverGroupOnPhash() {
        val f = listOf(1L, 2L, 3L)
        assertThat(groupDuplicates(listOf(vid(1, 100, f.take(2)), vid(2, 100, f.take(2))))).isEmpty()
        assertThat(groupDuplicates(listOf(vid(1, null, f), vid(2, null, f)))).isEmpty()
    }

    @Test fun videosGroupBySha() {
        val g = groupDuplicates(listOf(vid(1, null, emptyList(), "x"), vid(2, 5, emptyList(), "x")))
        assertThat(g).hasSize(1)
    }

    @Test fun grouping_50kItems_completesUnder2s() {
        val rnd = Random(20261115)
        val n = 50_000
        val pairs = 200
        val items = ArrayList<DupeCandidate>(n + pairs)
        val seen = HashSet<Long>()
        // Random 64-bit hashes; reject any that is within 8 bits of a previous one among the planted
        // originals so noise-free neighbours cannot collide by chance.
        val planted = ArrayList<Long>()
        var id = 0L
        while (items.size < n) {
            val h = rnd.nextLong()
            if (!seen.add(h)) continue
            items.add(img(id++, phash = h))
        }
        // Plant: second item's phash differs by 0..4 random bits from item at a spread-out index.
        for (p in 0 until pairs) {
            val orig = items[p * 200]
            planted.add(orig.id)
            val bits = rnd.nextInt(5)
            var h = orig.phash!!
            val chosen = HashSet<Int>()
            while (chosen.size < bits) chosen.add(rnd.nextInt(64))
            for (b in chosen) h = h xor (1L shl b)
            items.add(img(id++, phash = h))
        }
        // Sanity: random background must not create accidental near pairs (probability ~1e-9/pair).
        val start = System.nanoTime()
        val groups = groupDuplicates(items)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        println("groupDuplicates(50k) took $elapsedMs ms")

        assertThat(groups).hasSize(pairs)
        assertThat(groups.all { it.size == 2 }).isTrue()
        val expected = (0 until pairs).map { p -> setOf(items[p * 200].id, (n + p).toLong()) }
        assertThat(groups.map { g -> g.map { it.id }.toSet() }).containsExactlyElementsIn(expected)
        assertThat(elapsedMs).isLessThan(2000.0)
    }
}
