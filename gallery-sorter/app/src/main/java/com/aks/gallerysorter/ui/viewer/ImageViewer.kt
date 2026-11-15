package com.aks.gallerysorter.ui.viewer

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultCrypto
import com.aks.gallerysorter.vault.VaultStore
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_PX = 2560

/**
 * A full-screen image with pinch zoom (1–5x), pan while zoomed and double-tap to toggle 2x.
 * Single-finger drags at 1x are left unconsumed so the surrounding pager can swipe.
 */
@Composable
fun ImageViewer(key: Any, load: suspend () -> Bitmap?) {
    val bmp by produceState<Bitmap?>(null, key) {
        value = try {
            withContext(Dispatchers.IO) { load() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ImageViewer", "decode failed: ${e.javaClass.name}")
            null
        }
    }
    var scale by remember(key) { mutableFloatStateOf(1f) }
    var offset by remember(key) { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(key) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + event.calculatePan()
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(key) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2f
                })
            },
    ) {
        val b = bmp
        if (b == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            Image(
                b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y,
                ),
            )
        }
    }
}

private fun ImageDecoder.Source.decodeBounded(): Bitmap = ImageDecoder.decodeBitmap(this) { decoder, info, _ ->
    val longest = maxOf(info.size.width, info.size.height)
    if (longest > MAX_PX) {
        val f = MAX_PX.toFloat() / longest
        decoder.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
    }
}

/** A gallery image, decoded through MediaStore and downscaled to at most [MAX_PX]. */
fun loadGalleryImage(context: Context, row: MediaRow): Bitmap {
    val base = if (row.kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    return ImageDecoder.createSource(context.contentResolver, ContentUris.withAppendedId(base, row.id)).decodeBounded()
}

/** A vault image, decrypted into memory only (never to disk); the plaintext buffer is zeroed after decoding. */
fun loadVaultImage(store: VaultStore, priv: PrivateHandle, row: VaultRow): Bitmap {
    val bytes = VaultCrypto.decryptingStream(priv, row.wrappedFileKey, store.resolve(row.tier, row.vaultFile)).use { ch ->
        val size = ch.size()
        require(size <= Int.MAX_VALUE) { "image too large" }
        val buf = ByteBuffer.allocate(size.toInt())
        while (buf.hasRemaining() && ch.read(buf) >= 0) Unit
        buf.array()
    }
    try {
        return ImageDecoder.createSource(ByteBuffer.wrap(bytes)).decodeBounded()
    } finally {
        bytes.fill(0)
    }
}
