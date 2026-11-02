package com.nexus.launcher.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

data class Contact(val name: String, val number: String)

/** The device-side half of NEXUS's actions: contacts, calls, texts and hardware controls. */
class PhoneActions(private val context: Context) {

    fun has(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Resolves "Mum", "raj kumar" or "+44 7700 900123" to a contact and number.
     * Prefers an exact name, then a name starting with the query, then any word match.
     */
    fun findContact(query: String): Contact? {
        val q = query.trim()
        val digits = q.filter { it.isDigit() || it == '+' }
        if (digits.count(Char::isDigit) >= 3 && digits.length >= q.count { !it.isWhitespace() } - 2) return Contact(q, digits)
        if (!has(Manifest.permission.READ_CONTACTS)) return null
        val all = mutableListOf<Contact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val number = c.getString(1) ?: continue
                if (c.getInt(2) == 1) all.add(0, Contact(name, number)) else all += Contact(name, number)
            }
        }
        val lower = q.lowercase()
        return all.firstOrNull { it.name.equals(q, ignoreCase = true) }
            ?: all.firstOrNull { it.name.lowercase().startsWith(lower) }
            ?: all.firstOrNull { c -> c.name.lowercase().split(" ").any { it == lower } }
            ?: all.firstOrNull { it.name.lowercase().contains(lower) }
    }

    fun call(number: String): Boolean = start(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")))

    fun sendSms(number: String, message: String): Boolean = runCatching {
        val sms = smsManager()
        val parts = sms.divideMessage(message)
        sms.sendMultipartTextMessage(number, null, parts, null, null)
    }.isSuccess

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java) else SmsManager.getDefault()

    /** WhatsApp can't send on its own; this opens the chat with the message typed in. */
    fun whatsApp(number: String, message: String): Boolean {
        val digits = number.filter(Char::isDigit)
        return start(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")))
    }

    fun email(to: String, subject: String, body: String): Boolean = start(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(to)}"))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body),
    )

    fun directions(place: String): Boolean =
        start(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(place)}")))
            || start(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place)}")))

    fun playMusic(query: String): Boolean = start(
        Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            .putExtra(android.app.SearchManager.QUERY, query),
    )

    fun camera(video: Boolean): Boolean = start(
        Intent(if (video) MediaStore.INTENT_ACTION_VIDEO_CAMERA else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
    )

    fun calendarEvent(title: String, startMillis: Long?, minutes: Int): Boolean = start(
        Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .apply {
                if (startMillis != null) {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + minutes * 60_000L)
                }
            },
    )

    fun torch(on: Boolean): Boolean = runCatching {
        val cm = context.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.first { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        cm.setTorchMode(id, on)
    }.isSuccess

    /** Media volume as a percentage; shows the system volume bar so the change is visible. */
    fun volume(percent: Int): Boolean = runCatching {
        val am = context.getSystemService(AudioManager::class.java)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (percent.coerceIn(0, 100) * max + 50) / 100, AudioManager.FLAG_SHOW_UI)
    }.isSuccess

    /** Quick-settings style panels: wifi, internet, bluetooth, nfc, volume, display, battery. */
    fun settingsPanel(which: String): Boolean {
        val action = when (which.lowercase()) {
            "wifi", "wi-fi" -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
            "internet", "data", "mobile data" -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS
            "nfc" -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_NFC else Settings.ACTION_NFC_SETTINGS
            "volume", "sound" -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery", "battery saver" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "hotspot", "tethering" -> Settings.ACTION_WIRELESS_SETTINGS
            "airplane", "flight" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return start(Intent(action))
    }

    private fun start(intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
