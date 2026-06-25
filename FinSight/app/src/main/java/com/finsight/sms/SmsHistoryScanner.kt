package com.finsight.sms

import android.content.Context
import android.provider.Telephony
import com.finsight.core.parser.ParseResult
import com.finsight.core.parser.SmsTransactionParser
import com.finsight.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * One-time historical scan of the device's SMS inbox, run during onboarding's "Initial Scan"
 * step to build the first transaction history. Read-only: queries [Telephony.Sms.Inbox] via
 * ContentResolver, never writes back to the SMS provider.
 */
class SmsHistoryScanner(
    private val context: Context,
    private val transactionRepository: TransactionRepository
) {
    /** Returns the number of new transactions imported. */
    suspend fun scanHistory(onProgress: (scanned: Int, imported: Int) -> Unit = { _, _ -> }): Int = withContext(Dispatchers.IO) {
        var scanned = 0
        var imported = 0

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
            null,
            null,
            "${Telephony.Sms.Inbox.DATE} DESC"
        )

        cursor?.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext()) {
                scanned++
                val sender = it.getString(addressIdx) ?: continue
                val body = it.getString(bodyIdx) ?: continue
                val dateMillis = it.getLong(dateIdx)

                val result = SmsTransactionParser.parse(body, Instant.ofEpochMilli(dateMillis))
                if (result is ParseResult.Success) {
                    val wasNew = transactionRepository.insertIfNotDuplicate(
                        result.transaction.copy(notes = "From SMS: $sender")
                    )
                    if (wasNew) imported++
                }
                if (scanned % 25 == 0) onProgress(scanned, imported)
            }
        }

        onProgress(scanned, imported)
        imported
    }
}
