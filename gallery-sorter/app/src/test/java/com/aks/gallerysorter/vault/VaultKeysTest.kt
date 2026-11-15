package com.aks.gallerysorter.vault

import com.aks.gallerysorter.core.VaultTier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Random
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

class VaultKeysTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val timeout: Timeout = Timeout.seconds(120)

    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private val wrapper = FakeKeyWrapper()

    private fun keys(dir: File = File(tmp.root, "keys")) = VaultKeys(dir, wrapper, fast)

    private fun pw(s: String) = s.toCharArray()

    /** Encrypts with the vault's public key, decrypts with [priv]; returns the plaintext. */
    private fun roundTrip(k: VaultKeys, priv: PrivateHandle, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val wrapped = VaultCrypto.encrypt(k.publicHandle(), ByteArrayInputStream(data), out)
        val f = tmp.newFile()
        f.writeBytes(out.toByteArray())
        VaultCrypto.decryptingStream(priv, wrapped, f).use { ch ->
            val buf = ByteBuffer.allocate(data.size + 16)
            while (ch.read(buf) >= 0) Unit
            return buf.array().copyOf(buf.position())
        }
    }

    private val sample = ByteArray(5000).also { Random(1).nextBytes(it) }

    @Test fun createThenUnlockWithSecret() {
        val k = keys()
        k.create(pw("123456"))
        assertThat(k.isCreated()).isTrue()
        val priv = k.unlockWithSecret(pw("123456"))
        assertThat(roundTrip(k, priv, sample)).isEqualTo(sample)
    }

    @Test fun wrongSecretThrowsWrongSecretException() {
        val k = keys()
        k.create(pw("123456"))
        assertThrows(WrongSecretException::class.java) { k.unlockWithSecret(pw("654321")) }
        assertThrows(WrongSecretException::class.java) { k.unlockWithSecret(pw("1234567")) }
    }

    @Test fun secretsAreZeroedAfterUse() {
        val k = keys()
        val s1 = pw("123456")
        k.create(s1)
        assertThat(s1.all { it == '\u0000' }).isTrue()
        val s2 = pw("123456")
        k.unlockWithSecret(s2)
        assertThat(s2.all { it == '\u0000' }).isTrue()
        val wrong = pw("000000")
        assertThrows(WrongSecretException::class.java) { k.unlockWithSecret(wrong) }
        assertThat(wrong.all { it == '\u0000' }).isTrue()
    }

    @Test fun createRefusesToOverwriteExistingKeys() {
        val dir = File(tmp.root, "keys")
        val k = keys(dir)
        k.create(pw("123456"))
        val pinBefore = File(dir, "private.pin.enc").readBytes()
        val pubBefore = File(dir, "public.keyset").readBytes()

        assertThrows(IllegalStateException::class.java) { keys(dir).create(pw("999999")) }

        assertThat(File(dir, "private.pin.enc").readBytes()).isEqualTo(pinBefore)
        assertThat(File(dir, "public.keyset").readBytes()).isEqualTo(pubBefore)
        keys(dir).unlockWithSecret(pw("123456"))
    }

    /** Simulates a crash after private.pin.enc was renamed into place but before public.keyset. */
    private fun orphan(dir: File) = check(File(dir, "public.keyset").delete())

    @Test fun createRepairsOrphanedPrivateKeyWithCorrectSecret() {
        val dir = File(tmp.root, "keys")
        val k = keys(dir)
        k.create(pw("123456"))
        val out = ByteArrayOutputStream()
        val wrapped = VaultCrypto.encrypt(k.publicHandle(), ByteArrayInputStream(sample), out)
        val f = tmp.newFile().apply { writeBytes(out.toByteArray()) }
        val pinBefore = File(dir, "private.pin.enc").readBytes()
        orphan(dir)
        assertThat(keys(dir).isCreated()).isFalse()

        val secret = pw("123456")
        keys(dir).create(secret)
        assertThat(secret.all { it == '\u0000' }).isTrue()

        val fresh = keys(dir)
        assertThat(fresh.isCreated()).isTrue()
        assertThat(File(dir, "private.pin.enc").readBytes()).isEqualTo(pinBefore)
        val priv = fresh.unlockWithSecret(pw("123456"))
        VaultCrypto.decryptingStream(priv, wrapped, f).use { ch ->
            val buf = ByteBuffer.allocate(sample.size)
            while (buf.hasRemaining() && ch.read(buf) >= 0) Unit
            assertThat(buf.array()).isEqualTo(sample)
        }
        assertThat(roundTrip(fresh, priv, sample)).isEqualTo(sample)
    }

    @Test fun createRefusesOrphanedPrivateKeyWithWrongSecret() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        val pinBefore = File(dir, "private.pin.enc").readBytes()
        orphan(dir)

        val wrong = pw("999999")
        assertThrows(OrphanedVaultException::class.java) { keys(dir).create(wrong) }
        assertThat(wrong.all { it == '\u0000' }).isTrue()
        assertThat(File(dir, "private.pin.enc").readBytes()).isEqualTo(pinBefore)
        assertThat(File(dir, "public.keyset").exists()).isFalse()
        assertThat(dir.list()!!.toList()).containsExactly("private.pin.enc")
    }

    @Test fun createIgnoresStrayTempFiles() {
        val dir = File(tmp.root, "keys").apply { mkdirs() }
        File(dir, "private.pin.enc.tmp").writeBytes(ByteArray(7))
        File(dir, "public.keyset.tmp").writeBytes(ByteArray(7))
        keys(dir).create(pw("123456"))
        keys(dir).unlockWithSecret(pw("123456"))
    }

    @Test fun noTempFilesLeftBehind() {
        val dir = File(tmp.root, "keys")
        val k = keys(dir)
        k.create(pw("123456"))
        val priv = k.unlockWithSecret(pw("123456"))
        k.enableBiometric(priv, k.biometricEncryptCipher())
        k.changeSecret(priv, pw("7654321"))
        assertThat(dir.list()!!.sorted()).containsExactly("private.bio.enc", "private.pin.enc", "public.keyset")
    }

    @Test fun pinFileHasVersionedHeaderWithParams() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        val b = ByteBuffer.wrap(File(dir, "private.pin.enc").readBytes())
        val magic = ByteArray(4).also { b.get(it) }
        assertThat(String(magic, Charsets.US_ASCII)).isEqualTo("GSV1")
        assertThat(b.get().toInt()).isEqualTo(1)
        b.position(b.position() + 16) // salt
        assertThat(b.int).isEqualTo(1)
        assertThat(b.int).isEqualTo(1024)
        assertThat(b.int).isEqualTo(1)
    }

    @Test fun tamperedHeaderIsRejected() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        val f = File(dir, "private.pin.enc")
        val bytes = f.readBytes()
        // Bump iterations 1 -> 2: the header is GCM associated data, so the tag must fail.
        bytes[4 + 1 + 16 + 3] = 2
        f.writeBytes(bytes)
        assertThrows(WrongSecretException::class.java) { keys(dir).unlockWithSecret(pw("123456")) }
    }

    @Test fun unknownVersionIsAnIoErrorNotWrongSecret() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        val f = File(dir, "private.pin.enc")
        val bytes = f.readBytes()
        bytes[4] = 9
        f.writeBytes(bytes)
        assertThrows(IOException::class.java) { keys(dir).unlockWithSecret(pw("123456")) }
    }

    @Test fun absurdArgonParamsAreRejectedBeforeDerivation() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        val f = File(dir, "private.pin.enc")
        val bytes = f.readBytes()
        ByteBuffer.wrap(bytes).putInt(4 + 1 + 16 + 4, Int.MAX_VALUE) // memory KiB
        f.writeBytes(bytes)
        assertThrows(IOException::class.java) { keys(dir).unlockWithSecret(pw("123456")) }
    }

    @Test fun argonParamCeilingsOnRead() {
        // offsets: iterations, memory KiB, parallelism; each value is just over its ceiling
        for ((offset, value) in listOf(21 to 11, 25 to 131073, 29 to 5)) {
            val dir = File(tmp.root, "ceil$offset")
            keys(dir).create(pw("123456"))
            val f = File(dir, "private.pin.enc")
            val bytes = f.readBytes()
            ByteBuffer.wrap(bytes).putInt(offset, value)
            f.writeBytes(bytes)
            assertThrows(IOException::class.java) { keys(dir).unlockWithSecret(pw("123456")) }
        }
    }

    @Test fun productionParamsAreWithinTheReadCeilings() {
        VaultKeys(File(tmp.root, "x"), wrapper, Argon2Params(10, 131072, 4))
        assertThrows(IllegalArgumentException::class.java) {
            VaultKeys(File(tmp.root, "y"), wrapper, Argon2Params(3, 131073, 1))
        }
    }

    @Test fun unpairedSurrogateSecretIsRejectedNotReplaced() {
        val k = keys()
        val lone = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', '\uD800')
        assertThat(validateSecret(VaultTier.HIGH_SECURITY, lone)).isFalse()
        assertThrows(InvalidSecretException::class.java) { k.create(lone) }
        assertThat(lone.all { it == '\u0000' }).isTrue()
        assertThat(k.isCreated()).isFalse()
        // A valid surrogate pair (emoji) is fine and distinct from '?'.
        k.create("abcdefghi\uD83D\uDE00".toCharArray())
        assertThrows(WrongSecretException::class.java) { k.unlockWithSecret(pw("abcdefghi?")) }
        k.unlockWithSecret("abcdefghi\uD83D\uDE00".toCharArray())
    }

    @Test fun truncatedPinFileIsAnIoError() {
        val dir = File(tmp.root, "keys")
        keys(dir).create(pw("123456"))
        File(dir, "private.pin.enc").writeBytes(ByteArray(10))
        assertThrows(IOException::class.java) { keys(dir).unlockWithSecret(pw("123456")) }
    }

    @Test fun changeSecretOldFailsNewWorksOldFilesStillDecrypt() {
        val dir = File(tmp.root, "keys")
        val k = keys(dir)
        k.create(pw("123456"))
        val priv = k.unlockWithSecret(pw("123456"))

        val out = ByteArrayOutputStream()
        val wrapped = VaultCrypto.encrypt(k.publicHandle(), ByteArrayInputStream(sample), out)
        val f = tmp.newFile().apply { writeBytes(out.toByteArray()) }

        val newSecret = pw("abcdefghij")
        k.changeSecret(priv, newSecret)
        assertThat(newSecret.all { it == '\u0000' }).isTrue()

        val fresh = keys(dir)
        assertThrows(WrongSecretException::class.java) { fresh.unlockWithSecret(pw("123456")) }
        val priv2 = fresh.unlockWithSecret(pw("abcdefghij"))
        VaultCrypto.decryptingStream(priv2, wrapped, f).use { ch ->
            val buf = ByteBuffer.allocate(sample.size)
            while (buf.hasRemaining() && ch.read(buf) >= 0) Unit
            assertThat(buf.array()).isEqualTo(sample)
        }
    }

    @Test fun changeSecretRejectsHandleFromAnotherVault() {
        val a = keys(File(tmp.root, "a")).apply { create(pw("123456")) }
        val b = keys(File(tmp.root, "b")).apply { create(pw("123456")) }
        val privB = b.unlockWithSecret(pw("123456"))
        val before = File(tmp.root, "a/private.pin.enc").readBytes()
        assertThrows(IllegalArgumentException::class.java) { a.changeSecret(privB, pw("999999")) }
        assertThat(File(tmp.root, "a/private.pin.enc").readBytes()).isEqualTo(before)
    }

    @Test fun biometricRoundTripAndSurvivesSecretChange() {
        val k = keys()
        k.create(pw("123456"))
        val priv = k.unlockWithSecret(pw("123456"))
        assertThat(k.isBiometricEnabled()).isFalse()
        k.enableBiometric(priv, k.biometricEncryptCipher())
        assertThat(k.isBiometricEnabled()).isTrue()

        val viaBio = k.unlockWithBiometric(k.biometricCipher())
        assertThat(roundTrip(k, viaBio, sample)).isEqualTo(sample)

        k.changeSecret(priv, pw("654321"))
        assertThat(k.isBiometricEnabled()).isTrue()
        assertThat(roundTrip(k, k.unlockWithBiometric(k.biometricCipher()), sample)).isEqualTo(sample)
    }

    @Test fun biometricKeyDeletedGivesTypedErrorAndPinStillWorks() {
        val k = keys()
        k.create(pw("123456"))
        k.enableBiometric(k.unlockWithSecret(pw("123456")), k.biometricEncryptCipher())
        wrapper.delete()
        assertThrows(BiometricInvalidatedException::class.java) { k.biometricCipher() }
        val priv = k.unlockWithSecret(pw("123456"))
        assertThat(roundTrip(k, priv, sample)).isEqualTo(sample)
    }

    @Test fun tamperedBioFileGivesTypedErrorAndPinStillWorks() {
        val dir = File(tmp.root, "keys")
        val k = keys(dir)
        k.create(pw("123456"))
        k.enableBiometric(k.unlockWithSecret(pw("123456")), k.biometricEncryptCipher())
        val f = File(dir, "private.bio.enc")
        val good = f.readBytes()

        val flipped = good.copyOf().also { it[it.size - 1] = (it.last().toInt() xor 1).toByte() }
        f.writeBytes(flipped)
        assertThrows(BiometricInvalidatedException::class.java) { k.unlockWithBiometric(k.biometricCipher()) }

        f.writeBytes(good.copyOf().also { it[0] = 'X'.code.toByte() })
        assertThrows(BiometricInvalidatedException::class.java) { k.biometricCipher() }

        f.writeBytes(good.copyOf(8))
        assertThrows(BiometricInvalidatedException::class.java) { k.unlockWithBiometric(k.biometricCipher()) }

        assertThat(roundTrip(k, k.unlockWithSecret(pw("123456")), sample)).isEqualTo(sample)
    }

    @Test fun disableBiometricRemovesWrapAndKey() {
        val k = keys()
        k.create(pw("123456"))
        k.enableBiometric(k.unlockWithSecret(pw("123456")), k.biometricEncryptCipher())
        k.disableBiometric()
        assertThat(k.isBiometricEnabled()).isFalse()
        assertThrows(IllegalStateException::class.java) { k.biometricCipher() }
    }

    @Test fun productionArgonParamsWork() {
        val dir = File(tmp.root, "prod")
        val k = VaultKeys(dir, wrapper)
        k.create(pw("123456"))
        val b = ByteBuffer.wrap(File(dir, "private.pin.enc").readBytes())
        b.position(4 + 1 + 16)
        assertThat(listOf(b.int, b.int, b.int)).containsExactly(3, 65536, 1).inOrder()
        val priv = k.unlockWithSecret(pw("123456"))
        assertThat(roundTrip(k, priv, sample)).isEqualTo(sample)
        assertThrows(WrongSecretException::class.java) { k.unlockWithSecret(pw("123457")) }
    }

    @Test fun validateSecretRules() {
        assertThat(validateSecret(VaultTier.STANDARD, pw("12345"))).isFalse()
        assertThat(validateSecret(VaultTier.STANDARD, pw("123456"))).isTrue()
        assertThat(validateSecret(VaultTier.STANDARD, pw("12345a"))).isFalse()
        assertThat(validateSecret(VaultTier.STANDARD, pw("١٢٣٤٥٦"))).isFalse() // non-ASCII digits
        assertThat(validateSecret(VaultTier.HIGH_SECURITY, pw("abc123defg"))).isTrue()
        assertThat(validateSecret(VaultTier.HIGH_SECURITY, pw("abc123def"))).isFalse()
        val s = pw("123456")
        validateSecret(VaultTier.STANDARD, s)
        assertThat(String(s)).isEqualTo("123456") // validation does not consume the secret
    }
}
