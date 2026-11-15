package com.aks.gallerysorter.ui.settings

import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aks.gallerysorter.AppGraph
import com.aks.gallerysorter.scan.toItem
import com.aks.gallerysorter.ui.graphViewModel
import com.aks.gallerysorter.ui.recategorize
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [models]: camera models found in DCIM/Camera, most photos first; [mine]: the ones chosen as own phones. */
data class PastPhonesState(val loaded: Boolean = false, val models: List<String> = emptyList(), val mine: Set<String> = emptySet())

/**
 * "My past phones": the EXIF camera models of DCIM/Camera photos (this phone always counts as
 * yours). Saved as ScanSettings own camera models; items shot on a model whose choice changed are
 * re-categorized (My nudes → Taken on my camera depends on it).
 */
class PastPhonesViewModel(private val graph: AppGraph) : ViewModel() {
    private val _state = MutableStateFlow(PastPhonesState())
    val state: StateFlow<PastPhonesState> = _state.asStateFlow()
    private var idsByModel: Map<String, List<Long>> = emptyMap()
    private var saved: Set<String> = emptySet()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val rows = graph.db.mediaDao().cameraImages()
            // EXIF reads are per file; a large library is sampled evenly (past phones span years).
            val sample = if (rows.size <= MAX_READS) rows else rows.indices.step(rows.size / MAX_READS + 1).map { rows[it] }
            val byModel = HashMap<String, MutableList<Long>>()
            for (row in sample) {
                val model = try {
                    graph.mediaSource.exif(row.toItem())?.model?.trim()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (!model.isNullOrEmpty() && !model.equals(Build.MODEL, ignoreCase = true)) byModel.getOrPut(model) { mutableListOf() } += row.id
            }
            idsByModel = byModel
            saved = graph.scanSettings.ownCameraModels().filterNot { it.equals(Build.MODEL, ignoreCase = true) }.toSet()
            val models = byModel.entries.sortedByDescending { it.value.size }.map { it.key }
            _state.value = PastPhonesState(loaded = true, models = (models + saved).distinct(), mine = saved)
        }
    }

    fun toggle(model: String) {
        val s = _state.value
        _state.value = s.copy(mine = if (model in s.mine) s.mine - model else s.mine + model)
    }

    fun save() {
        val mine = _state.value.mine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                graph.scanSettings.setOwnCameraModels(mine)
                val changed = (mine - saved) + (saved - mine)
                saved = mine
                recategorize(graph, changed.flatMap { idsByModel[it].orEmpty() }.toSet())
                graph.notify("Saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("PastPhones", "save failed: ${e.javaClass.name}")
                graph.notify("Couldn't save")
            }
        }
    }

    private companion object {
        const val MAX_READS = 2000
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastPhonesScreen(onBack: () -> Unit) {
    val vm = graphViewModel { PastPhonesViewModel(it) }
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My past phones") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (state.loaded) TextButton(onClick = { vm.save(); onBack() }) { Text("Save") } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !state.loaded -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Reading camera models…", Modifier.padding(top = 8.dp))
                }
                state.models.isEmpty() -> Text("No other camera models found in DCIM/Camera", Modifier.align(Alignment.Center).padding(16.dp))
                else -> LazyColumn {
                    item { Text("This phone (${Build.MODEL}) always counts as yours.", Modifier.padding(16.dp)) }
                    items(state.models, key = { it }) { m ->
                        ListItem(
                            headlineContent = { Text(m) },
                            trailingContent = { Checkbox(checked = m in state.mine, onCheckedChange = { vm.toggle(m) }) },
                            modifier = Modifier.clickable { vm.toggle(m) },
                        )
                    }
                }
            }
        }
    }
}
