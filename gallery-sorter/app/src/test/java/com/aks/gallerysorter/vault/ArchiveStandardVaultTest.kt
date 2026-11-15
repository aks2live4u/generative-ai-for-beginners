package com.aks.gallerysorter.vault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** "Start a new vault" after a reinstall with a forgotten PIN: the old vault is moved aside intact, never deleted. */
@RunWith(RobolectricTestRunner::class)
class ArchiveStandardVaultTest {
    @get:Rule val tmp = TemporaryFolder()
    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private lateinit var before: AppDatabase
    private lateinit var after: AppDatabase
    private lateinit var store: VaultStore

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        before = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        after = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        store = VaultStore(File(tmp.root, "sd/.vault-gs"), File(tmp.root, "internal/hsvault"))
    }

    @After fun tearDown() {
        before.close()
        after.close()
    }

    private fun guard(db: AppDatabase) = VaultKeyGuard(store, db.settingsDao()) { dir -> VaultKeys(dir, FakeKeyWrapper(), fast) }

    @Test fun archive_renamesTheOldVaultIntact_thenANewVaultIsCreated() = runBlocking<Unit> {
        // Earlier install: a vault with a hidden file.
        guard(before).createVault(VaultTier.STANDARD, "123456".toCharArray())
        store.ensure(VaultTier.STANDARD)
        val enc = File(store.filesDir(VaultTier.STANDARD), "a.enc").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val root = store.root(VaultTier.STANDARD)
        val oldPublic = File(VaultKeys.keysDir(root), VaultKeys.PUBLIC_FILE).readBytes()
        val oldPin = File(VaultKeys.keysDir(root), VaultKeys.PIN_FILE).readBytes()
        assertThat(enc.exists()).isTrue()

        // Reinstall: a fresh index, the PIN is forgotten.
        val g = guard(after)
        assertThat(g.hasUnpinnedKeys(VaultTier.STANDARD)).isTrue()
        val archived = g.archiveStandardVault(LocalDateTime.of(2026, 11, 15, 10, 20, 30))

        assertThat(archived).isEqualTo(File(root.parentFile, ".vault-gs.old-20261115-102030"))
        assertThat(root.exists()).isFalse()
        assertThat(File(archived, "files/a.enc").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(File(archived, VaultStore.NOMEDIA).isFile).isTrue()
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PUBLIC_FILE).readBytes()).isEqualTo(oldPublic)
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PIN_FILE).readBytes()).isEqualTo(oldPin)

        g.createVault(VaultTier.STANDARD, "654321".toCharArray())
        assertThat(g.isSetUp(VaultTier.STANDARD)).isTrue()
        assertThat(File(VaultKeys.keysDir(root), VaultKeys.PUBLIC_FILE).readBytes()).isNotEqualTo(oldPublic)
        // The archive is untouched by the new vault.
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PIN_FILE).readBytes()).isEqualTo(oldPin)
        assertThat(File(archived, "files/a.enc").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun archive_refusesAVaultSetUpInThisIndex() = runBlocking<Unit> {
        val g = guard(before)
        g.createVault(VaultTier.STANDARD, "123456".toCharArray())
        assertThrows(IllegalStateException::class.java) { runBlocking { g.archiveStandardVault(LocalDateTime.of(2026, 1, 1, 0, 0)) } }
        assertThat(store.root(VaultTier.STANDARD).exists()).isTrue()
    }
}
