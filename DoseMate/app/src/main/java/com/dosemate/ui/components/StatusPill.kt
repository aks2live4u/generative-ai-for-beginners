package com.dosemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosemate.data.LogStatus
import com.dosemate.ui.theme.StatusMissed
import com.dosemate.ui.theme.StatusPending
import com.dosemate.ui.theme.StatusTaken

fun statusColor(status: LogStatus): Color = when (status) {
    LogStatus.TAKEN -> StatusTaken
    LogStatus.MISSED -> StatusMissed
    LogStatus.SKIPPED -> StatusMissed
    LogStatus.PENDING -> StatusPending
}

fun statusLabel(status: LogStatus): String = when (status) {
    LogStatus.TAKEN -> "Taken"
    LogStatus.MISSED -> "Missed"
    LogStatus.SKIPPED -> "Skipped"
    LogStatus.PENDING -> "Pending"
}

@Composable
fun StatusPill(status: LogStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Text(
        text = statusLabel(status),
        color = color,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(PaddingValues(horizontal = 10.dp, vertical = 4.dp))
    )
}
