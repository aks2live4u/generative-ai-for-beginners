package com.finsight.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.finsight.FinSightApp
import com.finsight.core.parser.ParseResult
import com.finsight.core.parser.SmsTransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Receives incoming SMS in real time and, if it looks like a bank/UPI transaction, parses and
 * stores it. This receiver is read-only: it never calls SmsManager.sendTextMessage or otherwise
 * writes to the SMS provider.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().originatingAddress ?: "unknown"
        val timestamp = messages.first().timestampMillis
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        val app = context.applicationContext as FinSightApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SmsTransactionParser.parse(fullBody, Instant.ofEpochMilli(timestamp))
                if (result is ParseResult.Success) {
                    app.container.transactionRepository.insertIfNotDuplicate(
                        result.transaction.copy(notes = "From SMS: $sender")
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
