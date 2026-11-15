package com.aks.gallerysorter.places

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TripPoint(val id: Long, val dateMs: Long, val city: City)

data class Trip(val name: String, val startMs: Long, val endMs: Long, val itemIds: List<Long>)

private const val AWAY_KM = 100.0
private const val MAX_GAP_DAYS = 2L

/**
 * Home = most frequent city by item count (tie: earliest seen). A trip is a run of
 * days whose items are more than 100 km from home; consecutive away days more than
 * 2 calendar days apart start a new trip. Days are local dates in [zone].
 */
fun detectTrips(points: List<TripPoint>, zone: ZoneId = ZoneId.systemDefault()): List<Trip> {
    if (points.isEmpty()) return emptyList()
    val sorted = points.sortedBy { it.dateMs }
    val home = mostFrequent(sorted.map { it.city })
    val away = sorted.filter {
        haversineKm(home.lat, home.lon, it.city.lat, it.city.lon) > AWAY_KM
    }
    if (away.isEmpty()) return emptyList()

    fun dayOf(p: TripPoint): LocalDate = Instant.ofEpochMilli(p.dateMs).atZone(zone).toLocalDate()

    val groups = mutableListOf<MutableList<TripPoint>>()
    var lastDay: LocalDate? = null
    for (p in away) {
        val d = dayOf(p)
        val prev = lastDay
        if (prev == null || d.toEpochDay() - prev.toEpochDay() > MAX_GAP_DAYS) {
            groups.add(mutableListOf())
        }
        groups.last().add(p)
        lastDay = d
    }

    val fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    return groups.map { g ->
        val start = g.first().dateMs
        Trip(
            name = "${mostFrequent(g.map { it.city }).name} · " +
                Instant.ofEpochMilli(start).atZone(zone).format(fmt),
            startMs = start,
            endMs = g.last().dateMs,
            itemIds = g.map { it.id },
        )
    }
}

/** Most frequent element; ties go to the one seen first. */
private fun mostFrequent(cities: List<City>): City {
    val counts = LinkedHashMap<City, Int>()
    for (c in cities) counts[c] = (counts[c] ?: 0) + 1
    var best: City? = null
    var bestN = 0
    for ((c, n) in counts) if (n > bestN) { best = c; bestN = n }
    return best!!
}
