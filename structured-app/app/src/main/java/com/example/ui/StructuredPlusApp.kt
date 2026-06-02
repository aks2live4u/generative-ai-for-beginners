package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.focus.onFocusChanged
import com.example.data.BrainDump
import com.example.data.Habit
import com.example.data.MemoryItem
import com.example.data.TimeBlock
import com.example.data.TimelineTask
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

data class CalendarDay(val dayName: String, val dateStr: String, val dateNum: String)

data class PendingEditData(
    val title: String,
    val startTime: String,
    val endTime: String,
    val energy: String,
    val hasReminder: Boolean,
    val reminderMinutesBefore: Int = 15,
    val repeatType: String,
    val selectedDays: List<Int>,
    val chosenDate: String,
    val details: String
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StructuredPlusApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isEmergency by viewModel.isEmergencyMode.collectAsStateWithLifecycle()
    val isDyslexiaFont by viewModel.isDyslexiaFontApplied.collectAsStateWithLifecycle()
    val themeOption by viewModel.appThemeOption.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(0) }
    
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeOption) {
        "Cosmos Dark" -> true
        "Sand Light" -> false
        else -> isSystemDark
    }

    // Dynamic High-Contrast Text Color Selection
    val selectedColorHex by viewModel.selectedTextColorHex.collectAsStateWithLifecycle()
    val highlightColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            Color(0xFFFFFFFF)
        }
    }

    val customFontFamily = if (isDyslexiaFont) FontFamily.Monospace else FontFamily.SansSerif

    // Pure pitch black background brush for maximum contrast in black mode, or Sand Light background in white mode
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(colors = listOf(SandDb, SandDb))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                selectedIndex = currentTab,
                onTabSelected = { currentTab = it },
                highlightColor = highlightColor,
                isDark = isDark
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = if (isDark) Color.Black else SandDb
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBarHeader(viewModel, customFontFamily, highlightColor)
                    
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            0 -> PlannerTab(viewModel, customFontFamily, highlightColor, isDark)
                            1 -> InboxTab(viewModel, customFontFamily, highlightColor, isDark)
                            2 -> FocusTab(viewModel, customFontFamily, highlightColor, isDark)
                            3 -> SettingsTab(viewModel, customFontFamily, highlightColor, isDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconEmblemMini(
    iconStyle: String,
    highlightColor: Color
) {
    val customAccentColor = when (iconStyle) {
        "Neon Cyan" -> Color(0xFF38BDF8)
        "Golden Hour" -> Color(0xFFFBBF24)
        "Lilac Bloom" -> Color(0xFFC084FC)
        "Electric Rose" -> Color(0xFFFB7185)
        else -> if (highlightColor == Color.White) Color(0xFF38BDF8) else highlightColor
    }

    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0B10), Color(0xFF1B1C2E))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokeW = 1.5.dp.toPx()
            
            // Outer Segment 1
            drawArc(
                color = customAccentColor,
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Outer Segment 2
            drawArc(
                color = if (iconStyle == "Theme Match") Color(0xFF38BDF8) else customAccentColor,
                startAngle = 45f,
                sweepAngle = 130f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Central core container (R = 2.dp)
            drawCircle(
                color = Color(0xFF151622),
                radius = 3.dp.toPx()
            )
        }
        
        Text(
            text = "+",
            color = if (iconStyle == "Theme Match") Color(0xFFFB7185) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TopAppBarHeader(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color) {
    val themeOption by viewModel.appThemeOption.collectAsStateWithLifecycle()
    val appIconPreset by viewModel.appIconPresetColor.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeOption) {
        "Cosmos Dark" -> true
        "Sand Light" -> false
        else -> isSystemDark
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppIconEmblemMini(
                iconStyle = appIconPreset,
                highlightColor = highlightColor
            )
            
            Text(
                text = "Structured+",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp,
                color = if (isDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun AppBottomBar(selectedIndex: Int, onTabSelected: (Int) -> Unit, highlightColor: Color, isDark: Boolean) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = if (isDark) Color(0xFF070707) else Color(0xFFE4E4E7),
        modifier = Modifier.drawBehind {
            val borderSize = 1.dp.toPx()
            drawLine(
                color = if (isDark) Color(0x33555555) else Color(0x1FA2A2A2),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = borderSize
            )
        }
    ) {
        val items = listOf(
            Triple(0, "Planner", Icons.Default.CalendarToday),
            Triple(1, "Brain Dump", Icons.Default.Inbox),
            Triple(2, "Focus", Icons.Default.Timer),
            Triple(3, "Settings", Icons.Default.Settings)
        )

        items.forEach { (index, label, icon) ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = label,
                        tint = if (isSelected) highlightColor else Color.Gray
                    ) 
                },
                label = { 
                    Text(
                        text = label, 
                        maxLines = 1, 
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) highlightColor else Color.Gray
                    ) 
                },
                alwaysShowLabel = true,
                modifier = Modifier.testTag("bottom_nav_${label.lowercase().replace(" ", "_")}")
            )
        }
    }
}

// ==========================================
// VIEW 1: PLANNER (Timeline Day View)
// ==========================================

sealed class TimelineDisplayItem {
    data class Task(val task: TimelineTask) : TimelineDisplayItem()
    data class Block(val block: TimeBlock) : TimelineDisplayItem()
    data class FreeSlot(val startTime: String, val endTime: String, val durationMinutes: Int) : TimelineDisplayItem()
}

fun timeStringToMinutes(time: String): Int {
    val parts = time.split(":")
    if (parts.size < 2) return 0
    val h = parts[0].trim().toIntOrNull() ?: 0
    val m = parts[1].trim().take(2).toIntOrNull() ?: 0
    return h * 60 + m
}

fun minutesToTimeString(minutes: Int): String {
    val clamped = minutes.coerceIn(0, 24 * 60)
    val h = clamped / 60
    val m = clamped % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}

fun buildMergedTimeline(tasks: List<TimelineTask>, blocks: List<TimeBlock>): List<TimelineDisplayItem> {
    data class Slot(val start: Int, val end: Int, val item: TimelineDisplayItem)
    val allSlots = mutableListOf<Slot>()
    for (task in tasks) {
        val s = timeStringToMinutes(task.timeSlotStart)
        var e = timeStringToMinutes(task.timeSlotEnd)
        if (e <= s) e = s + 30
        allSlots.add(Slot(s, e, TimelineDisplayItem.Task(task)))
    }
    for (block in blocks) {
        val s = timeStringToMinutes(block.startTime)
        val e = timeStringToMinutes(block.endTime)
        if (e < s) {
            // Midnight-crossing block (e.g. Sleep 23:00–07:00): split into two visible segments
            allSlots.add(Slot(s, 24 * 60, TimelineDisplayItem.Block(block))) // evening portion
            allSlots.add(Slot(0, e, TimelineDisplayItem.Block(block)))        // morning portion
        } else {
            allSlots.add(Slot(s, e, TimelineDisplayItem.Block(block)))
        }
    }
    allSlots.sortBy { it.start }
    val result = mutableListOf<TimelineDisplayItem>()
    var cursor = 0
    for (slot in allSlots) {
        val gapMinutes = slot.start - cursor
        if (gapMinutes >= 15) {
            result.add(TimelineDisplayItem.FreeSlot(
                minutesToTimeString(cursor),
                minutesToTimeString(slot.start),
                gapMinutes
            ))
        }
        result.add(slot.item)
        if (slot.end > cursor) cursor = slot.end
    }
    val remaining = 24 * 60 - cursor
    if (remaining >= 30) {
        result.add(TimelineDisplayItem.FreeSlot(minutesToTimeString(cursor), "24:00", remaining))
    }
    return result
}

