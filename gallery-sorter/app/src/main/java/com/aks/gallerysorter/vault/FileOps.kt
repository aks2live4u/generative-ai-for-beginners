package com.aks.gallerysorter.vault

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import java.io.File

/** The file-system and MediaStore effects of hiding/unhiding gallery files (faked in tests). */
interface FileOps {
    fun exists(path: String): Boolean
    fun freeBytes(dir: File): Long

    /** Deletes a gallery original; true only if it is gone afterwards. */
    fun deleteOriginal(path: String): Boolean
    fun scanIntoMediaStore(path: String)
    fun removeFromMediaStore(id: Long)
}

/** Direct file access (needs all-files access), MediaScanner for adds, ContentResolver for removals. */
class AndroidFileOps(context: Context) : FileOps {
    private val app = context.applicationContext

    override fun exists(path: String): Boolean = File(path).exists()

    override fun freeBytes(dir: File): Long = dir.usableSpace

    override fun deleteOriginal(path: String): Boolean {
        val f = File(path)
        return f.delete() && !f.exists()
    }

    override fun scanIntoMediaStore(path: String) {
        MediaScannerConnection.scanFile(app, arrayOf(path), null, null)
    }

    override fun removeFromMediaStore(id: Long) {
        val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), id)
        app.contentResolver.delete(uri, null, null)
    }
}
