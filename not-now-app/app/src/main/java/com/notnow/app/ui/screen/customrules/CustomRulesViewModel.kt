package com.notnow.app.ui.screen.customrules

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notnow.app.data.entity.AppCategory
import com.notnow.app.data.entity.AppRule
import com.notnow.app.data.entity.FrictionLevel
import com.notnow.app.data.repository.AppRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val appName: String)

class CustomRulesViewModel(
    private val repo: AppRuleRepository,
    private val context: Context
) : ViewModel() {

    val allRules = repo.allRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.appName.lowercase() }
    }

    fun addRule(pkg: InstalledApp, frictionLevel: FrictionLevel) {
        viewModelScope.launch {
            repo.upsert(
                AppRule(
                    packageName       = pkg.packageName,
                    appName           = pkg.appName,
                    category          = AppCategory.OTHER,
                    frictionLevel     = frictionLevel,
                    blockedInFocusMode = true,
                    blockedAtNight    = true,
                    isEnabled         = true
                )
            )
        }
    }

    fun deleteRule(rule: AppRule) {
        viewModelScope.launch { repo.delete(rule) }
    }

    fun toggleRule(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repo.setEnabled(packageName, enabled) }
    }

    class Factory(private val repo: AppRuleRepository, private val context: Context) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            CustomRulesViewModel(repo, context) as T
    }
}
