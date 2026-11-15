package com.aks.gallerysorter.places

import java.io.DataInputStream
import java.io.InputStream
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class City(val name: String, val country: String, val lat: Double, val lon: Double)

/** Great-circle distance in km. */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0088
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dp = p2 - p1
    val dl = Math.toRadians(lon2 - lon1)
    val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
    return 2 * r * asin(min(1.0, sqrt(a)))
}

/**
 * Offline nearest-city lookup over the bundled GeoNames cities15000 table
 * (cities.bin, sorted by latitude). Fully loaded into memory on construction.
 */
class CityIndex(bin: InputStream) {
    private val lats: FloatArray
    private val lons: FloatArray
    private val names: Array<String>
    private val countries: Array<String>

    init {
        DataInputStream(bin).use { d ->
            val n = d.readInt()
            lats = FloatArray(n)
            lons = FloatArray(n)
            names = Array(n) { "" }
            countries = Array(n) { "" }
            for (i in 0 until n) {
                lats[i] = d.readFloat()
                lons[i] = d.readFloat()
                names[i] = d.readUTF()
                countries[i] = d.readUTF()
            }
        }
    }

    /** Nearest city within [MAX_KM], or null. */
    fun nearest(lat: Double, lon: Double): City? {
        val lo = lowerBound((lat - WINDOW_DEG).toFloat())
        var best = -1
        var bestKm = Double.MAX_VALUE
        var i = lo
        while (i < lats.size && lats[i] <= lat + WINDOW_DEG) {
            val km = haversineKm(lat, lon, lats[i].toDouble(), lons[i].toDouble())
            if (km < bestKm) {
                bestKm = km
                best = i
            }
            i++
        }
        if (best < 0 || bestKm > MAX_KM) return null
        return City(names[best], countries[best], lats[best].toDouble(), lons[best].toDouble())
    }

    private fun lowerBound(v: Float): Int {
        var lo = 0
        var hi = lats.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (lats[mid] < v) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private companion object {
        const val MAX_KM = 50.0
        const val WINDOW_DEG = 0.45 // ~50 km of latitude
    }
}
