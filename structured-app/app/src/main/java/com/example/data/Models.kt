package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_tasks")
data class TimelineTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timeSlotStart: String, // e.g. "09:00"
    val timeSlotEnd: String,   // e.g. "10:00"
    val energyLevel: String = "Medium", // "Low", "Medium", "High"
    val isCompleted: Boolean = false,
    val category: String = "Routine", // "Routine", "Deep Work", "Urgent", "Quick Win", "Break"
    val emoji: String = "📌",
    val substepsJson: String? = null, // JSON list of Substep objects
    val durationMinutes: Int = 30,
    val isAutomatic: Boolean = false,
    val dayDate: String, // YYYY-MM-DD
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 15
) {
    companion object {
        fun createDefaultSubsteps(title: String): String {
            return when {
                title.lowercase().contains("clean") -> 
                    "[\"Pick up clothes off floor\", \"Make the bed\", \"Wipe down major surfaces\", \"Take out the garbage\"]"
                title.lowercase().contains("study") || title.lowercase().contains("work") -> 
                    "[\"Open your textbook/IDE\", \"Write down 1 small goal\", \"Set a 15-minute timer\", \"Close distracting tabs\"]"
                title.lowercase().contains("exercise") || title.lowercase().contains("gym") -> 
                    "[\"Put on your shoes\", \"Fill a water bottle\", \"Do a gentle 2-minute stretch\", \"Just walk out the door\"]"
                title.lowercase().contains("cook") || title.lowercase().contains("eat") -> 
                    "[\"Decide on 1 simple thing to eat\", \"Take out ingredients\", \"Prep the dishes/pot\", \"Turn on the heat\"]"
                else -> 
                    "[\"Break task into 3 small steps\", \"Do the first step for just 1 minute\", \"Tick off when done\"]"
            }
        }
    }
}

data class Substep(
    val text: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "brain_dumps")
data class BrainDump(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val processedStatus: String = "Inbox", // "Inbox", "Converted", "Trash"
    val tag: String? = null,
    val photoPath: String? = null
)

@Entity(tableName = "memory_vault")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val details: String,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val frequency: String = "Daily", // "Daily", "Flexible"
    val targetDaysPerWeek: Int = 3,
    val streak: Int = 0,
    val lastCompletedDate: String? = null, // YYYY-MM-DD
    val category: String = "Self-care",
    val emoji: String = "🌱"
)

@Entity(tableName = "mood_checkins")
data class MoodCheckin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mood: String, // "Overwhelmed", "Anxious", "Tired", "Motivated", "Frozen", "Distracted"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Entity(tableName = "time_blocks")
data class TimeBlock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val emoji: String = "🔒"
)
