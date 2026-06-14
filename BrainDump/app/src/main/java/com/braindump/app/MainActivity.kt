package com.braindump.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import com.braindump.app.bridge.AppBridge
import com.braindump.app.data.PreferencesStore
import com.braindump.app.data.ThoughtRepository
import com.braindump.app.files.MediaStorage
import com.braindump.app.security.BiometricHelper
import com.braindump.app.security.SecureStore
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
        private set

    private lateinit var repository: ThoughtRepository
    private lateinit var prefsStore: PreferencesStore
    private lateinit var secureStore: SecureStore
    private lateinit var mediaStorage: MediaStorage

    val biometricHelper by lazy { BiometricHelper(this) }

    // ---- Image picking ---------------------------------------------------

    private var pendingImageCallback: ((Uri?) -> Unit)? = null
    private var pendingCameraUri: Uri? = null

    private val pickVisualMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val cb = pendingImageCallback
        pendingImageCallback = null
        cb?.invoke(uri)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val cb = pendingImageCallback
        pendingImageCallback = null
        cb?.invoke(if (success) pendingCameraUri else null)
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            val cb = pendingImageCallback
            pendingImageCallback = null
            cb?.invoke(null)
        }
    }

    // ---- Audio recording ---------------------------------------------------

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0
    private var pendingRecordingCallback: ((Boolean, String?) -> Unit)? = null

    private val requestAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val cb = pendingRecordingCallback
        pendingRecordingCallback = null
        if (granted) doStartRecording(cb) else cb?.invoke(false, "Microphone permission denied")
    }

    // ---- Export / import (Storage Access Framework) -----------------------

    private var pendingExportContent: String? = null
    private var pendingExportCallback: ((Boolean, String?) -> Unit)? = null
    private var pendingImportCallback: ((String?, String?) -> Unit)? = null

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingExportContent
        val callback = pendingExportCallback
        pendingExportContent = null
        pendingExportCallback = null
        if (uri == null || content == null) {
            callback?.invoke(false, "Export cancelled")
            return@registerForActivityResult
        }
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            callback?.invoke(true, null)
        } catch (e: Exception) {
            callback?.invoke(false, e.message)
        }
    }

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = pendingImportCallback
        pendingImportCallback = null
        if (uri == null) {
            callback?.invoke(null, "Import cancelled")
            return@registerForActivityResult
        }
        try {
            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            callback?.invoke(content, null)
        } catch (e: Exception) {
            callback?.invoke(null, e.message)
        }
    }

    private var isFirstResume = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ThoughtRepository(this)
        prefsStore = PreferencesStore(this)
        secureStore = SecureStore(this)
        mediaStorage = MediaStorage(this)

        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/media/", WebViewAssetLoader.InternalStoragePathHandler(this, mediaStorage.mediaDir))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.addJavascriptInterface(
            AppBridge(this, repository, prefsStore, secureStore, mediaStorage),
            "AndroidBridge"
        )

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this) {
            webView.evaluateJavascript("(window.__handleBackButton ? window.__handleBackButton() : false)") { result ->
                if (result != "true") {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstResume) {
            webView.evaluateJavascript("window.__relock && window.__relock()", null)
        }
        isFirstResume = false
    }

    override fun onDestroy() {
        cancelRecording()
        super.onDestroy()
    }

    // -------------------------------------------------------------------
    // Image attachments
    // -------------------------------------------------------------------

    fun pickImage(source: String, callback: (Uri?) -> Unit) {
        runOnUiThread {
            pendingImageCallback = callback
            if (source == "camera") {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            } else {
                pickVisualMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    private fun launchCamera() {
        val captureDir = File(cacheDir, "captures").apply { mkdirs() }
        val file = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    // -------------------------------------------------------------------
    // Voice notes
    // -------------------------------------------------------------------

    fun startRecording(callback: (Boolean, String?) -> Unit) {
        runOnUiThread {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                doStartRecording(callback)
            } else {
                pendingRecordingCallback = callback
                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun doStartRecording(callback: ((Boolean, String?) -> Unit)?) {
        try {
            val file = mediaStorage.newAudioFile()
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            recordingFile = file
            recordingStartTime = System.currentTimeMillis()
            callback?.invoke(true, null)
        } catch (e: Exception) {
            callback?.invoke(false, e.message ?: "Recording failed")
        }
    }

    fun stopRecording(callback: (String?, Long, String?) -> Unit) {
        runOnUiThread {
            val recorder = mediaRecorder
            val file = recordingFile
            if (recorder == null || file == null) {
                callback(null, 0, "Not recording")
                return@runOnUiThread
            }
            try {
                recorder.stop()
                recorder.release()
                mediaRecorder = null
                val duration = System.currentTimeMillis() - recordingStartTime
                recordingFile = null
                callback(file.name, duration, null)
            } catch (e: Exception) {
                recorder.release()
                mediaRecorder = null
                recordingFile = null
                file.delete()
                callback(null, 0, e.message ?: "Could not save recording")
            }
        }
    }

    fun cancelRecording() {
        mediaRecorder?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaRecorder = null
        recordingFile?.delete()
        recordingFile = null
    }

    // -------------------------------------------------------------------
    // Biometrics
    // -------------------------------------------------------------------

    fun authenticateBiometric(callback: (Boolean) -> Unit) {
        runOnUiThread { biometricHelper.authenticate(callback) }
    }

    // -------------------------------------------------------------------
    // Export / import
    // -------------------------------------------------------------------

    fun exportData(content: String, callback: (Boolean, String?) -> Unit) {
        pendingExportContent = content
        pendingExportCallback = callback
        runOnUiThread {
            val fileName = "brain_dump_backup_${System.currentTimeMillis()}.json"
            createDocument.launch(fileName)
        }
    }

    fun importData(callback: (String?, String?) -> Unit) {
        pendingImportCallback = callback
        runOnUiThread { openDocument.launch(arrayOf("application/json")) }
    }

    // -------------------------------------------------------------------
    // App info
    // -------------------------------------------------------------------

    fun appVersionName(): String = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"

    fun appVersionCode(): Int {
        val info = packageManager.getPackageInfo(packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
    }
}
