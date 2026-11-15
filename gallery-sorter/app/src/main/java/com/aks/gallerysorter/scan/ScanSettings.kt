package com.aks.gallerysorter.scan

import android.os.Build
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.places.Trip

/**
 * Scan-related settings, stored in the encrypted index via [SettingsDao].
 * [deviceModel] (this phone's model) always counts as an own camera model.
 */
class ScanSettings(private val dao: SettingsDao, private val deviceModel: String = Build.MODEL) {

    /** User toggle for the first full scan ("only while charging"); defaults to on. */
    suspend fun onlyWhileCharging(): Boolean = dao.get(ONLY_WHILE_CHARGING)?.toBoolean() ?: true
    suspend fun setOnlyWhileCharging(on: Boolean) = put(ONLY_WHILE_CHARGING, on.toString())

    suspend fun firstScanDone(): Boolean = dao.get(FIRST_SCAN_DONE)?.toBoolean() ?: false
    suspend fun markFirstScanDone() = put(FIRST_SCAN_DONE, "true")

    /** Background scans need the charger only until the first full scan has finished (spec 6.2). */
    suspend fun requiresCharging(): Boolean = !firstScanDone() && onlyWhileCharging()

    /** "Auto-hide new Sensitive items"; off by default. */
    suspend fun autoHideSensitive(): Boolean = dao.get(AUTO_HIDE_SENSITIVE)?.toBoolean() ?: false
    suspend fun setAutoHideSensitive(on: Boolean) = put(AUTO_HIDE_SENSITIVE, on.toString())

    /** This phone's model plus past phones the user confirmed as their own. */
    suspend fun ownCameraModels(): Set<String> {
        val stored = dao.get(OWN_CAMERA_MODELS)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        return (stored + deviceModel).toSet()
    }
    suspend fun setOwnCameraModels(models: Set<String>) =
        put(OWN_CAMERA_MODELS, models.map { it.replace('\n', ' ').trim() }.filter { it.isNotEmpty() }.joinToString("\n"))

    /** Trips from the last scan (recomputed after every run). */
    suspend fun trips(): List<Trip> = dao.get(TRIPS).orEmpty().lines().filter { it.isNotEmpty() }.map { line ->
        val (name, start, end, ids) = line.split('\t')
        Trip(name, start.toLong(), end.toLong(), ids.split(',').filter { it.isNotEmpty() }.map { it.toLong() })
    }
    suspend fun setTrips(trips: List<Trip>) = put(TRIPS, trips.joinToString("\n") { t ->
        "${t.name.replace('\t', ' ').replace('\n', ' ')}\t${t.startMs}\t${t.endMs}\t${t.itemIds.joinToString(",")}"
    })

    private suspend fun put(key: String, value: String) = dao.put(SettingRow(key, value))

    private companion object {
        const val ONLY_WHILE_CHARGING = "scan.only_while_charging"
        const val FIRST_SCAN_DONE = "scan.first_scan_done"
        const val AUTO_HIDE_SENSITIVE = "scan.auto_hide_sensitive"
        const val OWN_CAMERA_MODELS = "scan.own_camera_models"
        const val TRIPS = "scan.trips"
    }
}
