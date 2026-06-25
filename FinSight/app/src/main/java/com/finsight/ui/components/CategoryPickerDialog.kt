package com.finsight.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.finsight.core.model.Category

/**
 * Tap-to-reclassify picker: a scrollable list of every [Category] the user can move a transaction
 * (or every transaction in a whole category) into. Used both for single-transaction reclassification
 * (tapping a row in Transactions) and bulk reclassification (tapping a category card on Dashboard/
 * Transactions, e.g. moving every wrongly-bucketed "Miscellaneous" entry somewhere more specific).
 */
@Composable
fun CategoryPickerDialog(
    title: String,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(Category.entries) { category ->
                    CategoryRow(category = category, onClick = { onCategorySelected(category) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = CircleShape,
                color = colorForGroup(category.group).copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconForGroup(category.group),
                        contentDescription = null,
                        tint = colorForGroup(category.group),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
