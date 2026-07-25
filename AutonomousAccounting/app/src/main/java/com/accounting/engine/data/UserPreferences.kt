package com.accounting.engine.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "accounting_engine_preferences")

/** User-tunable automation settings, persisted (unencrypted - contains no financial data) via DataStore. */
class UserPreferences(private val context: Context) {

    private object Keys {
        val CONTINGENCY_RESERVE_PERCENTAGE = doublePreferencesKey("contingency_reserve_percentage")
    }

    val contingencyReservePercentage: Flow<Double> =
        context.dataStore.data.map { it[Keys.CONTINGENCY_RESERVE_PERCENTAGE] ?: DEFAULT_RESERVE_PERCENTAGE }

    suspend fun setContingencyReservePercentage(percentage: Double) {
        context.dataStore.edit { it[Keys.CONTINGENCY_RESERVE_PERCENTAGE] = percentage }
    }

    companion object {
        const val DEFAULT_RESERVE_PERCENTAGE = 10.0
    }
}
