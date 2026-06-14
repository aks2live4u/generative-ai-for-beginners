package com.bodylog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingExportData: ByteArray? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null) {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else {
                data?.data?.let { arrayOf(it) }
            }
        } else null
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                }
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }

        // BodyLog's Export button creates a Blob + <a download>, which WebView
        // can't save directly. Intercept it and hand the data to AndroidBridge
        // so it can be saved via the system "Save As" dialog.
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            if (url.startsWith("blob:")) {
                val filename = parseFilename(contentDisposition) ?: "bodylog-backup.json"
                val type = mimeType?.takeIf { it.isNotBlank() } ?: "application/json"
                webView.evaluateJavascript(
                    """
                    (function() {
                        fetch('$url').then(function(r){ return r.blob(); }).then(function(blob){
                            var reader = new FileReader();
                            reader.onloadend = function() {
                                AndroidBridge.exportFile(reader.result.split(',')[1], '$filename', '$type');
                            };
                            reader.readAsDataURL(blob);
                        });
                    })();
                    """.trimIndent(), null
                )
            }
        }

        webView.loadUrl("file:///android_asset/bodylog.html")
    }

    private fun parseFilename(contentDisposition: String?): String? {
        if (contentDisposition == null) return null
        return Regex("filename=\"?([^\";]+)\"?").find(contentDisposition)?.groupValues?.get(1)
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
