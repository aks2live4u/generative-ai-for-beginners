package com.nexus.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nexus.launcher.ui.NexusApp
import com.nexus.launcher.ui.theme.NexusTheme

class MainActivity : ComponentActivity() {
    private val graph get() = (application as NexusApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            NexusTheme { NexusApp(graph) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while the launcher is showing returns to the Home screen.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) graph.homePresses.tryEmit(Unit)
    }

    override fun onResume() {
        super.onResume()
        graph.apps.refreshIfStale()
    }
}
