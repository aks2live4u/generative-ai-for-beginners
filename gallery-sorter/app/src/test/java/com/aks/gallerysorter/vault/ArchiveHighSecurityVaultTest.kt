package com.aks.gallerysorter.vault

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.VaultRow
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** "Forgot passcode? Start a new vault": the high-security vault is moved aside intact, never deleted. */
@RunWith(RobolectricTestRunner::class)
class ArchiveHighSecurityVaultTest {
    @get:Rule val tmp = TemporaryFolder()
    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private val hs = VaultTier.HIGH_SECURITY
    private lateinit var db: AppDatabase
    private lateinit var store: VaultStore
    private lateinit var guard: VaultKeyGuard
    private lateinit var manager: HideManager

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        store = VaultStore(File(tmp.root, "sd/.vault-gs"), File(tmp.root, "internal/files/hsvault"))
        guard = VaultKeyGuard(store, db.settingsDao()) { dir -> VaultKeys(dir, FakeKeyWrapper(), fast) }
        manager = HideManager(
            store = store, guard = guard, vaultDao = db.vaultDao(), mediaDao = db.mediaDao(), labelDao = db.labelDao(),
            settings = db.settingsDao(), unhidden = db.unhiddenDao(), media = NoMedia, fileOps = NoFileOps,
            externalRoot = File(tmp.root, "sd"),
        )
    }

    @After fun tearDown() = db.close()

    private fun row(tier: VaultTier, file: String) = VaultRow(
        originalPath = "/sdcard/DCIM/$file.jpg", tier = tier, vaultFile = "files/$file.enc", kind = MediaKind.IMAGE,
        sizeBytes = 3, categories = "MY_NUDES", wrappedFileKey = byteArrayOf(1), thumbFile = null, thumbWrappedKey = null,
        plainSha256 = file, verified = true,
    )

    @Test fun archive_renamesTheVaultIntact_dropsItsRows_thenANewVaultIsCreated() = runBlocking<Unit> {
        guard.createVault(hs, "correct horse".toCharArray())
        guard.createVault(VaultTier.STANDARD, "123456".toCharArray())
        store.ensure(hs)
        File(store.filesDir(hs), "a.enc").writeBytes(byteArrayOf(1, 2, 3))
        File(store.filesDir(hs), "a.meta").writeBytes(byteArrayOf(4, 5))
        db.vaultDao().insert(row(hs, "a"))
        db.vaultDao().insert(row(VaultTier.STANDARD, "s"))
        val root = store.root(hs)
        val oldPublic = File(VaultKeys.keysDir(root), VaultKeys.PUBLIC_FILE).readBytes()
        val oldPin = File(VaultKeys.keysDir(root), VaultKeys.PIN_FILE).readBytes()
        assertThat(guard.lastUnlockMs(hs)).isNotNull()

        val archived = manager.archiveVault(hs, LocalDateTime.of(2026, 11, 15, 10, 20, 30))

        assertThat(archived).isEqualTo(File(root.parentFile, "hsvault.old-20261115-102030"))
        assertThat(root.exists()).isFalse()
        assertThat(File(archived, "files/a.enc").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(File(archived, "files/a.meta").readBytes()).isEqualTo(byteArrayOf(4, 5))
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PUBLIC_FILE).readBytes()).isEqualTo(oldPublic)
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PIN_FILE).readBytes()).isEqualTo(oldPin)
        assertThat(guard.isSetUp(hs)).isFalse()
        assertThat(guard.hasUnpinnedKeys(hs)).isFalse()
        assertThat(guard.lastUnlockMs(hs)).isNull()
        assertThat(db.vaultDao().all().first().map { it.tier }).containsExactly(VaultTier.STANDARD)
        assertThat(guard.isSetUp(VaultTier.STANDARD)).isTrue()

        guard.createVault(hs, "a brand new passcode".toCharArray())
        assertThat(guard.isSetUp(hs)).isTrue()
        assertThat(File(VaultKeys.keysDir(root), VaultKeys.PUBLIC_FILE).readBytes()).isNotEqualTo(oldPublic)
        assertThat(File(VaultKeys.keysDir(archived), VaultKeys.PIN_FILE).readBytes()).isEqualTo(oldPin)
        assertThat(File(archived, "files/a.enc").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun archive_targetExists_refusesAndChangesNothing() = runBlocking<Unit> {
        guard.createVault(hs, "correct horse".toCharArray())
        db.vaultDao().insert(row(hs, "a"))
        val root = store.root(hs)
        File(root.parentFile, "hsvault.old-20260101-000000").mkdirs()

        assertThrows(IOException::class.java) { runBlocking { manager.archiveVault(hs, LocalDateTime.of(2026, 1, 1, 0, 0)) } }

        assertThat(root.isDirectory).isTrue()
        assertThat(guard.isSetUp(hs)).isTrue()
        assertThat(db.vaultDao().all().first()).hasSize(1)
    }

    @Test fun archive_noVault_refuses() {
        assertThrows(IOException::class.java) { runBlocking { manager.archiveVault(hs, LocalDateTime.of(2026, 1, 1, 0, 0)) } }
    }

    private object NoFileOps : FileOps {
        override fun exists(path: String) = File(path).exists()
        override fun freeBytes(dir: File) = Long.MAX_VALUE
        override fun deleteOriginal(path: String) = error("not used")
        override fun scanIntoMediaStore(path: String) = Unit
        override fun removeFromMediaStore(id: Long, deletedPath: String) = Unit
    }

    private object NoMedia : MediaSource {
        override suspend fun all() = emptyList<MediaItem>()
        override suspend fun changedSince(generation: Long) = emptyList<MediaItem>()
        override fun open(item: MediaItem): InputStream = error("not used")
        override suspend fun thumbnail(item: MediaItem, px: Int): Bitmap? = null
        override suspend fun exif(item: MediaItem): ExifData? = null
    }
}
