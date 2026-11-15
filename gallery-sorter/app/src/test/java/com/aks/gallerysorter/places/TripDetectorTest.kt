package com.aks.gallerysorter.places

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class TripDetectorTest {
    private val utc = ZoneId.of("UTC")
    private val mumbai = City("Mumbai", "IN", 19.07, 72.88)
    private val panaji = City("Panaji", "IN", 15.50, 73.83)
    private val pune = City("Pune", "IN", 18.52, 73.86) // ~120 km from Mumbai
    private val thane = City("Thane", "IN", 19.20, 72.97) // near home

    private var nextId = 1L
    private fun day(date: String, city: City, n: Int = 1): List<TripPoint> {
        val ms = LocalDate.parse(date).atTime(12, 0).atZone(utc).toInstant().toEpochMilli()
        return List(n) { TripPoint(nextId++, ms + it * 1000L, city) }
    }

    @Test fun goaTripBetweenHomeDays() {
        val pts = (1..20).flatMap { day("2026-02-%02d".format(it), mumbai) } +
            (2..5).flatMap { day("2026-03-%02d".format(it), panaji, 3) } +
            day("2026-03-10", mumbai)
        val trips = detectTrips(pts, utc)
        assertThat(trips).hasSize(1)
        val t = trips[0]
        assertThat(t.name).isEqualTo("Panaji · Mar 2026")
        assertThat(t.itemIds).hasSize(12)
        assertThat(t.startMs).isEqualTo(day("2026-03-02", panaji)[0].dateMs)
        assertThat(t.endMs).isEqualTo(day("2026-03-05", panaji, 3)[2].dateMs)
    }

    @Test fun nearbyCitiesAreNotTrips() {
        val pts = (1..10).flatMap { day("2026-02-%02d".format(it), mumbai) } +
            day("2026-02-12", thane)
        assertThat(detectTrips(pts, utc)).isEmpty()
    }

    @Test fun gapOfMoreThanTwoDaysSplitsTrips() {
        val home = (1..10).flatMap { day("2026-01-%02d".format(it), mumbai, 2) }
        val a = day("2026-02-01", panaji) + day("2026-02-02", panaji) + day("2026-02-04", panaji)
        val b = day("2026-02-10", panaji)
        val trips = detectTrips(home + a + b, utc)
        assertThat(trips).hasSize(2)
        assertThat(trips[0].itemIds).hasSize(3)
        assertThat(trips[1].itemIds).hasSize(1)
    }

    @Test fun differentCitiesInOneTripUseMostFrequent() {
        val home = (1..10).flatMap { day("2026-01-%02d".format(it), mumbai, 2) }
        val away = day("2026-02-01", pune, 1) + day("2026-02-02", panaji, 3) + day("2026-02-03", pune, 1)
        val trips = detectTrips(home + away, utc)
        assertThat(trips).hasSize(1)
        assertThat(trips[0].name).isEqualTo("Panaji · Feb 2026")
    }

    @Test fun emptyInput() {
        assertThat(detectTrips(emptyList(), utc)).isEmpty()
    }

    @Test fun dayUsesGivenZone() {
        val ist = ZoneId.of("Asia/Kolkata")
        val home = (1..5).flatMap { day("2026-01-%02d".format(it), mumbai, 3) }
        // 2026-02-28 20:00 UTC is 2026-03-01 01:30 in IST.
        val ms = LocalDate.parse("2026-02-28").atTime(20, 0).atZone(utc).toInstant().toEpochMilli()
        val away = listOf(TripPoint(900, ms, panaji))
        assertThat(detectTrips(home + away, utc)[0].name).isEqualTo("Panaji · Feb 2026")
        assertThat(detectTrips(home + away, ist)[0].name).isEqualTo("Panaji · Mar 2026")
    }

    @Test fun zoneChangesGapSplitting() {
        val ist = ZoneId.of("Asia/Kolkata")
        val home = (1..5).flatMap { day("2026-01-%02d".format(it), mumbai, 3) }
        fun at(date: String, hour: Int, id: Long) = TripPoint(
            id, LocalDate.parse(date).atTime(hour, 0).atZone(utc).toInstant().toEpochMilli(), panaji,
        )
        val far = listOf(at("2026-02-01", 20, 900), at("2026-02-04", 3, 901))
        // UTC: Feb 1 vs Feb 4 = 3 apart -> 2 trips. IST: Feb 2 vs Feb 4 = 2 apart -> 1 trip.
        assertThat(detectTrips(home + far, utc)).hasSize(2)
        assertThat(detectTrips(home + far, ist)).hasSize(1)
    }

    @Test fun homeIsMetroClusterNotSingleSuburb() {
        val thaneC = City("Thane", "IN", 19.20, 72.97)
        val navi = City("Navi Mumbai", "IN", 19.03, 73.03)
        val home = (1..15).flatMap { day("2026-01-%02d".format(it), mumbai) } +
            (1..15).flatMap { day("2026-01-%02d".format(it), thaneC) } +
            (1..15).flatMap { day("2026-01-%02d".format(it), navi) }
        val goa = (1..40).flatMap { day("2026-03-01", panaji) }
        val trips = detectTrips(home + goa, utc)
        assertThat(trips).hasSize(1)
        assertThat(trips[0].name).isEqualTo("Panaji · Mar 2026")
        assertThat(trips[0].itemIds).hasSize(40)
    }
}
