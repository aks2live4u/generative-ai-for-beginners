package com.braindump.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single journal entry. Images/audio are stored as paths relative to the
 * app's private media directory (see [com.braindump.app.files.MediaStorage]).
 */
data class Thought(
    val id: Long = 0,
    val text: String,
    val mood: String? = null,
    val images: List<String> = emptyList(),
    val audioPath: String? = null,
    val audioDuration: Long? = null,
    val audioTranscript: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("mood", mood ?: JSONObject.NULL)
        put("images", JSONArray(images))
        put("audioPath", audioPath ?: JSONObject.NULL)
        put("audioDuration", audioDuration ?: JSONObject.NULL)
        put("audioTranscript", audioTranscript ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): Thought {
            val images = mutableListOf<String>()
            json.optJSONArray("images")?.let { arr ->
                for (i in 0 until arr.length()) images.add(arr.getString(i))
            }
            val now = System.currentTimeMillis()
            return Thought(
                id = json.optLong("id", 0),
                text = json.optString("text", ""),
                mood = if (json.isNull("mood")) null else json.optString("mood"),
                images = images,
                audioPath = if (json.isNull("audioPath")) null else json.optString("audioPath"),
                audioDuration = if (json.isNull("audioDuration")) null else json.optLong("audioDuration"),
                audioTranscript = if (json.isNull("audioTranscript")) null else json.optString("audioTranscript"),
                createdAt = if (json.has("createdAt")) json.optLong("createdAt", now) else now,
                updatedAt = if (json.has("updatedAt")) json.optLong("updatedAt", now) else now
            )
        }
    }
}

/** A single message in the "Chat with AI" conversation. */
data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role)
        put("content", content)
        put("createdAt", createdAt)
    }
}
