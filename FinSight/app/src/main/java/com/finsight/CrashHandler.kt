package com.finsight

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Persists the last uncaught exception to a file so it can be read from the Settings screen.
 * Sideloaded debug builds have no Play Console/Crashlytics pipeline and the user may not have
 * adb access, so without this a crash is otherwise just an invisible process death.
 */
object CrashHandler {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                File(appContext.filesDir, FILE_NAME).writeText(writer.toString())
            } catch (_: Exception) {
                // Best-effort only - never let crash logging itself block the real crash handling.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrash(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE_NAME).delete()
    }
}
