package com.aks.gallerysorter.core

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Without ACCESS_MEDIA_LOCATION, opening the original throws; EXIF must still be read (without GPS). */
@RunWith(RobolectricTestRunner::class)
class MediaStoreSourceExifTest {
    private val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
    private val item = MediaItem(
        id = 7, path = "/storage/emulated/0/DCIM/Camera/a.jpg", kind = MediaKind.IMAGE, sizeBytes = 100,
        mime = "image/jpeg", width = 1, height = 1, dateTakenMs = null, durationMs = null, generation = 1,
    )
    private val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 7)

    @Test fun exif_requireOriginalUnsupported_fallsBackToPlainUri() = runBlocking<Unit> {
        val shadow = shadowOf(resolver)
        shadow.registerInputStreamSupplier(MediaStore.setRequireOriginal(uri)) {
            throw UnsupportedOperationException("caller lacks ACCESS_MEDIA_LOCATION")
        }
        shadow.registerInputStreamSupplier(uri) { ByteArrayInputStream(jpegWithMake("vivo")) }

        val exif = MediaStoreSource(resolver).exif(item)

        assertThat(exif).isNotNull()
        assertThat(exif!!.make).isEqualTo("vivo")
        assertThat(exif.lat).isNull()
        assertThat(exif.lon).isNull()
    }

    /** A minimal JPEG: SOI, an APP1 Exif segment with IFD0 holding only Make, EOI. */
    private fun jpegWithMake(make: String): ByteArray {
        val value = (make + "\u0000").toByteArray(Charsets.US_ASCII)
        val tiff = ByteArrayOutputStream().also { b ->
            DataOutputStream(b).use { o ->
                o.writeBytes("MM"); o.writeShort(42); o.writeInt(8) // big-endian header, IFD0 at 8
                o.writeShort(1) // one entry
                o.writeShort(0x010F); o.writeShort(2); o.writeInt(value.size); o.writeInt(8 + 2 + 12 + 4)
                o.writeInt(0) // no next IFD
                o.write(value)
            }
        }.toByteArray()
        val app1 = "Exif\u0000\u0000".toByteArray(Charsets.US_ASCII) + tiff
        return ByteArrayOutputStream().also { b ->
            DataOutputStream(b).use { o ->
                o.writeShort(0xFFD8)
                o.writeShort(0xFFE1); o.writeShort(app1.size + 2); o.write(app1)
                o.writeShort(0xFFD9)
            }
        }.toByteArray()
    }
}