@Composable
fun PlannerTab(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color, isDark: Boolean) {
    val tasks by viewModel.tasksForToday.collectAsStateWithLifecycle()
    val warning by viewModel.scheduleWarning.collectAsStateWithLifecycle()
    val selectedDate by viewModel.currentSelectedDate.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showTimeBlockDialog by remember { mutableStateOf(false) }
    var freeGapStartTime by remember { mutableStateOf("") }
    val timeBlocks by viewModel.timeBlocks.collectAsStateWithLifecycle()

    val weekOffsetDays by viewModel.weekOffsetDays.collectAsStateWithLifecycle()

    // Generates calendar list for visual week slider strip based on offset
    val currentWeekDays = remember(weekOffsetDays) {
        val list = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, weekOffsetDays)
        cal.add(Calendar.DAY_OF_YEAR, -3) // Offset back to present beautifully surrounding selected center
        
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateNumFormat = SimpleDateFormat("d", Locale.getDefault())
        
        for (i in 0 until 7) {
            list.add(
                CalendarDay(
                    dayName = dayFormat.format(cal.time),
                    dateStr = dateFormat.format(cal.time),
                    dateNum = dateNumFormat.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val monthYearStr = remember(selectedDate) {
        try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfIn.parse(selectedDate)
            if (date != null) {
                val sdfOut = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                sdfOut.format(date)
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Month Header Display
        Text(
            text = monthYearStr,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = highlightColor,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 4.dp)
        )

        // Week calendar slider strip with navigation buttons wrapped in a floating frosted 3D Card
        Card(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, if (isDark) Color(0x2BFFFFFF) else Color(0x1F000000)),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xB31E1E28) else Color(0xD9FFFFFF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.shiftWeek(-7) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous Week",
                        tint = highlightColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentWeekDays.forEach { (dayName, dateStr, dateNum) ->
                        val isDaySelected = selectedDate == dateStr
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDaySelected) highlightColor.copy(alpha = 0.2f) else Color.Transparent
                                )
                                .clickable { viewModel.currentSelectedDate.value = dateStr }
                                .padding(vertical = 6.dp)
                                .width(34.dp)
                        ) {
                            Text(
                                text = dayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDaySelected) highlightColor else Color.Gray,
                                fontFamily = fontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (isDaySelected) highlightColor else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dateNum,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.shiftWeek(7) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next Week",
                        tint = highlightColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Day Timeline",
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else Color.Black
                )
                
                if (weekOffsetDays != 0) {
                    Box(
                        modifier = Modifier
                            .background(highlightColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.resetWeekOffset() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Today",
                            color = highlightColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily
                        )
                    }
                }
            }

            Button(
                onClick = { showAddTaskDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x3B4B5563) else Color(0x1F2A2A31),
                    contentColor = highlightColor
                ),
                border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .testTag("add_task_trigger_btn")
                    .height(38.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task", tint = highlightColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add task", fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Warning banner
        warning?.let { check ->
            if (check.isOverloaded) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0x293F3F46) else Color(0xF2EFEEDB)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ErrorSoft.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = ErrorSoft,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Timeline Buffer Advice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = fontFamily,
                                color = ErrorSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = check.warningMessage,
                            fontSize = 11.sp,
                            fontFamily = fontFamily,
                            color = if (isDark) Color.LightGray else Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (tasks.isNotEmpty()) {
                                    viewModel.deleteTask(tasks.last())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorSoft.copy(alpha = 0.15f),
                                contentColor = ErrorSoft
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .align(Alignment.End)
                        ) {
                            Text(text = "💡 Skip Last Event", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day summary chip row
        val mergedTimeline = remember(tasks, timeBlocks) {
            buildMergedTimeline(tasks, timeBlocks)
        }
        val scheduledMinutes = remember(tasks) { tasks.sumOf { it.durationMinutes } }
        val blockedMinutes = remember(timeBlocks) {
            timeBlocks.sumOf {
                val s = timeStringToMinutes(it.startTime)
                var e = timeStringToMinutes(it.endTime)
                if (e < s) e += 24 * 60
                e - s
            }
        }
        val freeMinutes = (24 * 60 - scheduledMinutes - blockedMinutes).coerceAtLeast(0)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val freeHours = freeMinutes / 60
                val freeMins = freeMinutes % 60
                val freeLabel = if (freeHours > 0) "${freeHours}h ${freeMins}m free" else "${freeMins}m free"
                Box(
                    modifier = Modifier
                        .background(highlightColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(freeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                }
                if (scheduledMinutes > 0) {
                    val sH = scheduledMinutes / 60
                    val sM = scheduledMinutes % 60
                    val schedLabel = if (sH > 0) "${sH}h ${sM}m planned" else "${sM}m planned"
                    Box(
                        modifier = Modifier
                            .background(Color(0x1F888888), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(schedLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = fontFamily)
                    }
                }
            }
            IconButton(
                onClick = { showTimeBlockDialog = true },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (timeBlocks.isNotEmpty()) highlightColor.copy(alpha = 0.15f) else Color(0x1F888888),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Manage Time Blocks",
                    tint = if (timeBlocks.isNotEmpty()) highlightColor else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (mergedTimeline.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✨", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nice and clean daily schedule.\nAdd a task to populate your timeline!",
                        fontFamily = fontFamily,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                itemsIndexed(mergedTimeline) { index, item ->
                    when (item) {
                        is TimelineDisplayItem.Task -> {
                            val taskList = mergedTimeline.filterIsInstance<TimelineDisplayItem.Task>()
                            val taskIndex = taskList.indexOfFirst { it.task.id == item.task.id }
                            TimelineTaskItemCard(
                                task = item.task,
                                viewModel = viewModel,
                                fontFamily = fontFamily,
                                isFirst = taskIndex == 0,
                                isLast = taskIndex == taskList.size - 1,
                                highlightColor = highlightColor,
                                isDark = isDark
                            )
                        }
                        is TimelineDisplayItem.Block -> {
                            TimeBlockItemCard(
                                block = item.block,
                                fontFamily = fontFamily,
                                isDark = isDark,
                                highlightColor = highlightColor,
                                onDelete = { viewModel.deleteTimeBlock(item.block) }
                            )
                        }
                        is TimelineDisplayItem.FreeSlot -> {
                            FreeSlotCard(
                                startTime = item.startTime,
                                endTime = item.endTime,
                                durationMinutes = item.durationMinutes,
                                fontFamily = fontFamily,
                                isDark = isDark,
                                highlightColor = highlightColor,
                                onAddTask = {
                                    freeGapStartTime = item.startTime
                                    showAddTaskDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            fontFamily = fontFamily,
            highlightColor = highlightColor,
            isDark = isDark,
            initialDateStr = selectedDate,
            initialStartTime = freeGapStartTime.ifEmpty { "" },
            onDismiss = { showAddTaskDialog = false; freeGapStartTime = "" },
            onConfirm = { title, start, end, energy, reminder, reminderMinutes, repeatType, selectedDays, details, chosenDate ->
                viewModel.addTaskWithRepeat(title, start, end, energy, reminder, reminderMinutes, repeatType, selectedDays, dayDate = chosenDate, details = details)
                showAddTaskDialog = false
                freeGapStartTime = ""
            }
        )
    }

    if (showTimeBlockDialog) {
        TimeBlockManagementDialog(
            timeBlocks = timeBlocks,
            fontFamily = fontFamily,
            isDark = isDark,
            highlightColor = highlightColor,
            onDismiss = { showTimeBlockDialog = false },
            onAdd = { label, startTime, endTime, emoji ->
                viewModel.addTimeBlock(label, startTime, endTime, emoji)
            },
            onDelete = { block -> viewModel.deleteTimeBlock(block) }
        )
    }
}

@Composable
fun TimelineTaskItemCard(
    task: TimelineTask,
    viewModel: MainViewModel,
    fontFamily: FontFamily,
    isFirst: Boolean,
    isLast: Boolean,
    highlightColor: Color,
    isDark: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var showEditChoiceDialog by remember { mutableStateOf(false) }
    var showDeleteChoiceDialog by remember { mutableStateOf(false) }
    var pendingEditData by remember { mutableStateOf<PendingEditData?>(null) }
    val isCompleted = task.isCompleted

    if (showDeleteChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChoiceDialog = false },
            title = {
                Text(
                    text = "Delete Agenda Item?",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            },
            text = {
                Text(
                    text = "Which occurrences of this series would you like to delete?",
                    fontFamily = fontFamily,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.deleteTask(task, deleteType = "just_this")
                            showDeleteChoiceDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x1FCDCDDF) else Color(0xFFE4E4E7),
                            contentColor = if (isDark) Color.White else Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete just this", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteTask(task, deleteType = "now_and_future")
                            showDeleteChoiceDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorSoft,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete now and future tasks", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteTask(task, deleteType = "entire_series")
                            showDeleteChoiceDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorSoft.copy(alpha = 0.82f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete entire series", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showDeleteChoiceDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isDark) Color.LightGray else Color.DarkGray
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = if (isDark) Color(0xFF1E1E24) else Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEditTaskDialog) {
        EditTaskDialog(
            task = task,
            fontFamily = fontFamily,
            highlightColor = highlightColor,
            isDark = isDark,
            onDismiss = { showEditTaskDialog = false },
            onConfirm = { editedTitle, editStart, editEnd, editEnergy, editReminder, editReminderMin, repeatType, selectedDays, details, chosenDate ->
                pendingEditData = PendingEditData(editedTitle, editStart, editEnd, editEnergy, editReminder, editReminderMin, repeatType, selectedDays, chosenDate, details)
                showEditTaskDialog = false
                showEditChoiceDialog = true
            }
        )
    }

    if (showEditChoiceDialog && pendingEditData != null) {
        val data = pendingEditData!!
        AlertDialog(
            onDismissRequest = { 
                showEditChoiceDialog = false 
                pendingEditData = null
            },
            title = {
                Text(
                    text = "Apply Changes",
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
            },
            text = {
                Text(
                    text = "Which occurrences in this recurring task series would you like to update?",
                    fontFamily = fontFamily,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.updateTaskWithRepeat(
                                task = task,
                                title = data.title,
                                startTime = data.startTime,
                                endTime = data.endTime,
                                energy = data.energy,
                                hasReminder = data.hasReminder,
                                reminderMinutesBefore = data.reminderMinutesBefore,
                                repeatType = data.repeatType,
                                selectedDays = data.selectedDays,
                                updateType = "just_this",
                                newDayDate = data.chosenDate,
                                details = data.details
                            )
                            showEditChoiceDialog = false
                            pendingEditData = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x1FCDCDDF) else Color(0xFFE4E4E7),
                            contentColor = if (isDark) Color.White else Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Modify just this", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.updateTaskWithRepeat(
                                task = task,
                                title = data.title,
                                startTime = data.startTime,
                                endTime = data.endTime,
                                energy = data.energy,
                                hasReminder = data.hasReminder,
                                reminderMinutesBefore = data.reminderMinutesBefore,
                                repeatType = data.repeatType,
                                selectedDays = data.selectedDays,
                                updateType = "now_and_future",
                                newDayDate = data.chosenDate,
                                details = data.details
                            )
                            showEditChoiceDialog = false
                            pendingEditData = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = highlightColor,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Modify now and future tasks", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.updateTaskWithRepeat(
                                task = task,
                                title = data.title,
                                startTime = data.startTime,
                                endTime = data.endTime,
                                energy = data.energy,
                                hasReminder = data.hasReminder,
                                reminderMinutesBefore = data.reminderMinutesBefore,
                                repeatType = data.repeatType,
                                selectedDays = data.selectedDays,
                                updateType = "entire_series",
                                newDayDate = data.chosenDate,
                                details = data.details
                            )
                            showEditChoiceDialog = false
                            pendingEditData = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = highlightColor.copy(alpha = 0.82f),
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Modify entire series", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { 
                            showEditChoiceDialog = false 
                            pendingEditData = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isDark) Color.LightGray else Color.DarkGray
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x33000000)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = if (isDark) Color(0xFF1E1E24) else Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Symmetrical Gray Frosted Glass cards styled on dark background
    val cardBgColor = if (isDark) {
        if (isCompleted) Color(0x1F3F3F46) else Color(0x3B2C2D35)
    } else {
        if (isCompleted) Color(0xFFE4E4E7) else Color(0xF2F4F2EE)
    }
    val cardBorderColor = if (isCompleted) Color(0x1A7E7E7E) else highlightColor.copy(alpha = 0.25f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline continuous connector line
        Box(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
                .drawBehind {
                    val lineColor = if (isCompleted) highlightColor.copy(alpha = 0.5f) else (if (isDark) Color(0x22FFFFFF) else Color(0x33000000))
                    val circleTopY = 10.dp.toPx()
                    val circleBottomY = 40.dp.toPx()
                    
                    // Draw top segment if not first
                    if (!isFirst) {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, circleTopY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    
                    // Draw bottom segment if not last
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2, circleBottomY),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(30.dp)
                    .background(highlightColor.copy(alpha = 0.12f), CircleShape)
                    .border(
                        width = if (isCompleted) 2.dp else 1.dp,
                        color = if (isCompleted) highlightColor else (if (isDark) Color(0x33FFFFFF) else Color(0x33000000)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = task.emoji, fontSize = 15.sp)
            }
        }

        // Frosted Card Design with High-End soft blurred physical shadow!
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp, bottom = 6.dp)
                .shadow(
                    elevation = if (isCompleted) 0.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
                .testTag("task_item_${task.title.lowercase().replace(" ", "_")}")
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, cardBorderColor),
            colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.82f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(11.dp),
                        tint = highlightColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.timeSlotStart} - ${task.timeSlotEnd}",
                        fontSize = 11.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = highlightColor
                    )
                    
                    if (task.category.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    highlightColor.copy(alpha = 0.12f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = fontFamily,
                                color = highlightColor
                            )
                        }
                    }

                    if (task.hasReminder) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    Color(0xFFFF9800).copy(alpha = 0.12f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Reminder",
                                modifier = Modifier.size(10.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (task.reminderMinutesBefore == 0) "at start" else "${task.reminderMinutesBefore}m prior",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = fontFamily,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = fontFamily,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) Color.Gray else (if (isDark) Color.White else Color.Black),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { viewModel.toggleTaskCompletion(task) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (isCompleted) highlightColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!task.substepsJson.isNullOrBlank() && !isExpanded) {
                    val previewText = remember(task.substepsJson) {
                        val raw = task.substepsJson.orEmpty().trim()
                        if (raw.startsWith("[")) {
                            try {
                                val arr = org.json.JSONArray(raw)
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) {
                                    list.add(arr.getString(i))
                                }
                                if (list.isNotEmpty()) "• " + list.joinToString(", ") else ""
                            } catch (e: Exception) { "" }
                        } else {
                            raw
                        }
                    }
                    if (previewText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewText,
                            fontSize = 11.sp,
                            fontFamily = fontFamily,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (isDark) Color(0x1FCDCDDF) else Color(0x1F000000))
                    Spacer(modifier = Modifier.height(6.dp))

                    if (!task.substepsJson.isNullOrBlank()) {
                        val isJsonArray = task.substepsJson.orEmpty().trim().startsWith("[")
                        if (isJsonArray) {
                            Text(
                                text = "🏡 Micro-Steps & Milestones:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = fontFamily,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val substepsList = remember(task.substepsJson) {
                                val list = mutableListOf<String>()
                                try {
                                    val arr = org.json.JSONArray(task.substepsJson)
                                    for (i in 0 until arr.length()) {
                                        list.add(arr.getString(i))
                                    }
                                } catch (e: Exception) {}
                                list
                            }

                            substepsList.forEach { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = "bullet",
                                        modifier = Modifier.size(12.dp),
                                        tint = highlightColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = step,
                                        fontSize = 11.sp,
                                        fontFamily = fontFamily,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "📝 Notes & Details:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = fontFamily,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.substepsJson.orEmpty(),
                                fontSize = 11.sp,
                                fontFamily = fontFamily,
                                color = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Overwhelmed with this event? Autorenew small steps easily!",
                            fontSize = 11.sp,
                            fontFamily = fontFamily,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.generateAITaskBreakdown(task, "standard") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x3B3F3F46) else Color(0x33BDBDBD)),
                                border = BorderStroke(1.dp, highlightColor.copy(0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "⚡ Plan Split", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                            }

                            Button(
                                onClick = { viewModel.generateAITaskBreakdown(task, "five_minute") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x3B3F3F46) else Color(0x33BDBDBD)),
                                border = BorderStroke(1.dp, highlightColor.copy(0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "⏳ 5-Min Play", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                            }

                            Button(
                                onClick = { viewModel.generateAITaskBreakdown(task, "bare_minimum") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x3B3F3F46) else Color(0x33BDBDBD)),
                                border = BorderStroke(1.dp, Color(0x33EF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(text = "🚨 Bare Limit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErrorSoft, fontFamily = fontFamily)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (isDark) Color(0x0EFFFFFF) else Color(0x1F000000))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Energy Index: ${task.energyLevel}",
                            fontSize = 10.sp,
                            fontFamily = fontFamily,
                            color = Color.Gray
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showEditTaskDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Block",
                                    tint = highlightColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { showDeleteChoiceDialog = true },
                                modifier = Modifier.testTag("delete_task_btn").size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = ErrorSoft.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeBlockItemCard(
    block: TimeBlock,
    fontFamily: FontFamily,
    isDark: Boolean,
    highlightColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp, start = 46.dp, end = 8.dp)
            .background(
                if (isDark) Color(0x1A374151) else Color(0x1A9CA3AF),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0x33888888), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = block.emoji, fontSize = 14.sp)
            Column {
                Text(
                    text = block.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151)
                )
                Text(
                    text = "${block.startTime} – ${block.endTime} · Blocked",
                    fontSize = 10.sp,
                    fontFamily = fontFamily,
                    color = Color.Gray
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove block",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun FreeSlotCard(
    startTime: String,
    endTime: String,
    durationMinutes: Int,
    fontFamily: FontFamily,
    isDark: Boolean,
    highlightColor: Color,
    onAddTask: () -> Unit
) {
    val durationText = if (durationMinutes >= 60) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        if (m > 0) "${h}h ${m}m free" else "${h}h free"
    } else {
        "${durationMinutes}m free"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp, start = 46.dp, end = 8.dp)
            .border(1.dp, highlightColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "✨ $durationText",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                fontFamily = fontFamily,
                color = highlightColor.copy(alpha = 0.8f)
            )
            Text(
                text = "$startTime – $endTime",
                fontSize = 10.sp,
                fontFamily = fontFamily,
                color = Color.Gray
            )
        }
        TextButton(
            onClick = onAddTask,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = highlightColor, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = "Add Task", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
        }
    }
}

@Composable
fun TimeBlockManagementDialog(
    timeBlocks: List<TimeBlock>,
    fontFamily: FontFamily,
    isDark: Boolean,
    highlightColor: Color,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit,
    onDelete: (TimeBlock) -> Unit
) {
    var newLabel by remember { mutableStateOf("") }
    var newStart by remember { mutableStateOf("22:00") }
    var newEnd by remember { mutableStateOf("07:00") }
    var newEmoji by remember { mutableStateOf("🔒") }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }

    val presets = listOf(
        Triple("Sleep", "23:00" to "07:00", "🌙"),
        Triple("Commute", "08:00" to "09:00", "🚗"),
        Triple("Lunch", "12:00" to "13:00", "🍽️"),
        Triple("Gym", "06:00" to "07:00", "🏋️")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1E24) else Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Manage Time Blocks",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tap ✏️ to edit a block's times, or 🗑 to remove it.", fontSize = 11.sp, color = Color.Gray, fontFamily = fontFamily)

                // Existing blocks
                timeBlocks.forEach { block ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (editingBlock?.id == block.id) highlightColor.copy(alpha = 0.12f)
                                else if (isDark) Color(0x1F3F3F46) else Color(0xFFEFEFEF),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${block.emoji} ${block.label} (${block.startTime}–${block.endTime})", fontSize = 12.sp, fontFamily = fontFamily, color = if (isDark) Color.White else Color.Black, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            editingBlock = block
                            newLabel = block.label
                            newStart = block.startTime
                            newEnd = block.endTime
                            newEmoji = block.emoji
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = highlightColor, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = { onDelete(block); if (editingBlock?.id == block.id) editingBlock = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000))

                // Quick presets
                Text("Quick presets:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, times, emoji) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(highlightColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, highlightColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { onAdd(label, times.first, times.second, emoji) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 14.sp)
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0x1FFFFFFF) else Color(0x1F000000))

                // Custom add / edit form
                Text(if (editingBlock != null) "Edit block:" else "Add custom block:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = highlightColor, fontFamily = fontFamily)
                OutlinedTextField(
                    value = newLabel, onValueChange = { newLabel = it },
                    label = { Text("Label (e.g. Study Time)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newStart, onValueChange = { newStart = it },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newEnd, onValueChange = { newEnd = it },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newEmoji, onValueChange = { newEmoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(70.dp), shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingBlock != null) {
                        OutlinedButton(
                            onClick = { editingBlock = null; newLabel = ""; newStart = "22:00"; newEnd = "07:00"; newEmoji = "🔒" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", fontFamily = fontFamily)
                        }
                    }
                    Button(
                        onClick = {
                            if (newLabel.isNotBlank()) {
                                val eb = editingBlock
                                if (eb != null) {
                                    onDelete(eb) // remove old version
                                    editingBlock = null
                                }
                                onAdd(newLabel, newStart, newEnd, newEmoji)
                                newLabel = ""; newStart = "22:00"; newEnd = "07:00"; newEmoji = "🔒"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (editingBlock != null) "Save Changes" else "Add Block", fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontFamily = fontFamily, fontWeight = FontWeight.Bold, color = highlightColor)
            }
        }
    )
}

// ==========================================
// VIEW 2: BRAIN DUMP HUB (Unscheduled Brain Dump Drawer)
// ==========================================
@Composable
fun InboxTab(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color, isDark: Boolean) {
    val dumps by viewModel.activeBrainDumps.collectAsStateWithLifecycle()
    var rawText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var schedulingDump by remember { mutableStateOf<BrainDump?>(null) }

    if (schedulingDump != null) {
        AddTaskDialog(
            fontFamily = fontFamily,
            highlightColor = highlightColor,
            isDark = isDark,
            initialTitle = schedulingDump?.rawText ?: "",
            initialDateStr = viewModel.currentSelectedDate.value,
            initialStartTime = "",
            onDismiss = { schedulingDump = null },
            onConfirm = { title, start, end, energy, reminder, reminderMinutes, repeatType, selectedDays, details, chosenDate ->
                viewModel.addTaskWithRepeat(title, start, end, energy, reminder, reminderMinutes, repeatType, selectedDays, dayDate = chosenDate, details = details)
                schedulingDump?.let { viewModel.discardBrainDump(it) }
                schedulingDump = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Brain Dump Hub",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = fontFamily,
            color = if (isDark) Color.White else Color.Black
        )
        Text(
            text = "Deter chaos: slam thoughts here instantly, plan them into the timeline anytime with custom times & recurrence.",
            fontSize = 11.sp,
            fontFamily = fontFamily,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            placeholder = { Text(text = "Captured raw thoughts or unorganized notes...", fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .testTag("braindump_text_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color.Black,
                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                focusedBorderColor = highlightColor,
                unfocusedBorderColor = if (isDark) Color(0x40FFFFFF) else Color(0x40000000),
                focusedContainerColor = if (isDark) Color(0x242D2D35) else Color(0x1F000000),
                unfocusedContainerColor = if (isDark) Color(0x242D2D35) else Color(0x1F000000)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (rawText.isNotBlank()) {
                    viewModel.registerBrainDump(rawText)
                    rawText = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("braindump_save_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Capture")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Capture Thought", fontSize = 13.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Unscheduled Thoughts (${dumps.size})",
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            fontSize = 14.sp,
            color = if (isDark) Color.White else Color.Black
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (dumps.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No unscheduled thoughts.\nYou are completely synchronized! ✨",
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dumps) { dump ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x1F000000)),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x3B2C2D35) else Color(0xF2F4F2EE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = dump.rawText,
                                fontSize = 13.sp,
                                fontFamily = fontFamily,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { schedulingDump = dump },
                                    colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Text(text = "⚡ Plan & Schedule Event", fontSize = 11.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.discardBrainDump(dump) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Discard",
                                        tint = ErrorSoft,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 3: FOCUS MODE
// ==========================================
@Composable
fun FocusTab(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color, isDark: Boolean) {
    val secondsLeft by viewModel.focusTimerSecondsLeft.collectAsStateWithLifecycle()
    val timerPlaying by viewModel.focusTimerIsActive.collectAsStateWithLifecycle()
    val focusMins by viewModel.focusTimerMinutes.collectAsStateWithLifecycle()
    val breakMins by viewModel.breakTimerMinutes.collectAsStateWithLifecycle()
    val isBreakMode by viewModel.isBreakMode.collectAsStateWithLifecycle()
    val maxCycles by viewModel.focusMaxCycles.collectAsStateWithLifecycle()
    val remCycles by viewModel.focusCyclesRemaining.collectAsStateWithLifecycle()

    val formattedSeconds = remember(secondsLeft) {
        val mins = secondsLeft / 60
        val secs = secondsLeft % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Focus Timeline Helper",
                fontWeight = FontWeight.Black,
                fontFamily = fontFamily,
                fontSize = 20.sp,
                color = if (isDark) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Establish rhythm on outstanding agenda items. Stretch regularly.",
                fontSize = 11.sp,
                fontFamily = fontFamily,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Focus Mode vs Break Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.setTimerMode(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isBreakMode) highlightColor else (if (isDark) Color(0x1FCDCDDF) else Color(0xFFE4E4E7)),
                        contentColor = if (!isBreakMode) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Focus Session", fontSize = 11.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.setTimerMode(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBreakMode) highlightColor else (if (isDark) Color(0x1FCDCDDF) else Color(0xFFE4E4E7)),
                        contentColor = if (isBreakMode) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Short Break", fontSize = 11.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                }
            }

            // Duration Customization Steppers
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isBreakMode) {
                            viewModel.setBreakMinutes(breakMins - 1)
                        } else {
                            viewModel.setTimerMinutes(focusMins - 1)
                        }
                    },
                    modifier = Modifier.size(36.dp).background(if (isDark) Color(0x1FCDCDDF) else Color(0x33BDBDBD), CircleShape)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black, fontSize = 16.sp)
                }

                Text(
                    text = if (isBreakMode) "${breakMins}m Break" else "${focusMins}m Focus",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = fontFamily,
                    color = if (isDark) Color.White else Color.Black
                )

                IconButton(
                    onClick = {
                        if (isBreakMode) {
                            viewModel.setBreakMinutes(breakMins + 1)
                        } else {
                            viewModel.setTimerMinutes(focusMins + 1)
                        }
                    },
                    modifier = Modifier.size(36.dp).background(if (isDark) Color(0x1FCDCDDF) else Color(0x33BDBDBD), CircleShape)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Beautiful Symmetrical Auto-Cycle Loop Counter Selector
            Text(
                text = "Auto-Cycle Loops: Link Work & Break (1-4x)",
                fontSize = 11.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.LightGray else Color.DarkGray
            )
            
            Row(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 3, 4).forEach { loopNum ->
                    val isSelected = maxCycles == loopNum
                    val bg = if (isSelected) highlightColor else (if (isDark) Color(0x1FCDCDDF) else Color(0x1FA2A2A2))
                    val fg = if (isSelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(bg, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setMaxCycles(loopNum) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${loopNum}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = fg
                        )
                    }
                }
            }

            if (timerPlaying) {
                Text(
                    text = "Cycle Progress: $remCycles of $maxCycles remaining",
                    fontSize = 11.sp,
                    fontFamily = fontFamily,
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Centered Ring Card
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x19FFFFFF) else Color(0x1F000000)),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x3B2C2D35) else Color(0xF2F4F2EE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Symmetrical visual clock ring
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .drawBehind {
                                val maxSeconds = (if (isBreakMode) breakMins else focusMins) * 60f
                                val progress = if (maxSeconds > 0) (secondsLeft / maxSeconds).coerceIn(0f, 1f) else 1f
                                drawArc(
                                    color = highlightColor.copy(alpha = 0.12f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
                                )
                                drawArc(
                                    color = highlightColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 8.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formattedSeconds,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons aligned with equal proportions and beautifully centered and balanced
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (timerPlaying) viewModel.pauseFocusTimer() else viewModel.startFocusTimer()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerPlaying) Color(0xFFEF4444) else highlightColor,
                                contentColor = if (timerPlaying) Color.White else (if (isDark) Color.Black else Color.White)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .weight(1f)
                                .testTag("focus_start_stop_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (timerPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Trigger"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (timerPlaying) "Pause" else "Start ${if (isBreakMode) breakMins else focusMins}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.resetFocusTimer() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isDark) Color(0x2B4B5563) else Color(0x33BDBDBD), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, 
                                contentDescription = "Reset Timer", 
                                tint = if (isDark) Color.White else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VIEW 4: HABITS & ROUTINES
// ==========================================
@Composable
fun HabitsTab(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color, isDark: Boolean) {
    val habitsList by viewModel.habits.collectAsStateWithLifecycle()
    val selectedDate by viewModel.currentSelectedDate.collectAsStateWithLifecycle()
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Habits & Routines", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp, 
                    fontFamily = fontFamily, 
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "Gentle daily habits without overload.", 
                    fontSize = 11.sp, 
                    fontFamily = fontFamily, 
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Uniformly styled button matching add-task trigger button
            Button(
                onClick = { showAddHabitDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0x3B4B5563) else Color(0x1F2A2A31),
                    contentColor = highlightColor
                ),
                border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("add_habit_trigger")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit", tint = highlightColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add", fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (habitsList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Establish recurring daily routines.\nCreate your first custom habit above!", fontSize = 13.sp, fontFamily = fontFamily, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(habitsList) { habit ->
                    val isChecked = habit.lastCompletedDate == selectedDate
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0x1FA2A2A2) else Color(0x1F000000)),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x3B2C2D35) else Color(0xF2F4F2EE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = habit.emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = habit.title, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 14.sp, 
                                        fontFamily = fontFamily, 
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Streak: ${habit.streak} Days Completed", 
                                    fontSize = 11.sp, 
                                    color = highlightColor, 
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { viewModel.toggleHabitCompletion(habit) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "checkbox",
                                        tint = if (isChecked) DopamineGreen else Color.LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteHabit(habit) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "delete", tint = ErrorSoft.copy(0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        var hTitle by remember { mutableStateOf("") }
        var hEmoji by remember { mutableStateOf("🌱") }
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("Add New Custom Habit", fontFamily = fontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hTitle, 
                        onValueChange = { hTitle = it }, 
                        placeholder = { Text("e.g. Drink Water") },
                        label = { Text("Routine Label") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = hEmoji, 
                        onValueChange = { hEmoji = it }, 
                        label = { Text("Emoji Symbol") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hTitle.isNotBlank()) {
                            viewModel.addHabit(hTitle, "Flexible", 3, hEmoji)
                            showAddHabitDialog = false
                        }
                    }, 
                    shape = RoundedCornerShape(10.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White)
                ) {
                    Text("Add Routine", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showAddHabitDialog = false }) { Text("Cancel", fontFamily = fontFamily, color = Color.Gray) } }
        )
    }
}

// ==========================================
// VIEW 5: SETTINGS
// ==========================================
@Composable
fun AppIconPreview(
    iconStyle: String,
    highlightColor: Color,
    isDark: Boolean
) {
    val customAccentColor = when (iconStyle) {
        "Neon Cyan" -> Color(0xFF38BDF8)
        "Golden Hour" -> Color(0xFFFBBF24)
        "Lilac Bloom" -> Color(0xFFC084FC)
        "Electric Rose" -> Color(0xFFFB7185)
        else -> if (highlightColor == Color.White) Color(0xFF38BDF8) else highlightColor
    }

    Box(
        modifier = Modifier
            .size(76.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0B10), Color(0xFF1B1C2E))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .border(0.5.dp, Color(0x11FFFFFF))
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(0.5.dp, Color(0x18FFFFFF))
        )
        
        Canvas(modifier = Modifier.size(48.dp)) {
            val strokeW = 4.dp.toPx()
            
            // Outer Segment 1
            drawArc(
                color = customAccentColor,
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Outer Segment 2
            drawArc(
                color = if (iconStyle == "Theme Match") Color(0xFF38BDF8) else customAccentColor,
                startAngle = 45f,
                sweepAngle = 130f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Outer Segment 3
            drawArc(
                color = if (iconStyle == "Theme Match") Color(0xFFFBBF24) else Color.White,
                startAngle = 190f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            
            // Central core container (R = 8.dp)
            drawCircle(
                color = Color(0xFF151622),
                radius = 8.dp.toPx()
            )
            drawCircle(
                color = Color(0x30FFFFFF),
                radius = 8.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        Text(
            text = "+",
            color = if (iconStyle == "Theme Match") Color(0xFFFB7185) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 1.dp)
        )
    }
}

@Composable
fun SettingsTab(viewModel: MainViewModel, fontFamily: FontFamily, highlightColor: Color, isDark: Boolean) {
    val isDyslexiaFont by viewModel.isDyslexiaFontApplied.collectAsStateWithLifecycle()
    val themeOption by viewModel.appThemeOption.collectAsStateWithLifecycle()
    val selectedColorHex by viewModel.selectedTextColorHex.collectAsStateWithLifecycle()

    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileFocusStyle by viewModel.profileFocusStyle.collectAsStateWithLifecycle()
    val profileDailyEnergyGoal by viewModel.profileDailyEnergyGoal.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var showPermissionPrompt by remember { mutableStateOf(false) }

    if (showEditProfileDialog) {
        EditProfileDialog(
            fontFamily = fontFamily,
            highlightColor = highlightColor,
            isDark = isDark,
            currentName = profileName,
            currentStyle = profileFocusStyle,
            currentGoal = profileDailyEnergyGoal,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, style, goal ->
                viewModel.updateProfile(name, style, goal)
                showEditProfileDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings Customization",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = fontFamily,
            color = if (isDark) Color.White else Color.Black
        )

        // Custom 3D-floating standing-out Profile section layout
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, highlightColor.copy(alpha = 0.4f)),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x3B2C2D35) else Color(0xF2F4F2EE)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { showEditProfileDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initials = remember(profileName) {
                    profileName.split(" ")
                        .filter { it.isNotBlank() }
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .take(2)
                        .ifEmpty { "ME" }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(highlightColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, highlightColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initials, fontWeight = FontWeight.Black, color = highlightColor, fontSize = 14.sp, fontFamily = fontFamily)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color.Black,
                        fontFamily = fontFamily
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Personal Profile", fontSize = 11.sp, color = Color.Gray, fontFamily = fontFamily)
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = highlightColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Section 1: Dynamic High-Contrast Text Colors
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0x19FFFFFF) else Color(0x1F000000)),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x223A3A3C) else Color(0xF2F2F7F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🎨 SELECT DYNAMIC HIGHLIGHT COLOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = fontFamily,
                    color = highlightColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val beautifulColors = listOf(
                        "#FFFFFF" to "White",
                        "#38BDF8" to "Cyan",
                        "#34D399" to "Mint",
                        "#FBBF24" to "Gold",
                        "#C084FC" to "Lilac",
                        "#FB7185" to "Pink"
                    )

                    beautifulColors.forEach { (hex, name) ->
                        val itemColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(itemColor, CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (hex == "#FFFFFF") Color.DarkGray else Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.selectedTextColorHex.value = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Notification & Alerts Hub
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0x19FFFFFF) else Color(0x1F000000)),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x223A3A3C) else Color(0xF2F2F7F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔔 NOTIFICATIONS & ALERTS STATUS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = fontFamily,
                    color = highlightColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasNotificationPermission) "System Alerts Active" else "Alerts Currently Blocked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = fontFamily,
                            color = if (hasNotificationPermission) Color(0xFF34D399) else (if (isDark) Color.White else Color.Black)
                        )
                        Text(
                            text = if (hasNotificationPermission) "Gentle notifications will fire at your chosen time before scheduled events." else "Tap to trigger the notification configuration setup.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = fontFamily
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= 33) {
                                showPermissionPrompt = true
                            } else {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Notifications are properly authorized!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasNotificationPermission) Color(0xFF34D399).copy(alpha = 0.15f) else highlightColor,
                            contentColor = if (hasNotificationPermission) Color(0xFF34D399) else (if (isDark) Color.Black else Color.White)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "Settings" else "Configure",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily
                        )
                    }
                }
            }
        }

        // Exact alarm permission banner (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⏰ Exact Alarm Permission Needed", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = fontFamily, color = Color(0xFFFF9800))
                            Text("Without this, reminders may be delayed by up to 15 minutes. Tap 'Fix' to grant it.", fontSize = 10.sp, color = Color.Gray, fontFamily = fontFamily)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        }
                    }
                }
            }
        }

        // Exclusive App Icon Customizer Panel
        val appIconPreset by viewModel.appIconPresetColor.collectAsStateWithLifecycle()
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0x19FFFFFF) else Color(0x1F000000)),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x223A3A3C) else Color(0xF2F2F7F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🌌 PREMIUM APP LOGO COLOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = fontFamily,
                    color = highlightColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconPreview(
                        iconStyle = appIconPreset,
                        highlightColor = highlightColor,
                        isDark = isDark
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "In-App Accent Emblem Color",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = fontFamily,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            text = "Select a signature premium accent color for your customized in-app app symbol in the top toolbar.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = fontFamily
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Horizontally scrollable presets for premium feel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presetsList = listOf("Theme Match", "Neon Cyan", "Golden Hour", "Lilac Bloom", "Electric Rose")
                            presetsList.forEach { p ->
                                val isSelectedPreset = appIconPreset == p
                                val presetLabel = p.substringBefore(" ")
                                val presetColor = when (p) {
                                    "Neon Cyan" -> Color(0xFF38BDF8)
                                    "Golden Hour" -> Color(0xFFFBBF24)
                                    "Lilac Bloom" -> Color(0xFFC084FC)
                                    "Electric Rose" -> Color(0xFFFB7185)
                                    else -> highlightColor
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelectedPreset) presetColor.copy(alpha = 0.15f) 
                                            else (if (isDark) Color(0x11FFFFFF) else Color(0x0A000000))
                                        )
                                        .border(
                                            width = if (isSelectedPreset) 1.5.dp else 0.5.dp,
                                            color = if (isSelectedPreset) presetColor else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.appIconPresetColor.value = p }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontFamily,
                                        color = if (isSelectedPreset) presetColor else (if (isDark) Color.LightGray else Color.DarkGray)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Themes & Modes Card
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0x19FFFFFF) else Color(0x1F000000)),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x223A3A3C) else Color(0xF2F2F7F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Visual Theme Configuration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = fontFamily,
                    color = highlightColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val themes = listOf(
                        Triple("System", "💻 Auto", Icons.Default.BrightnessAuto),
                        Triple("Cosmos Dark", "🌌 Cosmos", Icons.Default.DarkMode),
                        Triple("Sand Light", "🌾 Sand", Icons.Default.LightMode)
                    )

                    themes.forEach { (themeKey, displayLabel, icon) ->
                        val isSelected = themeOption == themeKey
                        Button(
                            onClick = { viewModel.appThemeOption.value = themeKey },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) highlightColor else (if (isDark) Color(0x1FCDCDDF) else Color(0xFFE4E4E7)),
                                contentColor = if (isSelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = icon, contentDescription = displayLabel, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = displayLabel.substringAfter(" "), fontSize = 11.sp, fontFamily = fontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0x0EFFFFFF) else Color(0x1F000000))

                // Dyslexia options custom switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Dyslexia-friendly Layout", fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = fontFamily, color = if (isDark) Color.White else Color.Black)
                        Text(text = "Applies uniform clean gaps and spacing constraints.", fontSize = 10.sp, color = Color.Gray, fontFamily = fontFamily)
                    }
                    Switch(
                        checked = isDyslexiaFont,
                        onCheckedChange = { viewModel.isDyslexiaFontApplied.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = highlightColor,
                            checkedTrackColor = highlightColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showPermissionPrompt) {
        NotificationPermissionPromptDialog(
            fontFamily = fontFamily,
            onDismissRequest = { showPermissionPrompt = false },
            onConfirmEnable = {
                showPermissionPrompt = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

// Dialog: Add Task Form with on-the-fly classification and repetition options
fun adjustTimeOffset(timeStr: String, offsetMinutes: Int): String {
    try {
        val parts = timeStr.split(":")
        if (parts.size < 2) return timeStr
        var hour = parts[0].trim().toIntOrNull() ?: 9
        var minute = parts[1].trim().take(2).toIntOrNull() ?: 0
        
        minute += offsetMinutes
        while (minute < 0) {
            minute += 60
            hour -= 1
        }
        while (minute >= 60) {
            minute -= 60
            hour += 1
        }
        if (hour < 0) hour += 24
        if (hour >= 24) hour -= 24
        
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } catch (e: Exception) {
        return timeStr
    }
}

fun calculateDurationMinutes(start: String, end: String): Int {
    try {
        val startParts = start.split(":")
        val endParts = end.split(":")
        if (startParts.size < 2 || endParts.size < 2) return 60
        val startH = startParts[0].trim().toIntOrNull() ?: 9
        val startM = startParts[1].trim().take(2).toIntOrNull() ?: 0
        val endH = endParts[0].trim().toIntOrNull() ?: 10
        val endM = endParts[1].trim().take(2).toIntOrNull() ?: 0
        
        var diff = (endH * 60 + endM) - (startH * 60 + startM)
        if (diff < 0) diff += 24 * 60
        return diff
    } catch (e: Exception) {
        return 60
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledTaskDialog(
    isEditMode: Boolean,
    task: TimelineTask?,
    fontFamily: FontFamily,
    highlightColor: Color,
    isDark: Boolean,
    initialTitle: String = "",
    initialDateStr: String = "",
    initialStartTime: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean, Int, String, List<Int>, String, String) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: initialTitle) }
    var startHour by remember { mutableStateOf(task?.timeSlotStart ?: initialStartTime.ifEmpty { "09:00" }) }
    var endHour by remember { mutableStateOf(task?.timeSlotEnd ?: "10:00") }
    var energy by remember { mutableStateOf(task?.energyLevel ?: "Medium") }
    var hasReminder by remember { mutableStateOf(task?.hasReminder ?: false) }
    var reminderMinutes by remember { mutableStateOf(task?.reminderMinutesBefore ?: 15) }
    var repeatType by remember { mutableStateOf("Just Today") } 
    val selectedDays = remember { mutableStateListOf<Int>() }
    val subtasks = remember { mutableStateListOf<String>() }
    var newSubtaskText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var taskDate by remember { mutableStateOf(task?.dayDate ?: initialDateStr.ifEmpty { String.format(Locale.getDefault(), "%tF", Date()) }) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            hasReminder = true
        }
    }

    var showPermissionPopup by remember { mutableStateOf(false) }

    LaunchedEffect(task) {
        if (task != null && !task.substepsJson.isNullOrBlank()) {
            val trimmed = task.substepsJson.trim()
            if (trimmed.startsWith("[")) {
                try {
                    val array = org.json.JSONArray(trimmed)
                    subtasks.clear()
                    for (i in 0 until array.length()) {
                        subtasks.add(array.getString(i))
                    }
                } catch (e: Exception) {}
            } else {
                subtasks.clear()
                subtasks.add(trimmed)
            }
        }
    }

    if (showDatePicker) {
        TaskDatePickerDialog(
            initialDateStr = taskDate,
            fontFamily = fontFamily,
            onDateSelected = { selectedDate ->
                taskDate = selectedDate
            },
            onDismiss = { showDatePicker = false }
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { win ->
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = if (isDark) Color(0xFF121214) else Color(0xFFF7F4EF)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEditMode) "Edit " else "Add ",
                            color = if (isDark) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = fontFamily
                        )
                        Text(
                            text = "Task",
                            color = highlightColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = fontFamily
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title component Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(if (isDark) Color(0x3B2C2D35) else Color(0x1FA2A2A2), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessAlarm,
                                contentDescription = "Clock",
                                tint = highlightColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("What is the agenda item?", color = Color.Gray, fontSize = 18.sp, fontFamily = fontFamily) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = highlightColor,
                                unfocusedIndicatorColor = if (isDark) Color(0x33FFFFFF) else Color(0x33000000),
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("task_title_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontFamily = fontFamily, fontSize = 20.sp)
                        )
                    }
                    
                    // Section: When?
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("When?", color = Color.Gray, fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Card
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isDark) Color(0x1F2C2D35) else Color(0x0FA2A2A2), RoundedCornerShape(14.dp))
                                    .border(1.dp, highlightColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Start Time", fontSize = 10.sp, color = Color.Gray, fontFamily = fontFamily)
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = startHour,
                                    onValueChange = {
                                        if (it.length <= 5) {
                                            startHour = it
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontFamily = fontFamily,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    ),
                                    modifier = Modifier.width(64.dp),
                                    singleLine = true,
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(highlightColor)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "-15m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { startHour = adjustTimeOffset(startHour, -15) },
                                        color = highlightColor
                                    )
                                    Text(
                                        text = "+15m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { startHour = adjustTimeOffset(startHour, 15) },
                                        color = highlightColor
                                    )
                                }
                            }
                            
                            // End Card
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isDark) Color(0x1F2C2D35) else Color(0x0FA2A2A2), RoundedCornerShape(14.dp))
                                    .border(1.dp, highlightColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("End Time", fontSize = 10.sp, color = Color.Gray, fontFamily = fontFamily)
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = endHour,
                                    onValueChange = {
                                        if (it.length <= 5) {
                                            endHour = it
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontFamily = fontFamily,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    ),
                                    modifier = Modifier.width(64.dp),
                                    singleLine = true,
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(highlightColor)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "-15m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { endHour = adjustTimeOffset(endHour, -15) },
                                        color = highlightColor
                                    )
                                    Text(
                                        text = "+15m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { endHour = adjustTimeOffset(endHour, 15) },
                                        color = highlightColor
                                    )
                                }
                            }
                        }
                        
                        // Mini date display helper that opens Calendar
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { showDatePicker = true }
                                .background(if (isDark) Color(0x0EFFFFFF) else Color(0x07000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = highlightColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = taskDate,
                                color = highlightColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                        }
                    }
                    
                    // Section: How long?
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("How long?", color = Color.Gray, fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                        
                        val currentDur = calculateDurationMinutes(startHour, endHour)
                        val durations = listOf(
                            Triple("1m", 1, "1m"),
                            Triple("15", 15, "15m"),
                            Triple("30", 30, "30m"),
                            Triple("45", 45, "45m"),
                            Triple("1h", 60, "1h"),
                            Triple("1.5h", 90, "1.5h")
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            durations.forEach { (label, minutes, desc) ->
                                val isSelected = currentDur == minutes
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) highlightColor else (if (isDark) Color(0x1F2C2D35) else Color(0x1FA2A2A2))
                                        )
                                        .clickable {
                                            endHour = adjustTimeOffset(startHour, minutes)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                                        fontFamily = fontFamily
                                    )
                                }
                            }
                        }
                    }
                    

                    // Section: How often?
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("How often?", color = Color.Gray, fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Starting on ${task?.dayDate ?: String.format(Locale.getDefault(), "%tF", Date())}",
                                color = highlightColor,
                                fontSize = 11.sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0x1F2C2D35) else Color(0x1FA2A2A2)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val repeatOptions = listOf(
                                        "Once" to "Just Today",
                                        "Daily" to "Daily",
                                        "Weekdays" to "Weekdays",
                                        "Specific Days" to "Specific Days"
                                    )
                                    
                                    repeatOptions.forEach { (label, value) ->
                                        val isSelectedOption = repeatType == value
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelectedOption) highlightColor else Color.Transparent
                                                )
                                                .clickable {
                                                    repeatType = value
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelectedOption) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                                                fontFamily = fontFamily
                                            )
                                        }
                                    }
                                }
                                      
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (repeatType) {
                                            "Daily" -> "Every day"
                                            "Weekdays" -> "Every cycle"
                                            "Specific Days" -> "Selected weekdays"
                                            else -> "Once only"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.LightGray else Color.Black,
                                        fontFamily = fontFamily
                                    )
                                }
                                     
                                if (repeatType == "Specific Days") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val weekDays = listOf(
                                            Triple("M", java.util.Calendar.MONDAY, "Monday"),
                                            Triple("T", java.util.Calendar.TUESDAY, "Tuesday"),
                                            Triple("W", java.util.Calendar.WEDNESDAY, "Wednesday"),
                                            Triple("T", java.util.Calendar.THURSDAY, "Thursday"),
                                            Triple("F", java.util.Calendar.FRIDAY, "Friday"),
                                            Triple("S", java.util.Calendar.SATURDAY, "Saturday"),
                                            Triple("S", java.util.Calendar.SUNDAY, "Sunday")
                                        )

                                        weekDays.forEach { (label, calendarVal, desc) ->
                                            val isDaySelected = selectedDays.contains(calendarVal)
                                            val dayBadgeBg = if (isDaySelected) highlightColor else (if (isDark) Color(0x11FFFFFF) else Color(0x1F000000))
                                            val dayBadgeFg = if (isDaySelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black)

                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(dayBadgeBg)
                                                    .border(1.dp, if (isDaySelected) highlightColor else Color.Gray.copy(alpha = 0.4f), CircleShape)
                                                    .clickable {
                                                        if (isDaySelected) {
                                                            selectedDays.remove(calendarVal)
                                                        } else {
                                                            selectedDays.add(calendarVal)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = dayBadgeFg,
                                                    fontFamily = fontFamily,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Section: Needs alerts?
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Needs alerts?", color = Color.Gray, fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (hasReminder) "Reminder enabled" else "No reminder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = fontFamily,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                if (hasReminder) {
                                    Text(
                                        text = if (reminderMinutes == 0) "Fires at task start time" else "Fires $reminderMinutes min before start",
                                        fontSize = 10.sp,
                                        color = highlightColor,
                                        fontFamily = fontFamily
                                    )
                                }
                            }
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { newVal ->
                                    if (newVal && !hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= 33) {
                                        showPermissionPopup = true
                                    } else {
                                        hasReminder = newVal
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = highlightColor,
                                    checkedTrackColor = highlightColor.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (hasReminder) {
                            val reminderOptions = listOf(
                                0 to "At start",
                                5 to "5 min",
                                15 to "15 min",
                                30 to "30 min",
                                60 to "1 hour"
                            )
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                reminderOptions.forEach { (mins, label) ->
                                    val isSelected = reminderMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) highlightColor else (if (isDark) Color(0x1F2C2D35) else Color(0x1FA2A2A2)))
                                            .border(1.dp, if (isSelected) highlightColor else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { reminderMinutes = mins },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White else Color.Black),
                                            fontFamily = fontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Section: Any details?
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Any details?", color = Color.Gray, fontSize = 12.sp, fontFamily = fontFamily, fontWeight = FontWeight.SemiBold)
                        
                        subtasks.forEachIndexed { idx, st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0x0F2C2D35) else Color(0x0FA2A2A2), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $st",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else Color.Black,
                                    fontFamily = fontFamily,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { subtasks.removeAt(idx) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ErrorSoft,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("Subtask item description", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = fontFamily)
                            )
                            
                            Button(
                                onClick = {
                                    if (newSubtaskText.isNotBlank()) {
                                        subtasks.add(newSubtaskText)
                                        newSubtaskText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val detailsToSave = if (subtasks.isNotEmpty()) {
                                    val jsonArr = org.json.JSONArray()
                                    subtasks.forEach { jsonArr.put(it) }
                                    jsonArr.toString()
                                } else {
                                    ""
                                }
                                
                                onConfirm(title, startHour, endHour, energy, hasReminder, reminderMinutes, repeatType, selectedDays.toList(), detailsToSave, taskDate)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(if (isEditMode) "task_dialog_update_btn" else "task_dialog_add_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = highlightColor,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "Update Task" else "Create Task",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = fontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showPermissionPopup) {
        NotificationPermissionPromptDialog(
            fontFamily = fontFamily,
            onDismissRequest = { showPermissionPopup = false },
            onConfirmEnable = {
                showPermissionPopup = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

@Composable
fun NotificationPermissionPromptDialog(
    fontFamily: FontFamily,
    onDismissRequest: () -> Unit,
    onConfirmEnable: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Alerts",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Enable Task Notifications",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = "Structured+ sends gentle reminder notifications before your scheduled timeline items. Please enable notification access in the system settings dialog.",
                fontFamily = fontFamily,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmEnable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("Enable", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", fontFamily = fontFamily, color = Color.Gray)
            }
        }
    )
}

@Composable
fun AddTaskDialog(
    fontFamily: FontFamily,
    highlightColor: Color,
    isDark: Boolean,
    initialTitle: String = "",
    initialDateStr: String = "",
    initialStartTime: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean, Int, String, List<Int>, String, String) -> Unit
) {
    StyledTaskDialog(
        isEditMode = false,
        task = null,
        fontFamily = fontFamily,
        highlightColor = highlightColor,
        isDark = isDark,
        initialTitle = initialTitle,
        initialDateStr = initialDateStr,
        initialStartTime = initialStartTime,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

// Dialog: Edit Profile Details Custom ADHD Hub
@Composable
fun EditProfileDialog(
    fontFamily: FontFamily,
    highlightColor: Color,
    isDark: Boolean,
    currentName: String,
    currentStyle: String,
    currentGoal: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Profile", fontFamily = fontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, currentStyle, currentGoal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = highlightColor, contentColor = if (isDark) Color.Black else Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Save Profile", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", fontFamily = fontFamily, color = Color.Gray)
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: TimelineTask,
    fontFamily: FontFamily,
    highlightColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean, Int, String, List<Int>, String, String) -> Unit
) {
    StyledTaskDialog(
        isEditMode = true,
        task = task,
        fontFamily = fontFamily,
        highlightColor = highlightColor,
        isDark = isDark,
        initialDateStr = task.dayDate,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePickerDialog(
    initialDateStr: String,
    fontFamily: FontFamily,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsedDate = try {
        sdf.parse(initialDateStr)
    } catch (e: Exception) {
        null
    }
    val initialMillis = parsedDate?.time ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val formatted = sdf.format(Date(selectedMillis))
                        onDateSelected(formatted)
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm", fontFamily = fontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = fontFamily, color = Color.Gray)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
