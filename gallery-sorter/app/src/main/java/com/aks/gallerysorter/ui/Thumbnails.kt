package com.aks.gallerysorter.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.scan.toItem
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultIo
import com.aks.gallerysorter.vault.VaultStore
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Grid thumbnails, cached in memory only. Vault thumbnails are decrypted on the fly and never
 * written to disk. [clear] runs on lockAll (wired by AppGraph); a load that started before a
 * lock is not cached afterwards (the lock epoch is checked).
 */
class Thumbnails(
    private val source: MediaSource,
    private val store: VaultStore,
    private val lockState: LockState,
) {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    suspend fun media(row: MediaRow): Bitmap? = cached("m${row.id}") {
        source.thumbnail(row.toItem(), PX)
    }

    suspend fun vault(row: VaultRow, priv: PrivateHandle): Bitmap? {
        val file = row.thumbFile ?: return null
        val key = row.thumbWrappedKey ?: return null
        return cached("v${row.tier}${row.id}") {
            val bytes = VaultIo.decryptSmall(priv, key, store.resolve(row.tier, file))
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                bytes.fill(0)
            }
        }
    }

    fun clear() = cache.evictAll()

    private suspend fun cached(key: String, load: suspend () -> Bitmap?): Bitmap? {
        cache.get(key)?.let { return it }
        val epoch = lockState.epoch()
        val bmp = try {
            withContext(Dispatchers.IO) { load() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "thumbnail failed: ${e.javaClass.name}")
            null
        } ?: return null
        if (lockState.epoch() == epoch) cache.put(key, bmp)
        return bmp
    }

    private companion object {
        const val TAG = "Thumbnails"
        const val PX = 256
    }
}
