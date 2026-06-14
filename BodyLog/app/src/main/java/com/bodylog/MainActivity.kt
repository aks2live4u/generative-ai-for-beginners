package com.bodylog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pendingExportData: ByteArray? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingExportData?.let { data ->
                    contentResolver.openOutputStream(uri)?.use { it.write(data) }
                }
            }
        }
        pendingExportData = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        // Keep the WebView confined to the bundled app: only file:///android_asset/
        // pages may load, everything else (e.g. links tapped in "notes") is rejected.
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return !request.url.toString().startsWith("file:///android_asset/")
            }
        }

        webView.loadUrl("file:///android_asset/bodylog.html")
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun exportFile(base64Data: String, filename: String, mimeType: String) {
            pendingExportData = Base64.decode(base64Data, Base64.DEFAULT)
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, filename)
            }
            runOnUiThread {
                createDocumentLauncher.launch(intent)
            }
        }
    }
}
