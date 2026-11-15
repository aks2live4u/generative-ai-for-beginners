package com.aks.gallerysorter.ui.viewer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.entities.VaultRow
import com.aks.gallerysorter.vault.Argon2Params
import com.aks.gallerysorter.vault.FakeKeyWrapper
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultCrypto
import com.aks.gallerysorter.vault.VaultKeys
import com.aks.gallerysorter.vault.VaultStore
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Random
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VaultDataSourceTest {
    @get:Rule val tmp = TemporaryFolder()

    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private val plain = ByteArray(3_000_000).also { Random(7).nextBytes(it) }
    private lateinit var store: VaultStore
    private lateinit var priv: PrivateHandle
    private lateinit var row: VaultRow

    @Before fun setUp() {
        store = VaultStore(File(tmp.root, "std"), File(tmp.root, "hs"))
        store.ensure(VaultTier.STANDARD)
        val keys = VaultKeys(VaultKeys.keysDir(store.root(VaultTier.STANDARD)), FakeKeyWrapper(), fast)
        keys.create("123456".toCharArray())
        priv = keys.unlockWithSecret("123456".toCharArray())
        val out = ByteArrayOutputStream()
        val wrapped = VaultCrypto.encrypt(keys.publicHandle(), ByteArrayInputStream(plain), out)
        store.resolve(VaultTier.STANDARD, "files/a.enc").writeBytes(out.toByteArray())
        row = VaultRow(
            id = 1, originalPath = "/x", tier = VaultTier.STANDARD, vaultFile = "files/a.enc",
            kind = MediaKind.VIDEO, sizeBytes = plain.size.toLong(), categories = "", wrappedFileKey = wrapped,
            thumbFile = null, thumbWrappedKey = null, plainSha256 = "", verified = true,
        )
    }

    /** Reads until END_OF_INPUT with an odd buffer size, as ExoPlayer's extractors do. */
    private fun readAll(source: VaultDataSource): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(70_001)
        while (true) {
            val n = source.read(buf, 0, buf.size)
            if (n == C.RESULT_END_OF_INPUT) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    @Test fun readFromOffset_equalsPlaintextSlice() {
        val source = VaultDataSource(priv, row, store)
        val length = source.open(DataSpec.Builder().setUri(Uri.parse("vault://1")).setPosition(1_500_000).build())

        assertThat(length).isEqualTo(1_500_000L)
        assertThat(readAll(source)).isEqualTo(plain.copyOfRange(1_500_000, plain.size))
        source.close()
    }

    @Test fun boundedRead_stopsAtRequestedLength() {
        val source = VaultDataSource(priv, row, store)
        val spec = DataSpec.Builder().setUri(Uri.parse("vault://1")).setPosition(999_000).setLength(5_000).build()

        assertThat(source.open(spec)).isEqualTo(5_000L)
        assertThat(readAll(source)).isEqualTo(plain.copyOfRange(999_000, 1_004_000))
        source.close()
    }
}
