package com.aks.gallerysorter.lock

import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.VaultCrypto
import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import org.junit.Test

class LockStateTest {
    private var now = 1_000L
    private val state = LockState { now }

    private fun handle(): PrivateHandle {
        VaultCrypto.ensureRegistered()
        return PrivateHandle(KeysetHandle.generateNew(KeyTemplates.get(VaultCrypto.HYBRID_TEMPLATE)))
    }

    @Test fun includeLocked_falseUntilUnlock() {
        assertThat(state.includeLocked.value).isFalse()
        assertThat(state.unlocked.value).isEmpty()
        state.unlock(VaultTier.STANDARD, handle())
        assertThat(state.includeLocked.value).isTrue()
    }

    @Test fun unlockStandard_doesNotUnlockHighSecurity() {
        val h = handle()
        state.unlock(VaultTier.STANDARD, h)
        assertThat(state.unlocked.value).containsExactly(VaultTier.STANDARD)
        assertThat(state.privateHandle(VaultTier.STANDARD)).isSameInstanceAs(h)
        assertThat(state.privateHandle(VaultTier.HIGH_SECURITY)).isNull()
        assertThat(state.isUnlocked(VaultTier.HIGH_SECURITY)).isFalse()
    }

    @Test fun lockAll_clearsHandles() {
        state.unlock(VaultTier.STANDARD, handle())
        state.unlock(VaultTier.HIGH_SECURITY, handle())
        state.lockAll()
        assertThat(state.privateHandle(VaultTier.STANDARD)).isNull()
        assertThat(state.privateHandle(VaultTier.HIGH_SECURITY)).isNull()
        assertThat(state.includeLocked.value).isFalse()
    }

    @Test fun lockAll_emitsEmptySet() {
        state.unlock(VaultTier.HIGH_SECURITY, handle())
        assertThat(state.unlocked.value).containsExactly(VaultTier.HIGH_SECURITY)
        assertThat(state.includeLocked.value).isTrue()
        state.lockAll()
        assertThat(state.unlocked.value).isEmpty()
    }

    @Test fun unlockedAt_usesInjectedClock() {
        state.unlock(VaultTier.STANDARD, handle())
        assertThat(state.unlockedAt(VaultTier.STANDARD)).isEqualTo(1_000L)
        assertThat(state.unlockedAt(VaultTier.HIGH_SECURITY)).isNull()
        now = 2_000L
        state.lockAll()
        assertThat(state.unlockedAt(VaultTier.STANDARD)).isNull()
    }

    @Test fun unlock_afterLockAllDuringPrompt_isRejected() {
        val epoch = state.epoch()
        state.lockAll() // app backgrounded while the unlock was in flight
        assertThat(state.unlock(VaultTier.STANDARD, handle(), epoch)).isFalse()
        assertThat(state.unlocked.value).isEmpty()
        assertThat(state.privateHandle(VaultTier.STANDARD)).isNull()
        assertThat(state.unlock(VaultTier.STANDARD, handle(), state.epoch())).isTrue()
    }
}
