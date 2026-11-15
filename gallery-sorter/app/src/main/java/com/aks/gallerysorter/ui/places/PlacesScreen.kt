package com.aks.gallerysorter.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aks.gallerysorter.R
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.SecureWhile
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.grid.GridSource

/** Cities (most items first) and trips; each opens a grid of its (visible) items. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(onOpen: (GridSource) -> Unit, onBack: () -> Unit) {
    val vm = graphViewModel { g -> PlacesViewModel({ g.db }, { g.scanSettings }, g.lockState) }
    val state by vm.state.collectAsState()
    val unlocked by LocalContext.current.graph.lockState.includeLocked.collectAsState()
    SecureWhile(unlocked)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            if (state.trips.isNotEmpty()) {
                item { Header("Trips") }
                items(state.trips, key = { "t${it.trip.startMs}" }) { t ->
                    ListItem(
                        headlineContent = { Text(t.trip.name) },
                        supportingContent = { Text("${t.count} items") },
                        modifier = Modifier.clickable { onOpen(GridSource.OfTrip(t.trip.startMs)) },
                    )
                }
            }
            item { Header("Cities") }
            if (state.loaded && state.cities.isEmpty()) item { Text("No places yet", Modifier.padding(16.dp)) }
            items(state.cities, key = { "c${it.city}" }) { c ->
                ListItem(
                    headlineContent = { Text(c.city) },
                    supportingContent = { Text("${c.n} items") },
                    modifier = Modifier.clickable { onOpen(GridSource.OfCity(c.city)) },
                )
            }
            item { Text(stringResource(R.string.geonames_credit), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}
