package com.aks.gallerysorter.lock

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.vault.PrivateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The in-memory unlock session. Private handles live only here, never on disk, and
 * [lockAll] drops every reference (the app calls it when it goes to the background).
 *
 * [includeLocked] is true while any tier is unlocked; ViewModels feed it to
 * `LabelDao.itemsIn(..., includeLocked)` so locked categories stay out of views until unlock.
 */
class LockState(private val clock: () -> Long = System::currentTimeMillis) {
    private val lock = Any()
    private val handles = mutableMapOf<VaultTier, PrivateHandle>()
    private val unlockTimes = mutableMapOf<VaultTier, Long>()

    private val _unlocked = MutableStateFlow<Set<VaultTier>>(emptySet())
    val unlocked: StateFlow<Set<VaultTier>> = _unlocked.asStateFlow()

    private val _includeLocked = MutableStateFlow(false)
    val includeLocked: StateFlow<Boolean> = _includeLocked.asStateFlow()

    fun unlock(tier: VaultTier, handle: PrivateHandle) = synchronized(lock) {
        handles[tier] = handle
        unlockTimes[tier] = clock()
        publish()
    }

    fun lockAll() = synchronized(lock) {
        handles.clear()
        unlockTimes.clear()
        publish()
    }

    fun privateHandle(tier: VaultTier): PrivateHandle? = synchronized(lock) { handles[tier] }

    fun isUnlocked(tier: VaultTier): Boolean = synchronized(lock) { tier in handles }

    /** When [tier] was unlocked (per the injected clock), or null while locked. */
    fun unlockedAt(tier: VaultTier): Long? = synchronized(lock) { unlockTimes[tier] }

    private fun publish() {
        _unlocked.value = handles.keys.toSet()
        _includeLocked.value = handles.isNotEmpty()
    }
}

/** Locks everything when the whole app goes to the background (spec 6.5). */
class AppLockObserver(private val lockState: LockState) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = lockState.lockAll()

    companion object {
        /** Call once from `Application.onCreate` (Task 14). */
        fun install(lockState: LockState) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(AppLockObserver(lockState))
        }
    }
}
