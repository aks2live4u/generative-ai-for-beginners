package com.example.data

import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class Repository(private val db: AppDatabase) {

    val timelineTaskDao = db.timelineTaskDao()
    val brainDumpDao = db.brainDumpDao()
    val memoryItemDao = db.memoryItemDao()
    val habitDao = db.habitDao()
    val moodCheckinDao = db.moodCheckinDao()
    val timeBlockDao = db.timeBlockDao()

    suspend fun clearAllData() {
        db.clearAllTables()
    }

    // --- Timeline Tasks ---
    fun getTasksForDay(day: String): Flow<List<TimelineTask>> = timelineTaskDao.getTasksForDay(day)
    fun getAllTasks(): Flow<List<TimelineTask>> = timelineTaskDao.getAllTasks()
    suspend fun insertTask(task: TimelineTask) = timelineTaskDao.insertTask(task)
    suspend fun updateTask(task: TimelineTask) = timelineTaskDao.updateTask(task)
    suspend fun deleteTask(task: TimelineTask) = timelineTaskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = timelineTaskDao.deleteTaskById(id)

    // --- Brain Dumps ---
    fun getActiveBrainDumps(): Flow<List<BrainDump>> = brainDumpDao.getActiveBrainDumps()
    suspend fun insertBrainDump(dump: BrainDump) = brainDumpDao.insertBrainDump(dump)
    suspend fun updateBrainDump(dump: BrainDump) = brainDumpDao.updateBrainDump(dump)
    suspend fun deleteBrainDump(dump: BrainDump) = brainDumpDao.deleteBrainDump(dump)
    suspend fun deleteBrainDumpById(id: Int) = brainDumpDao.deleteBrainDumpById(id)

    // --- Memory Vault ---
    fun getAllMemories(): Flow<List<MemoryItem>> = memoryItemDao.getAllMemories()
    fun searchMemories(query: String): Flow<List<MemoryItem>> = memoryItemDao.searchMemories(query)
    suspend fun insertMemory(item: MemoryItem) = memoryItemDao.insertMemory(item)
    suspend fun deleteMemory(item: MemoryItem) = memoryItemDao.deleteMemory(item)

    // --- Habits ---
    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    // --- Mood Checkins ---
    fun getAllCheckins(): Flow<List<MoodCheckin>> = moodCheckinDao.getAllCheckins()
    fun getRecentCheckins(): Flow<List<MoodCheckin>> = moodCheckinDao.getRecentCheckins()
    suspend fun insertCheckin(checkin: MoodCheckin) = moodCheckinDao.insertCheckin(checkin)

    // --- Time Blocks ---
    fun getAllTimeBlocks(): Flow<List<TimeBlock>> = timeBlockDao.getAllTimeBlocks()
    suspend fun insertTimeBlock(block: TimeBlock) = timeBlockDao.insertTimeBlock(block)
    suspend fun deleteTimeBlock(block: TimeBlock) = timeBlockDao.deleteTimeBlock(block)

    // --- Gemini AI Assistant Integration ---

    /**
     * Breakdown Engine: Breaks an overwhelming task into 4-6 micros-steps.
     */
    suspend fun breakDownTask(taskTitle: String, mode: String = "standard"): List<String> {
        val systemPrompt = """
            You are a compassionate, ADHD-friendly execution assistant. You understand that people with ADHD struggle with task initiation, but have high intelligence. Your goal is to break a large, intimidating task into 4 to 6 micro-steps that are absurdly tiny, direct, and completely free of toxic motivational speak or corporate filler. 
            Modes:
            - "standard": Balanced small steps.
            - "five_minute": Steps scaled down so the entire task feels like it could be done in 5 minutes.
            - "bare_minimum": The absolute lowest bar, focusing heavily on comforting the user that they do not have to finish it, and just starting is a huge win.
            
            Return ONLY a raw JSON array of strings. Do NOT wrap it in markdown block tags of any kind. No comments, no formatting.
            Example format: ["Take a deep breath and sit up", "Pick up exactly ONE t-shirt from the floor", "Put that t-shirt in the wash basket", "Praise yourself for this win"]
        """.trimIndent()

        val prompt = when (mode) {
            "five_minute" -> "Provide a ultra-fast, 5-minute microscopic breakdown for: '$taskTitle'. Make the steps small and fast."
            "bare_minimum" -> "Provide an 'Emergency Bare Minimum' list of 3-4 steps to initiate '$taskTitle', including reassuring the user they can stop after step 1."
            else -> "Break down the task: '$taskTitle' into micro-steps."
        }

        val response = GeminiApiClient.generateText(prompt, systemPrompt)
        
        if (response == "API_KEY_MISSING") {
            // Return fallback offline generated steps
            val localResult = TimelineTask.createDefaultSubsteps(taskTitle)
            return parseJsonArray(localResult)
        }

        return try {
            // Clean markdown tags if model outputs them
            val cleaned = response.replace("```json", "").replace("```", "").trim()
            parseJsonArray(cleaned)
        } catch (e: Exception) {
            parseJsonArray(TimelineTask.createDefaultSubsteps(taskTitle))
        }
    }

    /**
     * Inbox Analyzer: Categorizes a chaotic brain dump piece.
     */
    suspend fun categorizeBrainDump(rawText: String): BrainDumpCategoryResult {
        val systemPrompt = """
            You are a supportive ADHD memory-dump assistant. Your job is to read a chaotic mental dump and synthesize it into structure:
            1. Suggest a clean, short title for the item.
            2. Classify it into one of: "Task", "Idea", "Shopping List", "Reminder", "Important Memory".
            3. Synthesize a simplified, actionable note or description.
            4. Suggest a suitable Emoji.
            
            Return output as a JSON object with these keys:
            - title (String)
            - category (String)
            - content (String)
            - emoji (String)
            
            Return ONLY raw JSON. No markdown code blocks, no comments.
        """.trimIndent()

        val response = GeminiApiClient.generateText(rawText, systemPrompt)
        
        if (response == "API_KEY_MISSING") {
            return BrainDumpCategoryResult(
                title = rawText.take(20).plus("..."),
                category = "Task",
                content = rawText,
                emoji = "📝"
            )
        }

        return try {
            val cleaned = response.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleaned)
            BrainDumpCategoryResult(
                title = json.optString("title", "Brain Dump Item"),
                category = json.optString("category", "Task"),
                content = json.optString("content", rawText),
                emoji = json.optString("emoji", "🎯")
            )
        } catch (e: Exception) {
            BrainDumpCategoryResult(
                title = rawText.take(20).plus("..."),
                category = "Task",
                content = rawText,
                emoji = "📝"
            )
        }
    }

    /**
     * Burnout/Overplanning Detector: Warns users gently if scheduled tasks exceed safe capacity.
     */
    suspend fun evaluateScheduleWarning(tasksCount: Int, totalDurationMs: Long): ScheduleCheckResult {
        val totalHours = totalDurationMs.toDouble() / (1000 * 60 * 60)
        
        if (tasksCount > 12 || totalHours > 10.0) {
            return ScheduleCheckResult(
                isOverloaded = true,
                warningMessage = "Hey friend, you've planned $tasksCount tasks (${String.format("%.1f", totalHours)} hrs) today! ADHD brains thrive on empty buffer space. How about we skip or schedule some of these for later?",
                suggestedAction = "Simplify today"
            )
        }
        return ScheduleCheckResult(
            isOverloaded = false,
            warningMessage = "Your schedule looks beautifully spacious. Remember to pause, take deep breaths, and drink water. You've got this!",
            suggestedAction = ""
        )
    }

    private fun parseJsonArray(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // Fallback manual parse
            val cleaned = jsonStr.replace("[", "").replace("]", "").split(",")
            for (item in cleaned) {
                val s = item.trim().removeSurrounding("\"").removeSurrounding("'")
                if (s.isNotEmpty()) list.add(s)
            }
        }
        return if (list.isEmpty()) listOf("Take a deep breath", "Open the task", "Do 1 minute of activity") else list
    }
}

data class BrainDumpCategoryResult(
    val title: String,
    val category: String,
    val content: String,
    val emoji: String
)

data class ScheduleCheckResult(
    val isOverloaded: Boolean,
    val warningMessage: String,
    val suggestedAction: String
)
