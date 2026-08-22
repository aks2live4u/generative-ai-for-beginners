package com.nextaction

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

/**
 * Hosts the Next Action web app (single-page HTML/JS, self-contained) inside a
 * WebView. All planning logic lives in JS and talks to the Gemini API directly
 * over HTTPS; this shell only adds native touches (haptics, mic permission,
 * back navigation) on top of the same UI a desktop browser would render.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: WebView re-checks on next getUserMedia call */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // Expose native haptic feedback to JavaScript
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                // The app only ever asks for microphone access, for voice capture
                // in the Inbox. Grant it once the OS-level RECORD_AUDIO permission
                // is held; otherwise deny so the page falls back to typing.
                val needsAudio = request.resources.any { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }
                if (needsAudio && hasMicPermission()) {
                    runOnUiThread { request.grant(request.resources) }
                } else {
                    runOnUiThread { request.deny() }
                }
            }
        }

        if (!hasMicPermission()) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        webView.loadUrl("file:///android_asset/nextaction.html")
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

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
                    val parts = patternStr.split(',').mapNotNull { it.trim().toLongOrNull() }
                    if (parts.isNotEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(parts.toLongArray(), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(parts.toLongArray(), -1)
                        }
                    }
                } else {
                    val duration = patternStr.trim().toLongOrNull() ?: 30L
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
            vibrate("25")
        }

        @JavascriptInterface
        fun hapticSuccess() {
            vibrate("30,40,60")
        }
    }
}
