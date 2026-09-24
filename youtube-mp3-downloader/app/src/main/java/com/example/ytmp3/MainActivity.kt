package com.example.ytmp3

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private var ready = false
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        downloadButton = findViewById(R.id.downloadButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.pasteButton).setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (text != null) urlInput.setText(extractUrl(text) ?: text.trim())
        }
        downloadButton.setOnClickListener { onDownloadClicked() }

        handleShare(intent)
        setUpDownloader()
    }

    // Called when you share a link to the app while it is already open.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT) ?: "")?.let { urlInput.setText(it) }
        }
    }

    private fun extractUrl(text: String): String? = Regex("https?://\\S+").find(text)?.value

    /** Unpacks yt-dlp, Python and FFmpeg, then updates yt-dlp so it keeps working with YouTube. */
    private fun setUpDownloader() {
        Thread {
            try {
                YoutubeDL.init(applicationContext)
                FFmpeg.init(applicationContext)
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Could not start the downloader: ${e.message}" }
                return@Thread
            }
            runOnUiThread { statusText.text = "Checking for downloader updates…" }
            try {
                YoutubeDL.updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.STABLE)
            } catch (e: Exception) {
                // No internet or GitHub unreachable: carry on with the bundled version.
            }
            runOnUiThread {
                ready = true
                statusText.text = "Ready. Paste a link or share one from the YouTube app."
                refreshButton()
            }
        }.start()
    }

    private fun refreshButton() {
        downloadButton.isEnabled = ready && !busy
    }

    private fun onDownloadClicked() {
        val url = urlInput.text.toString().trim()
        if (!url.startsWith("http")) {
            statusText.text = "Please paste a valid YouTube link."
            return
        }
        // Android 8 and 9 need permission to write into Downloads.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        startDownload(url)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startDownload(urlInput.text.toString().trim())
        } else {
            statusText.text = "Storage permission is needed to save the MP3."
        }
    }

    private fun startDownload(url: String) {
        busy = true
        refreshButton()
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        statusText.text = "Starting…"

        Thread {
            // Download into the app's private cache first, then copy into Downloads.
            val workDir = File(cacheDir, "work").apply { deleteRecursively(); mkdirs() }
            try {
                val request = YoutubeDLRequest(url).apply {
                    addOption("--no-playlist")
                    addOption("-x") // audio only
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "0") // best quality
                    addOption("--embed-metadata") // title / artist tags
                    addOption("-o", "${workDir.absolutePath}/%(title)s.%(ext)s")
                }
                YoutubeDL.execute(request, "yt2mp3", false) { progress, _, _ ->
                    runOnUiThread {
                        progressBar.progress = progress.toInt().coerceIn(0, 100)
                        statusText.text = if (progress >= 100f) "Converting to MP3…" else "Downloading… ${progress.toInt().coerceAtLeast(0)}%"
                    }
                }

                val mp3 = workDir.listFiles()?.firstOrNull { it.extension == "mp3" }
                    ?: throw IllegalStateException("Conversion to MP3 failed.")
                saveToDownloads(mp3)
                runOnUiThread { statusText.text = "Saved to Downloads:\n${mp3.name}" }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Download failed:\n${e.message}" }
            } finally {
                workDir.deleteRecursively()
                runOnUiThread {
                    busy = false
                    progressBar.visibility = View.GONE
                    refreshButton()
                }
            }
        }.start()
    }

    /** Copies the MP3 into the phone's public Downloads folder so music apps and Files can see it. */
    private fun saveToDownloads(mp3: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, mp3.name)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create the file in Downloads.")
            contentResolver.openOutputStream(uri)!!.use { out -> mp3.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloads.mkdirs()
            val target = File(downloads, mp3.name)
            mp3.copyTo(target, overwrite = true)
            MediaScannerConnection.scanFile(this, arrayOf(target.absolutePath), arrayOf("audio/mpeg"), null)
        }
    }
}
