package com.braindump.app.data

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ThoughtRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    // ---------------------------------------------------------------------
    // Thoughts
    // ---------------------------------------------------------------------

    fun getThoughts(search: String?, mood: String?, limit: Int, offset: Int): List<Thought> {
        val db = dbHelper.readableDatabase
        val where = StringBuilder()
        val args = mutableListOf<String>()
        if (!search.isNullOrBlank()) {
            where.append("text LIKE ?")
            args.add("%$search%")
        }
        if (!mood.isNullOrBlank()) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("mood = ?")
            args.add(mood)
        }
        val cursor = db.query(
            "thoughts",
            null,
            if (where.isEmpty()) null else where.toString(),
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            "created_at DESC",
            "$offset,$limit"
        )
        val result = mutableListOf<Thought>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(cursorToThought(it))
            }
        }
        return result
    }

    fun getThought(id: Long): Thought? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("thoughts", null, "id = ?", arrayOf(id.toString()), null, null, null)
        cursor.use {
            if (it.moveToFirst()) return cursorToThought(it)
        }
        return null
    }

    fun insertThought(thought: Thought): Thought {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val values = thoughtToValues(thought.copy(createdAt = if (thought.createdAt > 0) thought.createdAt else now, updatedAt = now))
        val id = db.insert("thoughts", null, values)
        return getThought(id)!!
    }

    fun updateThought(id: Long, thought: Thought): Thought? {
        val db = dbHelper.writableDatabase
        val values = thoughtToValues(thought.copy(updatedAt = System.currentTimeMillis()))
        values.remove("created_at") // never overwrite the original creation time
        db.update("thoughts", values, "id = ?", arrayOf(id.toString()))
        return getThought(id)
    }

    /** Returns the list of media files (relative paths) that belonged to the deleted thought. */
    fun deleteThought(id: Long): List<String> {
        val thought = getThought(id) ?: return emptyList()
        val db = dbHelper.writableDatabase
        db.delete("thoughts", "id = ?", arrayOf(id.toString()))
        val files = mutableListOf<String>()
        files.addAll(thought.images)
        thought.audioPath?.let { files.add(it) }
        return files
    }

    fun deleteAllThoughts(): List<String> {
        val db = dbHelper.writableDatabase
        val files = mutableListOf<String>()
        val cursor = db.query("thoughts", arrayOf("images", "audio_path"), null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                val images = it.getString(0)
                JSONArray(images ?: "[]").let { arr ->
                    for (i in 0 until arr.length()) files.add(arr.getString(i))
                }
                it.getString(1)?.let { path -> files.add(path) }
            }
        }
        db.delete("thoughts", null, null)
        db.delete("chat_messages", null, null)
        return files
    }

    private fun thoughtToValues(thought: Thought): ContentValues = ContentValues().apply {
        put("text", thought.text)
        put("mood", thought.mood)
        put("images", JSONArray(thought.images).toString())
        put("audio_path", thought.audioPath)
        put("audio_duration", thought.audioDuration)
        put("audio_transcript", thought.audioTranscript)
        put("created_at", thought.createdAt)
        put("updated_at", thought.updatedAt)
    }

    private fun cursorToThought(cursor: android.database.Cursor): Thought {
        val images = mutableListOf<String>()
        val imagesJson = cursor.getString(cursor.getColumnIndexOrThrow("images"))
        if (!imagesJson.isNullOrEmpty()) {
            val arr = JSONArray(imagesJson)
            for (i in 0 until arr.length()) images.add(arr.getString(i))
        }
        fun colIndex(name: String) = cursor.getColumnIndexOrThrow(name)
        fun stringOrNull(name: String): String? {
            val idx = colIndex(name)
            return if (cursor.isNull(idx)) null else cursor.getString(idx)
        }
        fun longOrNull(name: String): Long? {
            val idx = colIndex(name)
            return if (cursor.isNull(idx)) null else cursor.getLong(idx)
        }
        return Thought(
            id = cursor.getLong(colIndex("id")),
            text = cursor.getString(colIndex("text")),
            mood = stringOrNull("mood"),
            images = images,
            audioPath = stringOrNull("audio_path"),
            audioDuration = longOrNull("audio_duration"),
            audioTranscript = stringOrNull("audio_transcript"),
            createdAt = cursor.getLong(colIndex("created_at")),
            updatedAt = cursor.getLong(colIndex("updated_at"))
        )
    }

    // ---------------------------------------------------------------------
    // Stats
    // ---------------------------------------------------------------------

    fun getStats(): JSONObject {
        val db = dbHelper.readableDatabase
        val total = db.rawQuery("SELECT COUNT(*) FROM thoughts", null).use {
            it.moveToFirst(); it.getInt(0)
        }

        val todayStart = startOfDay(0)
        val today = db.rawQuery("SELECT COUNT(*) FROM thoughts WHERE created_at >= ?", arrayOf(todayStart.toString())).use {
            it.moveToFirst(); it.getInt(0)
        }

        val weekStart = startOfWeek()
        val week = db.rawQuery("SELECT COUNT(*) FROM thoughts WHERE created_at >= ?", arrayOf(weekStart.toString())).use {
            it.moveToFirst(); it.getInt(0)
        }

        val streak = computeStreak()

        return JSONObject().apply {
            put("total", total)
            put("today", today)
            put("week", week)
            put("streak", streak)
        }
    }

    private fun computeStreak(): Int {
        val db = dbHelper.readableDatabase
        var dayStart = startOfDay(0)
        val hasToday = db.rawQuery(
            "SELECT COUNT(*) FROM thoughts WHERE created_at >= ? AND created_at < ?",
            arrayOf(dayStart.toString(), (dayStart + DAY_MS).toString())
        ).use { it.moveToFirst(); it.getInt(0) > 0 }

        if (!hasToday) {
            dayStart -= DAY_MS
        }

        var streak = 0
        while (true) {
            val count = db.rawQuery(
                "SELECT COUNT(*) FROM thoughts WHERE created_at >= ? AND created_at < ?",
                arrayOf(dayStart.toString(), (dayStart + DAY_MS).toString())
            ).use { it.moveToFirst(); it.getInt(0) }
            if (count <= 0) break
            streak++
            dayStart -= DAY_MS
        }
        return streak
    }

    fun getMoodStats(): JSONObject {
        val db = dbHelper.readableDatabase

        val allTime = JSONObject()
        db.rawQuery("SELECT mood, COUNT(*) FROM thoughts WHERE mood IS NOT NULL GROUP BY mood", null).use {
            while (it.moveToNext()) allTime.put(it.getString(0), it.getInt(1))
        }

        val recentCutoff = startOfDay(-29)
        val recent = JSONObject()
        db.rawQuery(
            "SELECT mood, COUNT(*) FROM thoughts WHERE mood IS NOT NULL AND created_at >= ? GROUP BY mood",
            arrayOf(recentCutoff.toString())
        ).use {
            while (it.moveToNext()) recent.put(it.getString(0), it.getInt(1))
        }

        // Last 7 days, one entry per day with the moods logged that day.
        val daily = JSONArray()
        for (offset in -6..0) {
            val start = startOfDay(offset)
            val end = start + DAY_MS
            val dayMoods = JSONObject()
            db.rawQuery(
                "SELECT mood, COUNT(*) FROM thoughts WHERE mood IS NOT NULL AND created_at >= ? AND created_at < ? GROUP BY mood",
                arrayOf(start.toString(), end.toString())
            ).use {
                while (it.moveToNext()) dayMoods.put(it.getString(0), it.getInt(1))
            }
            daily.put(JSONObject().apply {
                put("date", start)
                put("moods", dayMoods)
            })
        }

        return JSONObject().apply {
            put("allTime", allTime)
            put("recent", recent)
            put("daily", daily)
        }
    }

    private fun startOfDay(dayOffset: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    // ---------------------------------------------------------------------
    // AI chat history
    // ---------------------------------------------------------------------

    fun getChatMessages(): List<ChatMessage> {
        val db = dbHelper.readableDatabase
        val cursor = db.query("chat_messages", null, null, null, null, null, "created_at ASC")
        val result = mutableListOf<ChatMessage>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    ChatMessage(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        role = it.getString(it.getColumnIndexOrThrow("role")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return result
    }

    fun addChatMessage(role: String, content: String): ChatMessage {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("role", role)
            put("content", content)
            put("created_at", now)
        }
        val id = db.insert("chat_messages", null, values)
        return ChatMessage(id, role, content, now)
    }

    fun clearChatMessages() {
        dbHelper.writableDatabase.delete("chat_messages", null, null)
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
