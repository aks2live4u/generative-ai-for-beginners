package com.personalfinanceai.gmail

import com.personalfinanceai.core.parser.EmailTransactionParser
import com.personalfinanceai.core.parser.ParseResult
import com.personalfinanceai.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Calls the Gmail REST API (read-only scope) directly over HTTPS using org.json - this avoids
 * pulling in the much heavier google-api-services-gmail client library for what's a handful of
 * simple GET requests. Only `users.messages.list` and `users.messages.get` are ever called;
 * nothing in this class can send, modify, or delete mail.
 */
class GmailScanner(private val transactionRepository: TransactionRepository) {

    private val searchQuery = "newer_than:180d (amazon OR flipkart OR myntra OR netflix OR salary " +
        "OR payslip OR insurance OR electricity OR recharge OR invoice OR receipt OR order)"

    /** Returns the number of new transactions imported. */
    suspend fun scanRecentEmails(accessToken: String, maxResults: Int = 50): Int = withContext(Dispatchers.IO) {
        val messageIds = listMessageIds(accessToken, maxResults)
        var imported = 0
        for (id in messageIds) {
            val message = getMessage(accessToken, id) ?: continue
            val result = EmailTransactionParser.parse(
                senderEmail = message.from,
                subject = message.subject,
                bodySnippet = message.snippet,
                receivedAt = message.receivedAt
            )
            if (result is ParseResult.Success) {
                val wasNew = transactionRepository.insertIfNotDuplicate(result.transaction)
                if (wasNew) imported++
            }
        }
        imported
    }

    private fun listMessageIds(accessToken: String, maxResults: Int): List<String> {
        val url = URL(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages" +
                "?maxResults=$maxResults&q=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
        )
        val json = httpGetJson(url, accessToken) ?: return emptyList()
        val messagesArray = json.optJSONArray("messages") ?: return emptyList()
        return (0 until messagesArray.length()).map { messagesArray.getJSONObject(it).getString("id") }
    }

    private fun getMessage(accessToken: String, messageId: String): GmailMessage? {
        val url = URL(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId" +
                "?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date"
        )
        val json = httpGetJson(url, accessToken) ?: return null
        val headers = json.optJSONObject("payload")?.optJSONArray("headers") ?: return null
        var from = ""
        var subject = ""
        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            when (header.getString("name")) {
                "From" -> from = header.getString("value")
                "Subject" -> subject = header.getString("value")
            }
        }
        val snippet = json.optString("snippet", "")
        val internalDateMillis = json.optString("internalDate").toLongOrNull() ?: System.currentTimeMillis()
        return GmailMessage(from = from, subject = subject, snippet = snippet, receivedAt = Instant.ofEpochMilli(internalDateMillis))
    }

    private fun httpGetJson(url: URL, accessToken: String): JSONObject? {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private data class GmailMessage(val from: String, val subject: String, val snippet: String, val receivedAt: Instant)
}
