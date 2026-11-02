package com.nexus.launcher.system

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tier { PRIORITY, DIGEST }

data class NexusNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val tier: Tier,
    val contentIntent: PendingIntent?,
)

/**
 * Reads the device's notifications (after the user grants access) and sorts them into
 * Priority (people, calls, reminders, alarms) and Digest (everything else).
 */
class NexusNotificationService : NotificationListenerService() {

    companion object {
        private val _items = MutableStateFlow<List<NexusNotification>>(emptyList())
        val items: StateFlow<List<NexusNotification>> = _items.asStateFlow()
        private var instance: NexusNotificationService? = null

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun dismiss(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissAll(keys: List<String>) {
            instance?.cancelNotifications(keys.toTypedArray())
        }
    }

    override fun onListenerConnected() {
        instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = runCatching { activeNotifications }.getOrNull().orEmpty()
        _items.value = list
            .filter { !it.isOngoing && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .mapNotNull(::toModel)
            .sortedByDescending { it.postTime }
    }

    private fun toModel(sbn: StatusBarNotification): NexusNotification? {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val priorityCategories = setOf(
            Notification.CATEGORY_MESSAGE, Notification.CATEGORY_CALL, Notification.CATEGORY_ALARM,
            Notification.CATEGORY_REMINDER, Notification.CATEGORY_EVENT, Notification.CATEGORY_EMAIL,
        )
        val hasPeople = extras.containsKey(Notification.EXTRA_PEOPLE_LIST) || n.category == Notification.CATEGORY_SOCIAL
        val tier = if (n.category in priorityCategories || hasPeople) Tier.PRIORITY else Tier.DIGEST
        return NexusNotification(sbn.key, sbn.packageName, label, title, text, sbn.postTime, tier, n.contentIntent)
    }
}
