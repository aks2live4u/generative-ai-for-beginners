package com.aichiefofstaff.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aichiefofstaff.data.db.TaskPriority
import com.aichiefofstaff.ui.theme.PriorityHigh
import com.aichiefofstaff.ui.theme.PriorityLow
import com.aichiefofstaff.ui.theme.PriorityMedium
import com.aichiefofstaff.ui.theme.PriorityUrgent

@Composable
fun PriorityPill(priority: TaskPriority, modifier: Modifier = Modifier) {
    val color = priorityColor(priority)
    Text(
        text = priority.name,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.URGENT -> PriorityUrgent
    TaskPriority.HIGH -> PriorityHigh
    TaskPriority.MEDIUM -> PriorityMedium
    TaskPriority.LOW -> PriorityLow
}
