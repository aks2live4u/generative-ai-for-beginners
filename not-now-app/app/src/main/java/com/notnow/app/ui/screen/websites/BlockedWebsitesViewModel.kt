package com.notnow.app.ui.screen.websites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.BlockedWebsite
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.repository.BlockedWebsiteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockedWebsitesViewModel(private val repo: BlockedWebsiteRepository) : ViewModel() {

    val sites = repo.allSites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSite(domain: String, label: String, level: FrictionLevel) {
        val cleaned = domain.trim()
            .removePrefix("https://").removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .lowercase()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            repo.add(BlockedWebsite(domain = cleaned, label = label.ifBlank { cleaned }, frictionLevel = level))
        }
    }

    fun deleteSite(site: BlockedWebsite) {
        viewModelScope.launch { repo.delete(site) }
    }

    fun toggleSite(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setEnabled(id, enabled) }
    }

    class Factory(private val repo: BlockedWebsiteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = BlockedWebsitesViewModel(repo) as T
    }
}
