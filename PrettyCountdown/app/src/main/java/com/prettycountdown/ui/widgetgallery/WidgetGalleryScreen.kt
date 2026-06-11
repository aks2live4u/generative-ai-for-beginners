package com.prettycountdown.ui.widgetgallery

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.widget.CountdownWidgetReceiver
import com.prettycountdown.widget.DashboardWidgetReceiver

/** Showcases every widget style and lets the user pin a widget to their home screen. */
@Composable
fun WidgetGalleryScreen() {
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Widget Gallery") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Pretty Countdown widgets are the whole point - pin one for any event and watch it come alive on your home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedButton(onClick = { pinWidget(context, CountdownWidgetReceiver::class.java) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Widgets, contentDescription = null)
                    Text("  Add Countdown Widget")
                }
            }
            item {
                OutlinedButton(onClick = { pinWidget(context, DashboardWidgetReceiver::class.java) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Dashboard, contentDescription = null)
                    Text("  Add Dashboard Widget (all events)")
                }
            }
            item {
                Text("Available Styles", style = MaterialTheme.typography.titleMedium)
            }
            items(WidgetStyle.entries) { style ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(style.emoji, style = MaterialTheme.typography.headlineMedium)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(style.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                style.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pinWidget(context: Context, receiver: Class<*>) {
    val widgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, receiver)
    if (widgetManager.isRequestPinAppWidgetSupported) {
        widgetManager.requestPinAppWidget(provider, null, null)
    } else {
        Toast.makeText(context, "Add a Pretty Countdown widget from your launcher's widget picker.", Toast.LENGTH_LONG).show()
    }
}
