package com.finsight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
