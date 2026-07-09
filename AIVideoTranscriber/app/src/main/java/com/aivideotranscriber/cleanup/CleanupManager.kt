package com.aivideotranscriber.cleanup

import android.content.Context
import java.io.File

/**
 * Everything this app downloads or decodes on your behalf (downloaded videos, extracted audio)
 * lives only in [tempDir] and is wiped after each run - nothing is uploaded to a server, because
 * there is no server: transcription happens entirely on this device.
 */
object CleanupManager {

    fun tempDir(context: Context): File = File(context.cacheDir, "transcriber_tmp").apply { mkdirs() }

    fun clearTempFiles(context: Context) {
        tempDir(context).listFiles()?.forEach { it.deleteRecursively() }
    }
}
