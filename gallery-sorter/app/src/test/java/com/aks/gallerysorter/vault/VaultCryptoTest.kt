package com.aks.gallerysorter.vault

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.security.GeneralSecurityException
import java.util.Random
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

class VaultCryptoTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val timeout: Timeout = Timeout.seconds(120)

    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private lateinit var keysDir: File
    private lateinit var keys: VaultKeys
    private lateinit var priv: PrivateHandle

    @Before fun setUp() {
        keysDir = File(tmp.root, "keys")
        keys = VaultKeys(keysDir, FakeKeyWrapper(), fast)
        keys.create("123456".toCharArray())
        priv = keys.unlockWithSecret("123456".toCharArray())
    }

    private fun random(n: Int) = ByteArray(n).also { Random(n.toLong()).nextBytes(it) }

    private class Encrypted(val file: File, val wrappedKey: ByteArray)

    private fun encrypt(data: ByteArray, pub: PublicHandle = keys.publicHandle()): Encrypted {
        val out = ByteArrayOutputStream()
        val wrapped = VaultCrypto.encrypt(pub, ByteArrayInputStream(data), out)
        val f = tmp.newFile()
        f.writeBytes(out.toByteArray())
        return Encrypted(f, wrapped)
    }

    private fun readAll(ch: SeekableByteChannel): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteBuffer.allocate(64 * 1024)
        while (ch.read(buf) >= 0) {
            out.write(buf.array(), 0, buf.position())
            buf.clear()
        }
        return out.toByteArray()
    }

    private fun decrypt(e: Encrypted, p: PrivateHandle = priv): ByteArray =
        VaultCrypto.decryptingStream(p, e.wrappedKey, e.file).use { readAll(it) }

    @Test fun expectedCiphertextSizeMatchesActualAtSegmentEdges() {
        // First segment holds 1 MB - 16 (tag) - 40 (header) bytes; later ones 1 MB - 16.
        val first = 1048576 - 16 - 40
        for (n in listOf(0, 1, first - 1, first, first + 1, first + 1048560, 3 * 1024 * 1024)) {
            val e = encrypt(random(n))
            assertThat(e.file.length()).isEqualTo(VaultCrypto.expectedCiphertextSize(n.toLong()))
            assertThat(e.file.readBytes()[0].toInt()).isEqualTo(VaultCrypto.headerLength)
        }
    }

    @Test fun roundTripEmpty() {
        assertThat(decrypt(encrypt(ByteArray(0)))).isEmpty()
    }

    @Test fun roundTripOneByte() {
        assertThat(decrypt(encrypt(byteArrayOf(42)))).isEqualTo(byteArrayOf(42))
    }

    @Test fun roundTripThreeMegabytes() {
        val data = random(3 * 1024 * 1024)
        val e = encrypt(data)
        assertThat(e.file.length()).isGreaterThan(data.size.toLong())
        assertThat(decrypt(e)).isEqualTo(data)
    }

    @Test fun ciphertextDoesNotContainPlaintext() {
        val data = ByteArray(4096) { 'A'.code.toByte() }
        val ct = encrypt(data).file.readBytes()
        assertThat(String(ct, Charsets.ISO_8859_1)).doesNotContain("AAAAAAAAAAAAAAAA")
    }

    @Test fun seekIntoTheMiddleReturnsCorrectBytes() {
        val data = random(3 * 1024 * 1024)
        val e = encrypt(data)
        VaultCrypto.decryptingStream(priv, e.wrappedKey, e.file).use { ch ->
            assertThat(ch.size()).isEqualTo(data.size.toLong())
            for (pos in listOf(1_500_000L, 1024L * 1024 - 3, 17L)) {
                ch.position(pos)
                val buf = ByteBuffer.allocate(10_000)
                while (buf.hasRemaining() && ch.read(buf) >= 0) Unit
                assertThat(buf.array()).isEqualTo(data.copyOfRange(pos.toInt(), pos.toInt() + 10_000))
            }
        }
    }

    @Test fun eachFileGetsItsOwnKey() {
        val data = random(100)
        val a = encrypt(data)
        val b = encrypt(data)
        assertThat(a.wrappedKey).isNotEqualTo(b.wrappedKey)
        assertThat(a.file.readBytes()).isNotEqualTo(b.file.readBytes())
    }

    @Test fun encryptNeedsOnlyThePublicKeyFile() {
        File(keysDir, "private.pin.enc").delete()
        val pubOnly = VaultKeys(keysDir, FakeKeyWrapper(), fast)
        val data = random(2000)
        val e = encrypt(data, pubOnly.publicHandle())
        assertThat(decrypt(e)).isEqualTo(data)
    }

    @Test fun tamperedCiphertextByteFailsToDecrypt() {
        val data = random(200_000)
        val e = encrypt(data)
        val bytes = e.file.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 1).toByte()
        e.file.writeBytes(bytes)
        assertThrows(IOException::class.java) { decrypt(e) }
    }

    @Test fun truncatedCiphertextFailsToDecrypt() {
        val data = random(200_000)
        val e = encrypt(data)
        val bytes = e.file.readBytes()
        e.file.writeBytes(bytes.copyOf(bytes.size - 1))
        assertThrows(IOException::class.java) { decrypt(e) }
    }

    @Test fun truncatedAtSegmentBoundaryFailsToDecrypt() {
        val data = random(3 * 1024 * 1024)
        val e = encrypt(data)
        // Drop the whole last segment: the remaining ones were not encrypted as "last".
        e.file.writeBytes(e.file.readBytes().copyOf(2 * 1024 * 1024))
        assertThrows(IOException::class.java) { decrypt(e) }
    }

    @Test fun tamperedEmptyFileFailsToDecrypt() {
        val e = encrypt(ByteArray(0))
        val bytes = e.file.readBytes()
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 1).toByte()
        e.file.writeBytes(bytes)
        assertThrows(IOException::class.java) { decrypt(e) }
    }

    @Test fun readAtEndKeepsReturningEndOfStream() {
        val e = encrypt(random(10))
        VaultCrypto.decryptingStream(priv, e.wrappedKey, e.file).use { ch ->
            assertThat(readAll(ch)).hasLength(10)
            assertThat(ch.read(ByteBuffer.allocate(4))).isEqualTo(-1)
            assertThat(ch.read(ByteBuffer.allocate(4))).isEqualTo(-1)
        }
    }

    @Test fun tamperedWrappedKeyFailsToDecrypt() {
        val e = encrypt(random(100))
        e.wrappedKey[e.wrappedKey.size - 1] = (e.wrappedKey.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) { decrypt(e) }
    }

    @Test fun otherVaultsPrivateKeyCannotDecrypt() {
        val other = VaultKeys(File(tmp.root, "other"), FakeKeyWrapper(), fast)
        other.create("123456".toCharArray())
        val otherPriv = other.unlockWithSecret("123456".toCharArray())
        val e = encrypt(random(100))
        assertThrows(GeneralSecurityException::class.java) { decrypt(e, otherPriv) }
    }

    @Test fun encryptDoesNotCloseCallersStream() {
        var closed = false
        val out = object : ByteArrayOutputStream() {
            override fun close() { closed = true }
        }
        VaultCrypto.encrypt(keys.publicHandle(), ByteArrayInputStream(random(10)), out)
        assertThat(closed).isFalse()
    }
}
