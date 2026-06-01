package com.example.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReminderReceiver
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application, private val repository: Repository) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("structured_prefs", Context.MODE_PRIVATE)

    // --- Core UI State Toggles ---
    val isEmergencyMode = MutableStateFlow(prefs.getBoolean("is_emergency_mode", false))
    val isDyslexiaFontApplied = MutableStateFlow(prefs.getBoolean("is_dyslexia_font_applied", false))
    val currentSelectedDate = MutableStateFlow(getTodayDateString())

    // --- Visual Theme & Customization Choice ---
    val appThemeOption = MutableStateFlow(prefs.getString("app_theme_option", "System") ?: "System") // "System", "Cosmos Dark", "Sand Light"
    val selectedTextColorHex = MutableStateFlow(prefs.getString("selected_text_color_hex", "#FFFFFF") ?: "#FFFFFF") // #FFFFFF, #38BDF8, #34D399, #FBBF24, #C084FC, #F43F5E
    val appIconPresetColor = MutableStateFlow(prefs.getString("app_icon_preset_color", "Theme Match") ?: "Theme Match") // "Theme Match", "Neon Cyan", "Golden Hour", "Lilac Bloom", "Electric Rose"

    // --- Custom ADHD Personal Profile Hub details ---
    val profileName = MutableStateFlow(prefs.getString("profile_name", "Alex Sinclair") ?: "Alex Sinclair")
    val profileFocusStyle = MutableStateFlow(prefs.getString("profile_focus_style", "ADHD Explorer - Burst Variety") ?: "ADHD Explorer - Burst Variety")
    val profileDailyEnergyGoal = MutableStateFlow(prefs.getString("profile_daily_energy_goal", "3 Major Task Blocks Comfortably") ?: "3 Major Task Blocks Comfortably")

    fun updateProfile(name: String, focusStyle: String, energyGoal: String) {
        profileName.value = name
        profileFocusStyle.value = focusStyle
        profileDailyEnergyGoal.value = energyGoal
    }

    fun resetAllAppData() {
        viewModelScope.launch {
            repository.clearAllData()
            prefs.edit().putBoolean("has_seeded", false).apply()
            seedInitialSampleData()
        }
    }

    // --- State: Tasks ---
    val tasksForToday: StateFlow<List<TimelineTask>> = currentSelectedDate
        .flatMapLatest { date -> repository.getTasksForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Brain Dumps ---
    val activeBrainDumps: StateFlow<List<BrainDump>> = repository.getActiveBrainDumps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Memories ---
    val searchMemoryQuery = MutableStateFlow("")
    val memoryVaultItems: StateFlow<List<MemoryItem>> = searchMemoryQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllMemories()
            } else {
                repository.searchMemories(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Habits ---
    val habits: StateFlow<List<Habit>> = repository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Mood & Checkins ---
    val moodCheckins: StateFlow<List<MoodCheckin>> = repository.getAllCheckins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State: Time Blocks ---
    val timeBlocks: StateFlow<List<TimeBlock>> = repository.getAllTimeBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMood = MutableStateFlow("Motivated") // Default comfortable level

    // --- State: Overload Warning ---
    val scheduleWarning = MutableStateFlow<ScheduleCheckResult?>(null)

    // --- State: Interactive Task Breakdown ---
    val isLoadingBreakdown = MutableStateFlow(false)
    val currentBreakdownTarget = MutableStateFlow<TimelineTask?>(null)
    val activeBreakdownSteps = MutableStateFlow<List<String>>(emptyList())

    // --- State: Focus Mode / Doubling Room ---
    val focusTimerSecondsLeft = MutableStateFlow(25 * 60) // 25 Min
    val focusTimerIsActive = MutableStateFlow(false)
    val focusTimerMinutes = MutableStateFlow(25)
    val breakTimerMinutes = MutableStateFlow(5)
    val isBreakMode = MutableStateFlow(false)
    val ambientSoundSelected = MutableStateFlow("Campfire Rain") // "Silent", "Lo-Fi Beats", "Campfire Rain", "Brown Noise"
    val distractionNotes = MutableStateFlow("")
    val isCoworkingActive = MutableStateFlow(false)
    val coworkingParticipants = MutableStateFlow(5)
    
    // Auto cycle loop options
    val focusMaxCycles = MutableStateFlow(2) // 1, 2, 3, or 4
    val focusCyclesRemaining = MutableStateFlow(2)

    // --- Dynamic Week Offset Navigation ---
    val weekOffsetDays = MutableStateFlow(0)

    fun shiftWeek(days: Int) {
        weekOffsetDays.value += days
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(currentSelectedDate.value)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                cal.add(Calendar.DAY_OF_YEAR, days)
                currentSelectedDate.value = sdf.format(cal.time)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetWeekOffset() {
        weekOffsetDays.value = 0
        currentSelectedDate.value = getTodayDateString()
    }
    
    fun setMaxCycles(cycles: Int) {
        focusMaxCycles.value = cycles.coerceIn(1, 4)
        if (!focusTimerIsActive.value) {
            focusCyclesRemaining.value = focusMaxCycles.value
        }
    }

    // --- Dopamine / Reward Analytics ---
    val focusTimerSuccessCount = MutableStateFlow(0)
    val dopamineStreak: StateFlow<Int> = repository.getAllTasks()
        .map { allTasks ->
            val tasksByDay = allTasks.groupBy { it.dayDate }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            
            // Get all dates in the past and today that have tasks
            val pastAndTodayDates = tasksByDay.keys
                .filter { it.isNotBlank() && it <= todayStr } // Only today and past!
                .sortedDescending() // newest to oldest
            
            var streak = 0
            for (dateStr in pastAndTodayDates) {
                val dayTasks = tasksByDay[dateStr] ?: emptyList()
                if (dayTasks.isEmpty()) continue
                
                val allCompleted = dayTasks.all { it.isCompleted }
                
                if (dateStr == todayStr) {
                    if (allCompleted) {
                        streak++
                    } else {
                        // Today is incomplete, but don't break the streak yet as today is still active!
                    }
                } else {
                    if (allCompleted) {
                        streak++
                    } else {
                        // A past day has incomplete tasks! This breaks the streak.
                        break
                    }
                }
            }
            
            streak
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val momentumLevel = MutableStateFlow(0f)  // Progress from 0 to 100 representing completed items

    init {
        // Automatically check schedule warnings when tasks change
        viewModelScope.launch {
            tasksForToday.collect { list ->
                // Calculate total scheduled duration
                val count = list.size
                val totalDurationMinutes = list.sumOf { it.durationMinutes }
                val check = repository.evaluateScheduleWarning(count, totalDurationMinutes.toLong() * 60 * 1000)
                scheduleWarning.value = check

                // Re-calculate momentum completion level
                if (list.isNotEmpty()) {
                    val completed = list.count { it.isCompleted }.toFloat()
                    momentumLevel.value = (completed / list.size) * 100f
                } else {
                    momentumLevel.value = 0f
                }
            }
        }

        // Persist preferences automatically when changed
        viewModelScope.launch {
            isEmergencyMode.collect { value ->
                prefs.edit().putBoolean("is_emergency_mode", value).apply()
            }
        }
        viewModelScope.launch {
            isDyslexiaFontApplied.collect { value ->
                prefs.edit().putBoolean("is_dyslexia_font_applied", value).apply()
            }
        }
        viewModelScope.launch {
            appThemeOption.collect { value ->
                prefs.edit().putString("app_theme_option", value).apply()
            }
        }
        viewModelScope.launch {
            selectedTextColorHex.collect { value ->
                prefs.edit().putString("selected_text_color_hex", value).apply()
            }
        }
        viewModelScope.launch {
            appIconPresetColor.collect { value ->
                prefs.edit().putString("app_icon_preset_color", value).apply()
            }
        }
        viewModelScope.launch {
            profileName.collect { value ->
                prefs.edit().putString("profile_name", value).apply()
            }
        }
        viewModelScope.launch {
            profileFocusStyle.collect { value ->
                prefs.edit().putString("profile_focus_style", value).apply()
            }
        }
        viewModelScope.launch {
            profileDailyEnergyGoal.collect { value ->
                prefs.edit().putString("profile_daily_energy_goal", value).apply()
            }
        }

        // Seed some starter habits & tasks if the DB is completely empty!
        seedInitialSampleData()
    }

    // --- Day Helpers ---
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // --- Automatic Classification and Formatting Helpers ---
    fun classifyTaskTitle(title: String): Pair<String, String> {
        val lower = title.lowercase()
        return when {
            // Study / Work
            lower.contains("study") || lower.contains("exam") || lower.contains("homework") || lower.contains("lecture") || lower.contains("class") -> "Study" to "📚"
            lower.contains("code") || lower.contains("program") || lower.contains("develop") || lower.contains("bug") || lower.contains("ide") || lower.contains("github") -> "Work" to "💻"
            lower.contains("work") || lower.contains("meeting") || lower.contains("presentation") || lower.contains("report") || lower.contains("office") || lower.contains("email") -> "Work" to "💼"
            
            // Health / Exercise
            lower.contains("gym") || lower.contains("workout") || lower.contains("run") || lower.contains("walk") || lower.contains("exercise") || lower.contains("fit") || lower.contains("yoga") -> "Health" to "🏋️"
            lower.contains("doctor") || lower.contains("dentist") || lower.contains("clinic") || lower.contains("meds") || lower.contains("medicine") || lower.contains("pill") -> "Health" to "💊"
            
            // Chores / House
            lower.contains("clean") || lower.contains("wash") || lower.contains("laundry") || lower.contains("vacuum") || lower.contains("sweep") || lower.contains("dishes") -> "Chores" to "🧹"
            lower.contains("cook") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("breakfast") || lower.contains("meal") || lower.contains("grocery") || lower.contains("shop") -> "Chores" to "🍳"
            
            // Social / Fun / Leisure
            lower.contains("friend") || lower.contains("party") || lower.contains("hangout") || lower.contains("game") || lower.contains("play") || lower.contains("movie") || lower.contains("cinema") -> "Leisure" to "🎉"
            lower.contains("relax") || lower.contains("sleep") || lower.contains("nap") || lower.contains("read") || lower.contains("book") || lower.contains("meditate") || lower.contains("zen") -> "Leisure" to "🧘"
            
            // Defaults
            else -> "Personal" to "📌"
        }
    }

    fun parseEasyTimeInput(rawInput: String): String {
        val clean = rawInput.replace(":", "").trim()
        if (clean.isBlank()) return "09:00"
        
        val numbersOnly = clean.filter { it.isDigit() }
        if (numbersOnly.isEmpty()) return "09:00"
        
        return when (numbersOnly.length) {
            1 -> {
                val hour = numbersOnly.toInt()
                String.format(Locale.getDefault(), "%02d:00", hour.coerceIn(0, 23))
            }
            2 -> {
                val hour = numbersOnly.toInt()
                String.format(Locale.getDefault(), "%02d:00", hour.coerceIn(0, 23))
            }
            3 -> {
                val hour = numbersOnly.substring(0, 1).toInt()
                val min = numbersOnly.substring(1, 3).toInt()
                String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), min.coerceIn(0, 59))
            }
            4 -> {
                val hour = numbersOnly.substring(0, 2).toInt()
                val min = numbersOnly.substring(2, 4).toInt()
                String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), min.coerceIn(0, 59))
            }
            else -> {
                val truncated = numbersOnly.take(4)
                if (truncated.length == 4) {
                    val hour = truncated.substring(0, 2).toInt()
                    val min = truncated.substring(2, 4).toInt()
                    String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), min.coerceIn(0, 59))
                } else {
                    "09:00"
                }
            }
        }
    }

    fun calculateDurationMinutes(start: String, end: String): Int {
        try {
            val startParts = start.split(":")
            val endParts = end.split(":")
            if (startParts.size < 2 || endParts.size < 2) return 30
            val startH = startParts[0].trim().toIntOrNull() ?: 9
            val startM = startParts[1].trim().take(2).toIntOrNull() ?: 0
            val endH = endParts[0].trim().toIntOrNull() ?: 10
            val endM = endParts[1].trim().take(2).toIntOrNull() ?: 0
            
            var diff = (endH * 60 + endM) - (startH * 60 + startM)
            if (diff < 0) diff += 24 * 60
            return diff
        } catch (e: Exception) {
            return 30
        }
    }

    // --- Task Actions ---
    fun addTask(
        title: String,
        startTime: String,
        endTime: String,
        energy: String,
        category: String,
        emoji: String,
        hasReminder: Boolean = false,
        reminderMinutesBefore: Int = 15,
        duration: Int = 30
    ) {
        viewModelScope.launch {
            val (autoCategory, autoEmoji) = classifyTaskTitle(title)
            val task = TimelineTask(
                title = title,
                timeSlotStart = parseEasyTimeInput(startTime),
                timeSlotEnd = parseEasyTimeInput(endTime),
                energyLevel = energy,
                category = autoCategory,
                emoji = autoEmoji,
                durationMinutes = duration,
                dayDate = currentSelectedDate.value,
                hasReminder = hasReminder,
                reminderMinutesBefore = reminderMinutesBefore
            )
            val newId = repository.insertTask(task)
            scheduleTaskReminder(task.copy(id = newId.toInt()))
        }
    }

    fun updateTaskDetails(
        task: TimelineTask,
        title: String,
        startTime: String,
        endTime: String,
        energy: String,
        hasReminder: Boolean,
        reminderMinutesBefore: Int = 15,
        category: String,
        emoji: String,
        dayDate: String
    ) {
        viewModelScope.launch {
            val parsedStart = parseEasyTimeInput(startTime)
            val parsedEnd = parseEasyTimeInput(endTime)
            val updated = task.copy(
                title = title,
                timeSlotStart = parsedStart,
                timeSlotEnd = parsedEnd,
                energyLevel = energy,
                category = category,
                emoji = emoji,
                dayDate = dayDate,
                hasReminder = hasReminder,
                reminderMinutesBefore = reminderMinutesBefore
            )
            repository.updateTask(updated)
            scheduleTaskReminder(updated)
        }
    }

    fun updateTaskWithRepeat(
        task: TimelineTask,
        title: String,
        startTime: String,
        endTime: String,
        energy: String,
        hasReminder: Boolean,
        reminderMinutesBefore: Int = 15,
        repeatType: String,
        selectedDays: List<Int>,
        updateAllFuture: Boolean = false,
        updateType: String? = null,
        newDayDate: String? = null,
        details: String? = null
    ) {
        viewModelScope.launch {
            val type = updateType ?: if (updateAllFuture) "now_and_future" else "just_this"
            val parsedStart = parseEasyTimeInput(startTime)
            val parsedEnd = parseEasyTimeInput(endTime)
            val (autoCategory, autoEmoji) = classifyTaskTitle(title)

            val durationMin = calculateDurationMinutes(parsedStart, parsedEnd)
            val targetDate = newDayDate ?: task.dayDate

            val allTasks = repository.getAllTasks().first()
            when (type) {
                "now_and_future" -> {
                    allTasks.forEach { otherTask ->
                        if (otherTask.title.contentEquals(task.title, ignoreCase = true) && otherTask.dayDate >= task.dayDate) {
                            val isTarget = otherTask.id == task.id
                            val updated = otherTask.copy(
                                title = title,
                                timeSlotStart = parsedStart,
                                timeSlotEnd = parsedEnd,
                                energyLevel = energy,
                                category = autoCategory,
                                emoji = autoEmoji,
                                durationMinutes = durationMin,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore,
                                substepsJson = details ?: otherTask.substepsJson,
                                dayDate = if (isTarget) targetDate else otherTask.dayDate
                            )
                            repository.updateTask(updated)
                            scheduleTaskReminder(updated)
                        }
                    }
                }
                "entire_series" -> {
                    allTasks.forEach { otherTask ->
                        if (otherTask.title.contentEquals(task.title, ignoreCase = true)) {
                            val isTarget = otherTask.id == task.id
                            val updated = otherTask.copy(
                                title = title,
                                timeSlotStart = parsedStart,
                                timeSlotEnd = parsedEnd,
                                energyLevel = energy,
                                category = autoCategory,
                                emoji = autoEmoji,
                                durationMinutes = durationMin,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore,
                                substepsJson = details ?: otherTask.substepsJson,
                                dayDate = if (isTarget) targetDate else otherTask.dayDate
                            )
                            repository.updateTask(updated)
                            scheduleTaskReminder(updated)
                        }
                    }
                }
                else -> { // "just_this"
                    // Update the single edited task first, keeping its original baseDate
                    val updated = task.copy(
                        title = title,
                        timeSlotStart = parsedStart,
                        timeSlotEnd = parsedEnd,
                        energyLevel = energy,
                        category = autoCategory,
                        emoji = autoEmoji,
                        durationMinutes = durationMin,
                        hasReminder = hasReminder,
                        reminderMinutesBefore = reminderMinutesBefore,
                        substepsJson = details,
                        dayDate = targetDate
                    )
                    repository.updateTask(updated)
                    scheduleTaskReminder(updated)

                    // If repeating options were requested, generate the series
                    if (repeatType != "Just Today") {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val baseDate = sdf.parse(targetDate) ?: Date()

                        val datesToInsert = mutableListOf<String>()
                        when (repeatType) {
                            "Daily" -> {
                                val cal = Calendar.getInstance()
                                cal.time = baseDate
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                for (i in 0 until 364) {
                                    datesToInsert.add(sdf.format(cal.time))
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            "Weekdays" -> {
                                val cal = Calendar.getInstance()
                                cal.time = baseDate
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                for (i in 0 until 364) {
                                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                                    if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                                        datesToInsert.add(sdf.format(cal.time))
                                    }
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            "Specific Days" -> {
                                val cal = Calendar.getInstance()
                                cal.time = baseDate
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                for (i in 0 until 364) {
                                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                                    if (selectedDays.contains(dayOfWeek)) {
                                        datesToInsert.add(sdf.format(cal.time))
                                    }
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                        }

                        datesToInsert.forEach { dateStr ->
                            val repeatTask = TimelineTask(
                                title = title,
                                timeSlotStart = parsedStart,
                                timeSlotEnd = parsedEnd,
                                energyLevel = energy,
                                category = autoCategory,
                                emoji = autoEmoji,
                                durationMinutes = durationMin,
                                dayDate = dateStr,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore,
                                substepsJson = details
                            )
                            val newId = repository.insertTask(repeatTask)
                            scheduleTaskReminder(repeatTask.copy(id = newId.toInt()))
                        }
                    }
                }
            }
        }
    }

    fun addTaskWithRepeat(
        title: String,
        startTime: String,
        endTime: String,
        energy: String,
        hasReminder: Boolean,
        reminderMinutesBefore: Int = 15,
        repeatType: String, // "Just Today", "Daily", "Weekdays", "Specific Days"
        selectedDays: List<Int> = emptyList(), // 1=Sunday, 2=Monday, ...
        dayDate: String? = null,
        details: String? = null
    ) {
        viewModelScope.launch {
            val parsedStart = parseEasyTimeInput(startTime)
            val parsedEnd = parseEasyTimeInput(endTime)
            
            // Automatically sort and classify category + emoji
            val (autoCategory, autoEmoji) = classifyTaskTitle(title)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val baseDateStr = if (!dayDate.isNullOrBlank()) dayDate else currentSelectedDate.value
            val baseDate = sdf.parse(baseDateStr) ?: Date()
            
            val datesToInsert = mutableListOf<String>()
            
            when (repeatType) {
                "Just Today" -> {
                    datesToInsert.add(sdf.format(baseDate))
                }
                "Daily" -> {
                    val cal = Calendar.getInstance()
                    cal.time = baseDate
                    for (i in 0 until 365) {
                        datesToInsert.add(sdf.format(cal.time))
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                "Weekdays" -> {
                    val cal = Calendar.getInstance()
                    cal.time = baseDate
                    for (i in 0 until 365) {
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                            datesToInsert.add(sdf.format(cal.time))
                        }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                "Specific Days" -> {
                    val cal = Calendar.getInstance()
                    cal.time = baseDate
                    for (i in 0 until 365) {
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        if (selectedDays.contains(dayOfWeek)) {
                            datesToInsert.add(sdf.format(cal.time))
                        }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
            
            val durationMin = calculateDurationMinutes(parsedStart, parsedEnd)
            
            datesToInsert.forEach { dateStr ->
                val task = TimelineTask(
                    title = title,
                    timeSlotStart = parsedStart,
                    timeSlotEnd = parsedEnd,
                    energyLevel = energy,
                    category = autoCategory,
                    emoji = autoEmoji,
                    durationMinutes = durationMin,
                    dayDate = dateStr,
                    hasReminder = hasReminder,
                    reminderMinutesBefore = reminderMinutesBefore,
                    substepsJson = details
                )
                val newId = repository.insertTask(task)
                scheduleTaskReminder(task.copy(id = newId.toInt()))
            }
        }
    }

    fun toggleTaskCompletion(task: TimelineTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            
            // Give a generous, non-shaming dopamine boost!
            if (updated.isCompleted) {
                cancelTaskReminder(updated)
                momentumLevel.value = (momentumLevel.value + 15f).coerceAtMost(100f)
            } else {
                scheduleTaskReminder(updated)
            }
        }
    }

    fun deleteTask(task: TimelineTask, deleteAllFuture: Boolean = false, deleteType: String? = null) {
        viewModelScope.launch {
            val type = deleteType ?: if (deleteAllFuture) "now_and_future" else "just_this"
            val allTasks = repository.getAllTasks().first()
            when (type) {
                "just_this" -> {
                    repository.deleteTask(task)
                    cancelTaskReminder(task)
                }
                "now_and_future" -> {
                    allTasks.forEach { otherTask ->
                        if (otherTask.title.contentEquals(task.title, ignoreCase = true) && otherTask.dayDate >= task.dayDate) {
                            repository.deleteTask(otherTask)
                            cancelTaskReminder(otherTask)
                        }
                    }
                }
                "entire_series" -> {
                    allTasks.forEach { otherTask ->
                        if (otherTask.title.contentEquals(task.title, ignoreCase = true)) {
                            repository.deleteTask(otherTask)
                            cancelTaskReminder(otherTask)
                        }
                    }
                }
            }
        }
    }

    fun generateAITaskBreakdown(task: TimelineTask, mode: String = "standard") {
        currentBreakdownTarget.value = task
        isLoadingBreakdown.value = true
        viewModelScope.launch {
            val steps = repository.breakDownTask(task.title, mode)
            activeBreakdownSteps.value = steps
            isLoadingBreakdown.value = false
        }
    }

    fun applyBreakdownToSubsteps(task: TimelineTask, steps: List<String>) {
        viewModelScope.launch {
            // Convert list of strings to JSON list
            val jsonArray = org.json.JSONArray()
            steps.forEach { jsonArray.put(it) }
            val updated = task.copy(substepsJson = jsonArray.toString())
            repository.updateTask(updated)
            // Reset targets
            currentBreakdownTarget.value = null
            activeBreakdownSteps.value = emptyList()
        }
    }

    // --- Brain Dump Actions ---
    fun registerBrainDump(rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            val dump = BrainDump(rawText = rawText)
            repository.insertBrainDump(dump)
        }
    }

    fun convertBrainDumpToTask(dump: BrainDump) {
        viewModelScope.launch {
            // Initiate AI parsing
            val result = repository.categorizeBrainDump(dump.rawText)
            
            // Insert structured task
            val task = TimelineTask(
                title = result.title,
                timeSlotStart = "12:00",
                timeSlotEnd = "12:30",
                energyLevel = "Medium",
                category = result.category,
                emoji = result.emoji,
                dayDate = currentSelectedDate.value
            )
            repository.insertTask(task)
            
            // Set dump as converted (Trash/Done)
            repository.updateBrainDump(dump.copy(processedStatus = "Converted"))
        }
    }

    fun convertBrainDumpToMemory(dump: BrainDump) {
        viewModelScope.launch {
            // Integrate into Memory vault
            val result = repository.categorizeBrainDump(dump.rawText)
            val memory = MemoryItem(
                title = result.title,
                details = result.content
            )
            repository.insertMemory(memory)
            repository.updateBrainDump(dump.copy(processedStatus = "Converted"))
        }
    }

    fun discardBrainDump(dump: BrainDump) {
        viewModelScope.launch {
            repository.updateBrainDump(dump.copy(processedStatus = "Trash"))
        }
    }

    // --- Memory Actions ---
    fun addMemory(title: String, details: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val item = MemoryItem(title = title, details = details)
            repository.insertMemory(item)
        }
    }

    fun deleteMemory(item: MemoryItem) {
        viewModelScope.launch {
            repository.deleteMemory(item)
        }
    }

    // --- Habit Actions ---
    fun addHabit(title: String, frequency: String, targetDays: Int, emoji: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val habit = Habit(
                title = title,
                frequency = frequency,
                targetDaysPerWeek = targetDays,
                emoji = emoji
            )
            repository.insertHabit(habit)
        }
    }

    fun toggleHabitCompletion(habit: Habit) {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val completedToday = habit.lastCompletedDate == todayStr
            
            val updated = if (completedToday) {
                // Undo
                habit.copy(
                    streak = (habit.streak - 1).coerceAtLeast(0),
                    lastCompletedDate = null
                )
            } else {
                // Complete
                habit.copy(
                    streak = habit.streak + 1,
                    lastCompletedDate = todayStr
                )
            }
            repository.updateHabit(updated)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // --- Time Block Actions ---
    fun addTimeBlock(label: String, startTime: String, endTime: String, emoji: String = "🔒") {
        viewModelScope.launch {
            repository.insertTimeBlock(TimeBlock(label = label, startTime = startTime, endTime = endTime, emoji = emoji))
        }
    }

    fun deleteTimeBlock(block: TimeBlock) {
        viewModelScope.launch {
            repository.deleteTimeBlock(block)
        }
    }

    // --- Focus/Timer Actions ---
    private var timerJob: kotlinx.coroutines.Job? = null

    fun startFocusTimer() {
        if (focusTimerIsActive.value) return
        focusTimerIsActive.value = true
        timerJob = viewModelScope.launch {
            while (focusTimerSecondsLeft.value > 0 && focusTimerIsActive.value) {
                kotlinx.coroutines.delay(1000)
                focusTimerSecondsLeft.value -= 1
            }
            if (focusTimerSecondsLeft.value <= 0) {
                if (!isBreakMode.value) {
                    momentumLevel.value = (momentumLevel.value + 25f).coerceAtMost(100f)
                    focusTimerSuccessCount.value += 1
                    
                    isBreakMode.value = true
                    focusTimerSecondsLeft.value = breakTimerMinutes.value * 60
                    
                    kotlinx.coroutines.delay(100)
                    focusTimerIsActive.value = false
                    startFocusTimer()
                } else {
                    isBreakMode.value = false
                    focusTimerSecondsLeft.value = focusTimerMinutes.value * 60
                    
                    val rem = focusCyclesRemaining.value - 1
                    if (rem > 0) {
                        focusCyclesRemaining.value = rem
                        kotlinx.coroutines.delay(100)
                        focusTimerIsActive.value = false
                        startFocusTimer()
                    } else {
                        focusTimerIsActive.value = false
                        focusCyclesRemaining.value = focusMaxCycles.value
                    }
                }
            }
        }
    }

    fun pauseFocusTimer() {
        focusTimerIsActive.value = false
        timerJob?.cancel()
    }

    fun resetFocusTimer(minutes: Int = 25) {
        pauseFocusTimer()
        val m = if (isBreakMode.value) breakTimerMinutes.value else focusTimerMinutes.value
        focusTimerSecondsLeft.value = m * 60
        focusCyclesRemaining.value = focusMaxCycles.value
    }

    fun setTimerMinutes(mins: Int) {
        focusTimerMinutes.value = mins.coerceIn(1, 180)
        if (!focusTimerIsActive.value && !isBreakMode.value) {
            focusTimerSecondsLeft.value = focusTimerMinutes.value * 60
        }
    }

    fun setBreakMinutes(mins: Int) {
        breakTimerMinutes.value = mins.coerceIn(1, 120)
        if (!focusTimerIsActive.value && isBreakMode.value) {
            focusTimerSecondsLeft.value = breakTimerMinutes.value * 60
        }
    }

    fun setTimerMode(isBreak: Boolean) {
        isBreakMode.value = isBreak
        pauseFocusTimer()
        focusTimerSecondsLeft.value = (if (isBreak) breakTimerMinutes.value else focusTimerMinutes.value) * 60
    }

    fun toggleCoworkingRoom() {
        isCoworkingActive.value = !isCoworkingActive.value
        if (isCoworkingActive.value) {
            coworkingParticipants.value = (3..12).random()
        }
    }

    // --- Smart Reset ---
    fun runCompassionateDailyReset() {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            // Pull all incomplete tasks from other days and move them to today gently!
            val allIncomplete = repository.getAllTasks().first().filter { !it.isCompleted && it.dayDate != todayStr }
            allIncomplete.forEach { task ->
                repository.updateTask(task.copy(dayDate = todayStr))
            }
            currentSelectedDate.value = todayStr
        }
    }

    // --- Mood Checks ---
    fun performMoodCheckin(mood: String, note: String?) {
        currentMood.value = mood
        viewModelScope.launch {
            val checkin = MoodCheckin(mood = mood, note = note)
            repository.insertCheckin(checkin)
        }
    }

    // --- Seeding Starter Sample Data ---
    private fun seedInitialSampleData() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(Date())

            val hasSeeded = prefs.getBoolean("has_seeded", false)
            if (!hasSeeded) {
                // Seed structured tasks
                val initialTasks = listOf(
                    TimelineTask(
                        title = "Healthy Morning Breakfast",
                        timeSlotStart = "08:30",
                        timeSlotEnd = "09:00",
                        energyLevel = "Low",
                        category = "Routine",
                        emoji = "🥞",
                        dayDate = dateStr
                    ),
                    TimelineTask(
                        title = "Clean the bedroom clutter",
                        timeSlotStart = "10:30",
                        timeSlotEnd = "11:00",
                        energyLevel = "High",
                        category = "Urgent",
                        emoji = "🧹",
                        dayDate = dateStr
                    ),
                    TimelineTask(
                        title = "Water my green plant friends",
                        timeSlotStart = "14:15",
                        timeSlotEnd = "14:25",
                        energyLevel = "Low",
                        category = "Quick Win",
                        emoji = "🌿",
                        dayDate = dateStr
                    )
                )
                initialTasks.forEach { repository.insertTask(it) }

                // Seed some memories
                val initialMemories = listOf(
                    MemoryItem(title = "Car Backup Keys Location", details = "Hanging on the little wooden cup drawer inside the kitchen wall."),
                    MemoryItem(title = "Health Insurance Number ID", details = "Saved under cabinet code SEC-BD-77 with physical blue policy card.")
                )
                initialMemories.forEach { repository.insertMemory(it) }

                // Seed Habits
                val initialHabits = listOf(
                    Habit(title = "Drink warm visual tea cups", frequency = "Daily", streak = 5, emoji = "🍵"),
                    Habit(title = "Sit silently for 5 mins", frequency = "Flexible", streak = 3, emoji = "🧘")
                )
                initialHabits.forEach { repository.insertHabit(it) }

                // Seed a default sleep block if none exist
                val currentBlocks = repository.getAllTimeBlocks().first()
                if (currentBlocks.isEmpty()) {
                    repository.insertTimeBlock(TimeBlock(label = "Sleep", startTime = "23:00", endTime = "07:00", emoji = "🌙"))
                }

                prefs.edit().putBoolean("has_seeded", true).apply()
            }
        }
    }

    fun scheduleTaskReminder(task: TimelineTask) {
        if (!task.hasReminder) {
            cancelTaskReminder(task)
            return
        }
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val baseDate = sdf.parse(task.dayDate) ?: return
            
            val calendar = Calendar.getInstance()
            calendar.time = baseDate
            
            val timeParts = task.timeSlotStart.split(":")
            if (timeParts.isNotEmpty()) {
                val hour = timeParts[0].trim().toIntOrNull() ?: 9
                val minute = if (timeParts.size > 1) timeParts[1].trim().toIntOrNull() ?: 0 else 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val eventTime = calendar.timeInMillis

                // Subtract reminderMinutesBefore minutes for notification
                calendar.add(Calendar.MINUTE, -task.reminderMinutesBefore)
                var triggerTime = calendar.timeInMillis
                
                val now = System.currentTimeMillis()
                // Fallback: If 5-min prior is in the past, but the event is still in the future,
                // schedule to fire in 5 seconds to give instant feedback.
                if (triggerTime <= now && eventTime > now) {
                    triggerTime = now + 5000
                }
                
                if (triggerTime > now) {
                    val context = getApplication<Application>().applicationContext
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager != null) {
                        val intent = Intent(context, ReminderReceiver::class.java).apply {
                            putExtra("TASK_ID", task.id.toLong())
                            putExtra("TASK_TITLE", task.title)
                            putExtra("TASK_START_TIME", task.timeSlotStart)
                            putExtra("TASK_REMINDER_MINUTES", task.reminderMinutesBefore)
                        }
                        val pendingFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id,
                            intent,
                            pendingFlags
                        )
                        
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                                } else {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                                }
                            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                            } else {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                                } else {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                                }
                            }
                        } catch (se: SecurityException) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            } else {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelTaskReminder(task: TimelineTask) {
        try {
            val context = getApplication<Application>().applicationContext
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    task.id,
                    intent,
                    pendingFlags
                )
                alarmManager.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
