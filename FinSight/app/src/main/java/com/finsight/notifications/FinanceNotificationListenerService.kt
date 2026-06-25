package com.finsight.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.finsight.FinSightApp
import com.finsight.core.parser.NotificationTransactionParser
import com.finsight.core.parser.ParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Reads notifications only from the fixed allow-list of apps in [NotificationTransactionParser]
 * (Swiggy, Zomato, Uber, Ola, Blinkit, Amazon). Notifications from every other app are ignored
 * immediately and never inspected further. This service never calls cancelNotification or
 * otherwise modifies notifications - read-only by design.
 */
class FinanceNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!NotificationTransactionParser.isSupportedPackage(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return

        val postedAt = Instant.ofEpochMilli(sbn.postTime)
        val app = applicationContext as FinSightApp

        scope.launch {
            val result = NotificationTransactionParser.parse(packageName, title, text, postedAt)
            if (result is ParseResult.Success) {
                app.container.transactionRepository.insertIfNotDuplicate(result.transaction)
            }
        }
    }
}
