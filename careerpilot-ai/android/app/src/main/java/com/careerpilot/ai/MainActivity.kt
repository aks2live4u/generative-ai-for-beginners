package com.careerpilot.ai

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.careerpilot.ai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val homeUrl = "file:///android_asset/www/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            databaseEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidConfig")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }
        binding.swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        if (BackendConfig.isConfigured(this)) {
            webView.loadUrl(homeUrl)
        } else {
            promptForBackendUrl(showCancel = false) { webView.loadUrl(homeUrl) }
        }
    }

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
                binding.webView.reload()
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
