package com.aks.gallerysorter.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.vault.Argon2Params
import com.aks.gallerysorter.vault.FakeKeyWrapper
import com.aks.gallerysorter.vault.VaultKeys
import java.io.File

/** Shared fixtures for the ViewModel tests: an in-memory index, media rows and a real unlock. */
object TestIndex {
    fun db(): AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()

    fun media(id: Long, city: String? = null, kind: MediaKind = MediaKind.IMAGE, width: Int = 10) = MediaRow(
        id = id, path = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg", kind = kind, sizeBytes = 10,
        mime = "image/jpeg", width = width, height = 10, dateTakenMs = id, durationMs = null, generation = 1,
        status = ScanStatus.DONE, city = city,
    )

    /** Unlocks [tier] in [lockState] with a real (fast-Argon2) key pair created under [dir]. */
    fun unlock(lockState: LockState, dir: File, tier: VaultTier = VaultTier.STANDARD) {
        val keys = VaultKeys(dir, FakeKeyWrapper(), Argon2Params(1, 1024, 1))
        keys.create("123456".toCharArray())
        lockState.unlock(tier, keys.unlockWithSecret("123456".toCharArray()))
    }
}
