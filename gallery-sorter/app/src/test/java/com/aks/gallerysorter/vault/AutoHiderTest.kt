package com.aks.gallerysorter.vault

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.SettingRow
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.InputStream
import java.util.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoHiderTest {
    @get:Rule val tmp = TemporaryFolder()

    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private val day = 24L * 60 * 60 * 1000
    private var now = 1_800_000_000_000L

    private lateinit var db: AppDatabase
    private lateinit var store: VaultStore
    private lateinit var guard: VaultKeyGuard
    private lateinit var hideManager: HideManager
    private lateinit var gallery: File
    private var blocked = 0
    private val incomplete = mutableListOf<Pair<VaultTier, HideResult>>()

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        store = VaultStore(File(tmp.root, "sd/.vault-gs"), File(tmp.root, "internal/hsvault"))
        guard = VaultKeyGuard(store, db.settingsDao(), clock = { now }) { dir -> VaultKeys(dir, FakeKeyWrapper(), fast) }
        val external = File(tmp.root, "sd")
        gallery = File(external, "DCIM/Camera").apply { mkdirs() }
        hideManager = HideManager(
            store = store, guard = guard, vaultDao = db.vaultDao(), mediaDao = db.mediaDao(), labelDao = db.labelDao(),
            settings = db.settingsDao(), unhidden = db.unhiddenDao(), media = BlankThumbs, fileOps = DirectFileOps,
            externalRoot = external,
        )
        runBlocking {
            guard.createVault(VaultTier.STANDARD, "123456".toCharArray()) // also records an unlock "now"
            guard.createVault(VaultTier.HIGH_SECURITY, "correct horse".toCharArray())
        }
    }

    @After fun tearDown() = db.close()

    private fun autoHider() = AutoHider(
        hideManager = hideManager, guard = guard, tierSettings = TierSettings(db.settingsDao()), settings = db.settingsDao(),
        unhidden = db.unhiddenDao(), privateHandle = { null }, blocked = { blocked++ },
        incomplete = { t, r -> incomplete += t to r }, clock = { now },
    )

    private fun indexed(id: Long, content: ByteArray, name: String = "IMG_$id.jpg"): MediaRow {
        val f = File(gallery, name).apply { writeBytes(content) }
        val row = MediaRow(
            id = id, path = f.path, kind = MediaKind.IMAGE, sizeBytes = content.size.toLong(), mime = "image/jpeg",
            width = 4000, height = 3000, dateTakenMs = id, durationMs = null, generation = 1, status = ScanStatus.DONE,
        )
        runBlocking { db.mediaDao().upsert(row) }
        return row
    }

    private fun bytes(seed: Long) = ByteArray(4000).also { Random(seed).nextBytes(it) }
    private fun rows() = runBlocking { db.vaultDao().all().first() }
    private fun lastUnlock(tier: VaultTier, at: Long) = runBlocking {
        db.settingsDao().put(SettingRow(VaultKeyGuard.lastUnlockKey(tier), at.toString()))
    }

    private val adult = listOf(CategoryAssignment(Category.OTHER_ADULT, 0.95f))
    private val myNudes = listOf(CategoryAssignment(Category.MY_NUDES, 0.95f))

    // ---- tier choice (pure) -----------------------------------------------------------------

    private fun state(std: TierState, hs: TierState) = mapOf(VaultTier.STANDARD to std, VaultTier.HIGH_SECURITY to hs)
    private val fresh = TierState(setUp = true, lastUnlockMs = now - 1)

    @Test fun tier_wantedInUse_isUsed() {
        assertThat(autoHideTier(VaultTier.HIGH_SECURITY, state(fresh, fresh), now)).isEqualTo(VaultTier.HIGH_SECURITY)
        assertThat(autoHideTier(VaultTier.STANDARD, state(fresh, fresh), now)).isEqualTo(VaultTier.STANDARD)
        val edge = TierState(true, now - AUTO_HIDE_RECENT_UNLOCK_MS)
        assertThat(autoHideTier(VaultTier.HIGH_SECURITY, state(fresh, edge), now)).isEqualTo(VaultTier.HIGH_SECURITY)
    }

    @Test fun tier_highSecurityNotSetUpStaleOrNeverUnlocked_fallsBackToStandard() {
        for (hs in listOf(TierState(false, null), TierState(true, now - 31 * day), TierState(true, null), TierState(false, now))) {
            assertThat(autoHideTier(VaultTier.HIGH_SECURITY, state(fresh, hs), now)).isEqualTo(VaultTier.STANDARD)
        }
    }

    @Test fun tier_noVaultInUse_skips() {
        val stale = TierState(true, now - 31 * day)
        assertThat(autoHideTier(VaultTier.HIGH_SECURITY, state(stale, stale), now)).isNull()
        assertThat(autoHideTier(VaultTier.STANDARD, state(stale, fresh), now)).isNull()
        assertThat(autoHideTier(VaultTier.STANDARD, state(TierState(false, null), TierState(false, null)), now)).isNull()
    }

    // ---- auto-hide --------------------------------------------------------------------------

    @Test fun unhiddenItem_staysVisibleOnRescan_explicitHideStillWorks() = runBlocking<Unit> {
        val content = bytes(1)
        val first = indexed(1, content)
        autoHider().autoHide(first, adult)
        assertThat(File(first.path).exists()).isFalse()

        val priv = guard.keys(VaultTier.STANDARD).unlockWithSecret("123456".toCharArray())
        assertThat(hideManager.unhide(listOf(rows().single().id), priv).restored).isEqualTo(1)
        // The media scan gives the restored file a new MediaStore id with no overrides; the rescan auto-hides it.
        val restored = indexed(2, content, name = File(first.path).name)
        autoHider().autoHide(restored, adult)

        assertThat(File(restored.path).readBytes()).isEqualTo(content)
        assertThat(rows()).isEmpty()
        assertThat(db.mediaDao().get(2)).isNotNull()
        assertThat(incomplete).isEmpty()
        assertThat(blocked).isEqualTo(0)

        val res = hideManager.hide(listOf(2), VaultTier.STANDARD, { _, _ -> })
        assertThat(res.hidden).isEqualTo(1)
        assertThat(File(restored.path).exists()).isFalse()
        assertThat(db.unhiddenDao().contains(rows().single().plainSha256)).isFalse()

        // Other content is still auto-hidden.
        val other = indexed(3, bytes(3))
        autoHider().autoHide(other, adult)
        assertThat(File(other.path).exists()).isFalse()
        assertThat(rows()).hasSize(2)
    }

    @Test fun highSecurityNotUnlockedFor30Days_goesToStandard() = runBlocking<Unit> {
        lastUnlock(VaultTier.HIGH_SECURITY, now - 31 * day)
        val item = indexed(1, bytes(1))
        autoHider().autoHide(item, myNudes)
        assertThat(rows().single().tier).isEqualTo(VaultTier.STANDARD)

        guard.recordUnlock(VaultTier.HIGH_SECURITY)
        val next = indexed(2, bytes(2))
        autoHider().autoHide(next, myNudes)
        assertThat(rows().map { it.tier }).containsExactly(VaultTier.STANDARD, VaultTier.HIGH_SECURITY)
    }

    @Test fun noVaultInUse_keepsItemsAndNotifiesAtMostOncePerDay() = runBlocking<Unit> {
        lastUnlock(VaultTier.STANDARD, now - 40 * day)
        lastUnlock(VaultTier.HIGH_SECURITY, now - 40 * day)
        val a = indexed(1, bytes(1))
        val b = indexed(2, bytes(2))
        autoHider().autoHide(a, adult)
        autoHider().autoHide(b, myNudes)
        assertThat(blocked).isEqualTo(1)
        assertThat(File(a.path).exists()).isTrue()
        assertThat(File(b.path).exists()).isTrue()
        assertThat(rows()).isEmpty()

        now += day - 1
        autoHider().autoHide(a, adult)
        assertThat(blocked).isEqualTo(1)
        now += 1
        autoHider().autoHide(a, adult)
        assertThat(blocked).isEqualTo(2)
    }

    private object DirectFileOps : FileOps {
        override fun exists(path: String) = File(path).exists()
        override fun freeBytes(dir: File) = Long.MAX_VALUE
        override fun deleteOriginal(path: String) = File(path).delete()
        override fun scanIntoMediaStore(path: String) = Unit
        override fun removeFromMediaStore(id: Long, deletedPath: String) = Unit
    }

    private object BlankThumbs : MediaSource {
        override suspend fun all() = emptyList<MediaItem>()
        override suspend fun changedSince(generation: Long) = emptyList<MediaItem>()
        override fun open(item: MediaItem): InputStream = error("not used")
        override suspend fun thumbnail(item: MediaItem, px: Int): Bitmap? = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        override suspend fun exif(item: MediaItem): ExifData? = null
    }
}
