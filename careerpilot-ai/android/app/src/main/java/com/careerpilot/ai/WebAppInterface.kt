package com.careerpilot.ai

import android.content.Context
import android.webkit.JavascriptInterface

/**
 * Bridges the native "backend URL" setting into the bundled web app.
 * frontend/src/api/client.ts checks `window.AndroidConfig.getApiBaseUrl()`
 * before falling back to its build-time default, so changing the backend
 * URL here takes effect on the next page load with no app rebuild needed.
 */
class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun getApiBaseUrl(): String {
        return BackendConfig.getApiBaseUrl(context)
    }
}
