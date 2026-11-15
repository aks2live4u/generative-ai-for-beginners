package com.aks.gallerysorter.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.SecureWhile
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Stand-in for screens that arrive in Task 14b (Onboarding, People, Review, Duplicates, Settings, About, Places). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoon(title: String, onBack: () -> Unit) {
    // Same rule as every screen: secure while any vault is unlocked (14b screens show thumbnails).
    val unlocked by LocalContext.current.graph.lockState.includeLocked.collectAsState()
    SecureWhile(unlocked)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { Text("Coming soon", Modifier.align(Alignment.Center)) }
    }
}
