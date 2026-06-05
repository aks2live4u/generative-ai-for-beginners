package com.stockadvisor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockadvisor.data.model.StockAnalysis
import com.stockadvisor.data.network.AnthropicService
import com.stockadvisor.data.network.RateLimitException
import com.stockadvisor.data.network.YahooFinanceApi
import com.stockadvisor.data.preferences.ApiKeyRepository
import com.stockadvisor.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class AnalysisState {
    object Idle : AnalysisState()
    object LoadingMarketData : AnalysisState()
    object LoadingAIAnalysis : AnalysisState()
    data class Success(val analysis: StockAnalysis) : AnalysisState()
    data class Error(val message: String, val isRetryable: Boolean = true) : AnalysisState()
}

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val state: StateFlow<AnalysisState> = _state

    private val apiKeyRepo = ApiKeyRepository(application)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun hasApiKey(): Boolean = apiKeyRepo.hasApiKey()
    fun getApiKey(): String = apiKeyRepo.getApiKey()
    fun saveApiKey(key: String) = apiKeyRepo.saveApiKey(key)

    fun analyzeStock(symbol: String, decision: String) {
        if (symbol.isBlank()) return

        val apiKey = apiKeyRepo.getApiKey()
        if (apiKey.isBlank()) {
            _state.value = AnalysisState.Error(
                "API key not set. Tap the settings icon to add your key.",
                isRetryable = false
            )
            return
        }

        val repository = StockRepository(
            yahooFinanceApi = YahooFinanceApi(okHttpClient),
            anthropicService = AnthropicService(apiKey)
        )

        viewModelScope.launch {
            try {
                _state.value = AnalysisState.LoadingMarketData
                val stockData = repository.fetchStockData(symbol.trim().uppercase())

                _state.value = AnalysisState.LoadingAIAnalysis
                val analysis = repository.analyzeStock(stockData, decision)

                _state.value = AnalysisState.Success(analysis)
            } catch (e: RateLimitException) {
                _state.value = AnalysisState.Error(
                    "Rate limit reached. Please wait a moment and retry.",
                    isRetryable = true
                )
            } catch (e: UnknownHostException) {
                _state.value = AnalysisState.Error(
                    "No internet connection. Please check your network.",
                    isRetryable = true
                )
            } catch (e: IllegalArgumentException) {
                _state.value = AnalysisState.Error(
                    "Symbol '${symbol}' not found. Try adding .NS for Indian stocks (e.g. RELIANCE.NS).",
                    isRetryable = false
                )
            } catch (e: IOException) {
                _state.value = AnalysisState.Error(e.message ?: "Network error", isRetryable = true)
            } catch (e: Exception) {
                _state.value = AnalysisState.Error(
                    e.message ?: "Unexpected error",
                    isRetryable = true
                )
            }
        }
    }

    fun reset() {
        _state.value = AnalysisState.Idle
    }
}
