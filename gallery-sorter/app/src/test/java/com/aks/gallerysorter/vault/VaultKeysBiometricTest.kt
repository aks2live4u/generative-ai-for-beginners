package com.aks.gallerysorter.vault

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric only so the real KeyPermanentlyInvalidatedException can be thrown by the fake. */
@RunWith(RobolectricTestRunner::class)
class VaultKeysBiometricTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun invalidatedKeystoreKeyGivesTypedErrorAndPinStillWorks() {
        val wrapper = FakeKeyWrapper()
        val k = VaultKeys(File(tmp.root, "keys"), wrapper, Argon2Params(1, 1024, 1))
        k.create("123456".toCharArray())
        k.enableBiometric(k.unlockWithSecret("123456".toCharArray()), k.biometricEncryptCipher())

        wrapper.invalidate { throw KeyPermanentlyInvalidatedException() }

        assertThrows(BiometricInvalidatedException::class.java) { k.biometricCipher() }
        val priv = k.unlockWithSecret("123456".toCharArray())

        // Re-enrolling biometrics after invalidation works with a fresh Keystore key.
        k.enableBiometric(priv, k.biometricEncryptCipher())
        assertThat(k.unlockWithBiometric(k.biometricCipher())).isNotNull()
    }
}
