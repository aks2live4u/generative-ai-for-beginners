package com.nexus.launcher.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.launcher.data.UsageAccess
import com.nexus.launcher.ui.components.GlassCard
import com.nexus.launcher.ui.theme.Nx

/** Shown until usage access is on: without it NEXUS only knows apps opened through NEXUS. */
@Composable
fun UsageAccessNudge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    GlassCard(modifier.fillMaxWidth(), onClick = { UsageAccess.openSettings(context) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.QueryStats, contentDescription = null, tint = Nx.Cyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Rank apps by how you really use them", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap, find NEXUS, and turn on Usage access.",
                    style = MaterialTheme.typography.bodyMedium, color = Nx.Muted,
                )
            }
        }
    }
}
