package com.careerpilot.ai

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.careerpilot.ai.databinding.ActivityMainBinding

private const val TAG = "CareerPilotWebView"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Chromium (and therefore Android's WebView) refuses to execute
    // <script type="module"> -- which is what Vite emits -- when the page is
    // loaded from a file:// origin; it silently fails and you get a blank
    // white screen with no error dialog. WebViewAssetLoader serves the same
    // bundled files over a virtual https:// origin instead, which sidesteps
    // that restriction entirely (this is Google's documented fix, not a
    // workaround). See https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
        .build()

    private val homeUrl = "https://appassets.androidplatform.net/assets/www/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WebView.setWebContentsDebuggingEnabled(true)

        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidConfig")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(TAG, "${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                return true
            }
        }

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                binding.swipeRefresh.isRefreshing = false
                binding.errorView.visibility = View.GONE
                binding.webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    Log.e(TAG, "Load failed: ${error.description}")
                    binding.errorView.visibility = View.VISIBLE
                    binding.webView.visibility = View.GONE
                }
            }
        }

        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }
        binding.swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        binding.retryButton.setOnClickListener {
            binding.errorView.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE
            webView.loadUrl(homeUrl)
        }

        if (BackendConfig.isConfigured(this)) {
            webView.loadUrl(homeUrl)
        } else {
            promptForBackendUrl(showCancel = false) { webView.loadUrl(homeUrl) }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val webView = binding.webView
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                promptForBackendUrl(showCancel = true) { binding.webView.loadUrl(homeUrl) }
                true
            }
            R.id.action_reload -> {
                binding.webView.loadUrl(homeUrl)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun promptForBackendUrl(showCancel: Boolean, onSaved: () -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            hint = getString(R.string.settings_hint)
            setText(BackendConfig.getApiBaseUrl(this@MainActivity))
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setMessage(R.string.settings_hint)
            .setView(input)
            .setPositiveButton(R.string.settings_save) { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    BackendConfig.setApiBaseUrl(this, url)
                    onSaved()
                } else {
                    Toast.makeText(this, R.string.no_connection, Toast.LENGTH_LONG).show()
                    promptForBackendUrl(showCancel, onSaved)
                }
            }
            .setCancelable(showCancel)

        if (showCancel) {
            builder.setNegativeButton(R.string.settings_cancel, null)
        }
        builder.show()
    }
}
