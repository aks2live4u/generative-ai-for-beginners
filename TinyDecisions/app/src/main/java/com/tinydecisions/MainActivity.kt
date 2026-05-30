package com.tinydecisions

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge immersive display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            // Smooth scrolling and rendering
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // Hide status bar for full immersion
        hideSystemUI()

        // Expose native haptic feedback to JavaScript
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JS to wire up native haptic calls
                view?.evaluateJavascript("""
                    (function() {
                        // Patch navigator.vibrate to use native Android vibration
                        if (window.AndroidBridge) {
                            window._nativeVibrate = function(pattern) {
                                if (Array.isArray(pattern)) {
                                    AndroidBridge.vibrate(pattern.join(','));
                                } else {
                                    AndroidBridge.vibrate(String(pattern));
                                }
                            };
                        }
                    })();
                """.trimIndent(), null)
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("file:///android_asset/tinydecisions.html")
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class AndroidBridge(private val context: Context) {

        @JavascriptInterface
        fun vibrate(patternStr: String) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (patternStr.contains(',')) {
                    // Pattern vibration (on, off, on, off...)
                    val parts = patternStr.split(',').mapNotNull { it.trim().toLongOrNull() }
                    if (parts.isNotEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val timings = parts.toLongArray()
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(parts.toLongArray(), -1)
                        }
                    }
                } else {
                    val duration = patternStr.trim().toLongOrNull() ?: 50L
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
            } catch (_: Exception) {
                // Vibration not available on this device — silently ignore
            }
        }

        @JavascriptInterface
        fun hapticLight() {
            vibrate("30")
        }

        @JavascriptInterface
        fun hapticMedium() {
            vibrate("60")
        }

        @JavascriptInterface
        fun hapticHeavy() {
            vibrate("100")
        }

        @JavascriptInterface
        fun hapticSuccess() {
            vibrate("40,40,80")
        }
    }
}
