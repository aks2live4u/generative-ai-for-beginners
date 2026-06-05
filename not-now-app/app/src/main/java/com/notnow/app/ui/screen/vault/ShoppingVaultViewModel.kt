package com.notnow.app.ui.screen.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.ShoppingVaultItem
import com.notnow.app.data.repository.ShoppingVaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingVaultViewModel(private val repo: ShoppingVaultRepository) : ViewModel() {

    val activeItems: StateFlow<List<ShoppingVaultItem>> = repo.activeItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchasedItems: StateFlow<List<ShoppingVaultItem>> = repo.purchasedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(title: String, url: String, price: String) = viewModelScope.launch {
        repo.save(ShoppingVaultItem(title = title, url = url, price = price))
    }

    fun markPurchased(id: Long) = viewModelScope.launch {
        repo.markPurchased(id)
    }

    fun delete(item: ShoppingVaultItem) = viewModelScope.launch {
        repo.delete(item)
    }

    class Factory(private val repo: ShoppingVaultRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShoppingVaultViewModel(repo) as T
    }
}
