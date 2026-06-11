package com.prettycountdown.ui.widgetgallery

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prettycountdown.data.model.WidgetStyle
import com.prettycountdown.widget.CountdownWidgetReceiver
import com.prettycountdown.widget.DashboardWidgetReceiver
import kotlinx.coroutines.delay

/** Showcases every widget style and lets the user pin a widget to their home screen. */
@OptIn(ExperimentalMaterial3Api::class)
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
                OutlinedButton(onClick = { pinWidget(context, CountdownWidgetReceiver::class.java) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Filled.Widgets, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Countdown Widget")
                }
            }
            item {
                OutlinedButton(onClick = { pinWidget(context, DashboardWidgetReceiver::class.java) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Filled.Dashboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Dashboard Widget (all events)")
                }
            }
            item {
                Text("Available Styles", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            itemsIndexed(WidgetStyle.entries.toList()) { index, style ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(style) {
                    delay(index * 60L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WidgetStylePreview(style = style, modifier = Modifier.size(width = 120.dp, height = 90.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(style.emoji, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(style.displayName, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
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
