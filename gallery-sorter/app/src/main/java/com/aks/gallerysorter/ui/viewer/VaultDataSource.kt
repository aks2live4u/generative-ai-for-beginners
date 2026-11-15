package com.aks.gallerysorter.ui.viewer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultCrypto
import com.aks.gallerysorter.vault.VaultStore
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.security.GeneralSecurityException

/**
 * Plays a vault item straight from its ciphertext: [VaultCrypto.decryptingStream] gives a
 * seekable, segment-authenticated channel, so ExoPlayer can seek without any plaintext on disk.
 * [store] resolves the row's tier-relative file.
 */
@OptIn(markerClass = [UnstableApi::class])
class VaultDataSource(
    private val priv: PrivateHandle,
    private val row: VaultRow,
    private val store: VaultStore,
) : BaseDataSource(/* isNetwork = */ false) {
    private var channel: SeekableByteChannel? = null
    private var uri: Uri? = null
    private var bytesRemaining = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        val ch = try {
            VaultCrypto.decryptingStream(priv, row.wrappedFileKey, store.resolve(row.tier, row.vaultFile))
        } catch (e: IOException) {
            throw DataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        } catch (e: GeneralSecurityException) {
            throw DataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        channel = ch
        val size = ch.size()
        if (dataSpec.position > size) {
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }
        ch.position(dataSpec.position)
        val available = size - dataSpec.position
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) available else minOf(dataSpec.length, available)
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val ch = channel ?: throw IllegalStateException("not opened")
        val want = minOf(length.toLong(), bytesRemaining).toInt()
        val dst = ByteBuffer.wrap(buffer, offset, want)
        val n = try {
            // A channel read may return 0; the DataSource contract wants at least one byte.
            var r = 0
            var zeros = 0
            while (r == 0) {
                r = ch.read(dst)
                if (r == 0 && ++zeros >= MAX_ZERO_READS) throw IOException("vault read stalled")
            }
            r
        } catch (e: IOException) {
            throw DataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (n < 0) return C.RESULT_END_OF_INPUT
        bytesRemaining -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            channel?.close()
        } catch (e: IOException) {
            throw DataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        } finally {
            channel = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }
}
