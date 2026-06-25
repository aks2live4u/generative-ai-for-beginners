package com.finsight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finsight.ui.state.TimePeriod

/** Lets the user pick whether Dashboard/Transactions figures are scoped to a week, month, year, or all time. */
@Composable
fun PeriodSelector(
    selected: TimePeriod,
    onSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelected(period) },
                label = {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

/** Compact "This Month ▾" pill that opens a dropdown to pick the period - used inline in card headers. */
@Composable
fun PeriodDropdown(
    selected: TimePeriod,
    onSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.label) },
                    onClick = {
                        onSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}
