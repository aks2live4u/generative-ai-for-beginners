package com.notnow.app.ui.screen.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.data.repository.ShoppingVaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ShoppingVaultViewModel(private val repo: ShoppingVaultRepository) : ViewModel() {

    val items: StateFlow<List<ShoppingVaultItem>> = repo.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markPurchased(id: Long) = viewModelScope.launch { repo.markPurchased(id) }
    fun remove(id: Long) = viewModelScope.launch { repo.markRemoved(id) }
    fun add(title: String, price: String = "", url: String = "") = viewModelScope.launch {
        repo.save(ShoppingVaultItem(title = title, price = price, url = url))
    }

    fun ageLabel(savedAt: Long): String {
        val diff = System.currentTimeMillis() - savedAt
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        return when {
            days >= 1 -> "$days day${if (days > 1) "s" else ""} ago"
            hours >= 1 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            else -> "just now"
        }
    }
}

class ShoppingVaultViewModelFactory(private val repo: ShoppingVaultRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return ShoppingVaultViewModel(repo) as T
    }
}
