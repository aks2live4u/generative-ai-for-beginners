package com.aks.gallerysorter.vault

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaItem
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.index.entities.VaultRow
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Random
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

@RunWith(RobolectricTestRunner::class)
class HideManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val fast = Argon2Params(iterations = 1, memoryKiB = 1024, parallelism = 1)
    private val pin = "123456"
    private val passcode = "correct horse"

    private lateinit var db: AppDatabase
    private lateinit var store: VaultStore
    private lateinit var guard: VaultKeyGuard
    private lateinit var fileOps: FakeFileOps
    private lateinit var gallery: File
    private lateinit var external: File

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        store = VaultStore(File(tmp.root, "sd/.vault-gs"), File(tmp.root, "internal/hsvault"))
        guard = VaultKeyGuard(store, db.settingsDao()) { dir -> VaultKeys(dir, FakeKeyWrapper(), fast) }
        fileOps = FakeFileOps()
        external = File(tmp.root, "sd")
        gallery = File(external, "DCIM/Camera").apply { mkdirs() }
        runBlocking {
            guard.createVault(VaultTier.STANDARD, pin.toCharArray())
            guard.createVault(VaultTier.HIGH_SECURITY, passcode.toCharArray())
        }
    }

    @After fun tearDown() = db.close()

    // ---- fixtures ---------------------------------------------------------------------------

    private fun manager(encrypt: Encryptor = VaultCrypto::encrypt) = HideManager(
        store = store, guard = guard, vaultDao = db.vaultDao(), mediaDao = db.mediaDao(),
        labelDao = db.labelDao(), media = FakeMediaSource(), fileOps = fileOps,
        externalRoot = external, encrypt = encrypt,
    )

    private fun bytes(n: Int, seed: Long = n.toLong()) = ByteArray(n).also { Random(seed).nextBytes(it) }

    /** A gallery file plus its index row (and a label), as the scanner would leave it. */
    private fun indexed(id: Long, content: ByteArray, name: String = "IMG_$id.jpg", dir: File = gallery): File {
        val f = File(dir.apply { mkdirs() }, name)
        f.writeBytes(content)
        runBlocking {
            db.mediaDao().upsert(
                MediaRow(
                    id = id, path = f.path, kind = MediaKind.IMAGE, sizeBytes = content.size.toLong(),
                    mime = "image/jpeg", width = 4000, height = 3000, dateTakenMs = id, durationMs = null,
                    generation = 1, status = ScanStatus.DONE,
                ),
            )
            db.labelDao().upsertAll(listOf(LabelRow(id, Category.BANKING, 0.9f)))
        }
        return f
    }

    private fun unlock(tier: VaultTier): PrivateHandle =
        guard.keys(tier).unlockWithSecret((if (tier == VaultTier.STANDARD) pin else passcode).toCharArray())

    private fun rows(): List<VaultRow> = runBlocking { db.vaultDao().all().first() }

    private fun filesIn(dir: File): List<String> = dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()

    private fun encFiles(tier: VaultTier) = filesIn(store.filesDir(tier)).filter { it != ".nomedia" }

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun decrypt(priv: PrivateHandle, wrapped: ByteArray, f: File): ByteArray =
        VaultCrypto.decryptingStream(priv, wrapped, f).use { ch ->
            val out = ByteArrayOutputStream()
            val buf = ByteBuffer.allocate(64 * 1024)
            while (ch.read(buf) >= 0) { out.write(buf.array(), 0, buf.position()); buf.clear() }
            out.toByteArray()
        }

    /** Asserts that nothing happened to item [id]: original intact, still indexed, no vault trace. */
    private fun assertUntouched(id: Long, original: File, content: ByteArray, tier: VaultTier = VaultTier.STANDARD) {
        assertThat(original.readBytes()).isEqualTo(content)
        assertThat(runBlocking { db.mediaDao().get(id) }).isNotNull()
        assertThat(rows()).isEmpty()
        assertThat(encFiles(tier)).isEmpty()
        assertThat(filesIn(store.thumbsDir(tier)).filter { it != ".nomedia" }).isEmpty()
        assertThat(fileOps.deleted).isEmpty()
        assertThat(fileOps.removedFromMediaStore).isEmpty()
    }

    // ---- hide / unhide ----------------------------------------------------------------------

    @Test fun hide_roundTrip_unhideRestoresIdenticalBytesAtOriginalPath() = runBlocking<Unit> {
        val content = bytes(1_500_000) // spans two 1 MB segments
        val original = indexed(1, content)
        val progress = mutableListOf<Pair<Int, Int>>()

        val res = manager().hide(listOf(1), VaultTier.STANDARD, { d, t -> progress += d to t })

        assertThat(res).isEqualTo(HideResult(hidden = 1, missing = emptyList(), failed = emptyList()))
        assertThat(progress).containsExactly(1 to 1)
        assertThat(original.exists()).isFalse()
        assertThat(fileOps.removedFromMediaStore).containsExactly(1L)
        assertThat(db.mediaDao().get(1)).isNull()
        val row = rows().single()
        assertThat(row.originalPath).isEqualTo(original.path)
        assertThat(row.tier).isEqualTo(VaultTier.STANDARD)
        assertThat(row.plainSha256).isEqualTo(sha256(content))
        assertThat(row.sizeBytes).isEqualTo(content.size.toLong())
        assertThat(row.categories).isEqualTo("BANKING")
        assertThat(row.vaultFile).matches("files/[0-9a-f-]{36}\\.enc")
        assertThat(row.thumbFile).matches("thumbs/[0-9a-f-]{36}\\.thumb\\.enc")
        val enc = store.resolve(VaultTier.STANDARD, row.vaultFile)
        assertThat(String(enc.readBytes(), Charsets.ISO_8859_1)).doesNotContain(String(content.copyOf(64), Charsets.ISO_8859_1))
        val priv = unlock(VaultTier.STANDARD)
        assertThat(decrypt(priv, row.thumbWrappedKey!!, store.resolve(VaultTier.STANDARD, row.thumbFile!!))).isNotEmpty()

        val un = manager().unhide(listOf(row.id), priv)

        assertThat(un).isEqualTo(UnhideResult(restored = 1, failed = emptyList()))
        assertThat(original.readBytes()).isEqualTo(content)
        assertThat(fileOps.scanned).containsExactly(original.path)
        assertThat(rows()).isEmpty()
        assertThat(encFiles(VaultTier.STANDARD)).isEmpty()
        assertThat(filesIn(store.thumbsDir(VaultTier.STANDARD))).containsExactly(".nomedia")
        assertThat(filesIn(gallery)).containsExactly(original.name)
    }

    @Test fun hide_unlockedVerify_decryptsBeforeDeleting() = runBlocking<Unit> {
        val content = bytes(70_000)
        val original = indexed(1, content)
        val res = manager().hide(listOf(1), VaultTier.HIGH_SECURITY, { _, _ -> }, unlock(VaultTier.HIGH_SECURITY))
        assertThat(res.hidden).isEqualTo(1)
        assertThat(original.exists()).isFalse()
        val row = rows().single()
        assertThat(decrypt(unlock(VaultTier.HIGH_SECURITY), row.wrappedFileKey, store.resolve(row.tier, row.vaultFile)))
            .isEqualTo(content)
    }

    @Test fun hide_unlockedVerify_corruptCiphertext_keepsOriginal() = runBlocking<Unit> {
        val content = bytes(70_000)
        val original = indexed(1, content)
        // Same length as a good ciphertext, one byte flipped: only a full decrypt can tell.
        val flipping: Encryptor = { pub, input, out ->
            val buf = ByteArrayOutputStream()
            val key = VaultCrypto.encrypt(pub, input, buf)
            val ct = buf.toByteArray().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 1).toByte() }
            out.write(ct)
            key
        }
        val res = manager(flipping).hide(listOf(1), VaultTier.STANDARD, { _, _ -> }, unlock(VaultTier.STANDARD))
        assertThat(res.hidden).isEqualTo(0)
        assertThat(res.failed.map { it.first }).containsExactly(1L)
        assertUntouched(1, original, content)
    }

    @Test fun hide_lockedVerify_truncatedCiphertext_keepsOriginal() = runBlocking<Unit> {
        val content = bytes(70_000)
        val original = indexed(1, content)
        val truncating: Encryptor = { pub, input, out ->
            val buf = ByteArrayOutputStream()
            val key = VaultCrypto.encrypt(pub, input, buf)
            out.write(buf.toByteArray(), 0, buf.size() - 100)
            key
        }
        val res = manager(truncating).hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(res.failed.map { it.first }).containsExactly(1L)
        assertUntouched(1, original, content)
    }

    @Test fun hide_interruptedBeforeVerify_keepsOriginal() = runBlocking<Unit> {
        val content = bytes(200_000)
        val original = indexed(1, content)
        val failing: Encryptor = { _, input, out ->
            val buf = ByteArray(50_000)
            input.read(buf)
            out.write(buf)
            throw IOException("disk went away")
        }
        val res = manager(failing).hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(res.hidden).isEqualTo(0)
        assertThat(res.failed.map { it.first }).containsExactly(1L)
        assertUntouched(1, original, content)
    }

    @Test fun hide_originalModifiedDuringHide_abortsAndKeepsOriginal() = runBlocking<Unit> {
        val content = bytes(70_000)
        val original = indexed(1, content)
        val modifying: Encryptor = { pub, input, out ->
            val key = VaultCrypto.encrypt(pub, input, out)
            original.appendBytes(byteArrayOf(1, 2, 3)) // another app writes while we encrypt
            key
        }
        val res = manager(modifying).hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(res.failed.map { it.first }).containsExactly(1L)
        assertUntouched(1, original, content + byteArrayOf(1, 2, 3))
    }

    @Test fun hide_deleteOriginalFails_keepsVaultCopyAndReports() = runBlocking<Unit> {
        val content = bytes(70_000)
        val original = indexed(1, content)
        fileOps.undeletable += original.path
        val res = manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(res).isEqualTo(HideResult(0, emptyList(), listOf(1L to "original not deleted")))
        assertThat(original.readBytes()).isEqualTo(content)
        assertThat(fileOps.removedFromMediaStore).isEmpty()
        assertThat(db.mediaDao().get(1)).isNotNull()
        val row = rows().single()
        assertThat(store.resolve(row.tier, row.vaultFile).exists()).isTrue()
        assertThat(decrypt(unlock(VaultTier.STANDARD), row.wrappedFileKey, store.resolve(row.tier, row.vaultFile)))
            .isEqualTo(content)
    }

    @Test fun hide_insufficientSpace_failsWithoutTouchingOriginal() = runBlocking<Unit> {
        val content = bytes(10_000)
        val original = indexed(1, content)
        fileOps.free = content.size + 52_428_800L - 1
        val res = manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(res).isEqualTo(HideResult(0, emptyList(), listOf(1L to "insufficient space")))
        assertUntouched(1, original, content)
        assertThat(fileOps.freeQueries).containsExactly(store.filesDir(VaultTier.STANDARD))

        fileOps.free = content.size + 52_428_800L // exactly enough
        assertThat(manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> }).hidden).isEqualTo(1)
    }

    @Test fun hide_missingFile_reportsAndDropsRow() = runBlocking<Unit> {
        val content = bytes(1000)
        indexed(1, content).delete()
        val present = indexed(2, bytes(2000))
        val progress = mutableListOf<Pair<Int, Int>>()
        val res = manager().hide(listOf(1, 2), VaultTier.STANDARD, { d, t -> progress += d to t })
        assertThat(res).isEqualTo(HideResult(hidden = 1, missing = listOf(1L), failed = emptyList()))
        assertThat(db.mediaDao().get(1)).isNull()
        assertThat(progress).containsExactly(1 to 2, 2 to 2).inOrder()
        assertThat(present.exists()).isFalse()
        assertThat(rows()).hasSize(1)
    }

    @Test fun hide_publicKeysetSwapped_refusesAndKeepsOriginal() = runBlocking<Unit> {
        val content = bytes(5000)
        val original = indexed(1, content)
        // An attacker with storage access replaces public.keyset with their own.
        val rogue = VaultKeys(File(tmp.root, "rogue"), FakeKeyWrapper(), fast).apply { create("999999".toCharArray()) }
        File(tmp.root, "rogue/${VaultKeys.PUBLIC_FILE}")
            .copyTo(File(VaultKeys.keysDir(store.root(VaultTier.STANDARD)), VaultKeys.PUBLIC_FILE), overwrite = true)
        assertThat(rogue.isCreated()).isTrue()

        assertThrows(VaultKeyMismatchException::class.java) {
            runBlocking { manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> }) }
        }
        assertUntouched(1, original, content)
    }

    @Test fun hide_noPinRecorded_refuses() = runBlocking<Unit> {
        val content = bytes(5000)
        val original = indexed(1, content)
        db.settingsDao().put(SettingRow(VaultKeyGuard.pinKey(VaultTier.STANDARD), ""))
        assertThrows(VaultKeyMismatchException::class.java) {
            runBlocking { manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> }) }
        }
        assertUntouched(1, original, content)
    }

    @Test fun unhide_pathOccupied_restoresWithSuffix() = runBlocking<Unit> {
        val content = bytes(3000)
        val original = indexed(1, content, name = "beach.jpg")
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        val newer = bytes(10, seed = 99)
        original.writeBytes(newer) // a new file now sits at the old path
        File(gallery, "beach (1).jpg").writeBytes(newer)

        val res = manager().unhide(listOf(rows().single().id), unlock(VaultTier.STANDARD))

        assertThat(res.restored).isEqualTo(1)
        assertThat(original.readBytes()).isEqualTo(newer)
        assertThat(File(gallery, "beach (1).jpg").readBytes()).isEqualTo(newer)
        assertThat(File(gallery, "beach (2).jpg").readBytes()).isEqualTo(content)
        assertThat(fileOps.scanned).containsExactly(File(gallery, "beach (2).jpg").path)
        assertThat(filesIn(gallery)).containsExactly("beach.jpg", "beach (1).jpg", "beach (2).jpg")
    }

    @Test fun unhide_parentGone_restoresToPicturesRestored() = runBlocking<Unit> {
        val content = bytes(3000)
        val dir = File(external, "WhatsApp/Private")
        val original = indexed(1, content, name = "doc.jpg", dir = dir)
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        assertThat(dir.delete()).isTrue()

        val res = manager().unhide(listOf(rows().single().id), unlock(VaultTier.STANDARD))

        assertThat(res.restored).isEqualTo(1)
        assertThat(original.exists()).isFalse()
        val restored = File(external, "Pictures/Restored/doc.jpg")
        assertThat(restored.readBytes()).isEqualTo(content)
        assertThat(fileOps.scanned).containsExactly(restored.path)
        assertThat(rows()).isEmpty()
    }

    @Test fun unhide_wrongTierKey_failsAndKeepsVaultCopy() = runBlocking<Unit> {
        val content = bytes(3000)
        val original = indexed(1, content)
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        val row = rows().single()

        val res = manager().unhide(listOf(row.id), unlock(VaultTier.HIGH_SECURITY))

        assertThat(res.restored).isEqualTo(0)
        assertThat(res.failed.map { it.first }).containsExactly(row.id)
        assertThat(original.exists()).isFalse()
        assertThat(filesIn(gallery)).isEmpty()
        assertThat(store.resolve(row.tier, row.vaultFile).exists()).isTrue()
        assertThat(rows()).hasSize(1)
    }

    @Test fun moveTier_reencryptsAndRemovesOldFile() = runBlocking<Unit> {
        val content = bytes(1_200_000)
        val original = indexed(1, content)
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        val old = rows().single()
        val oldFile = store.resolve(VaultTier.STANDARD, old.vaultFile)

        val res = manager().moveTier(listOf(old.id), VaultTier.HIGH_SECURITY, unlock(VaultTier.STANDARD))

        assertThat(res).isEqualTo(MoveResult(moved = 1, failed = emptyList()))
        assertThat(oldFile.exists()).isFalse()
        assertThat(encFiles(VaultTier.STANDARD)).isEmpty()
        assertThat(filesIn(store.thumbsDir(VaultTier.STANDARD))).containsExactly(".nomedia")
        val moved = rows().single()
        assertThat(moved.tier).isEqualTo(VaultTier.HIGH_SECURITY)
        assertThat(moved.plainSha256).isEqualTo(old.plainSha256)
        assertThat(moved.originalPath).isEqualTo(original.path)
        val hsPriv = unlock(VaultTier.HIGH_SECURITY)
        assertThat(decrypt(hsPriv, moved.wrappedFileKey, store.resolve(moved.tier, moved.vaultFile))).isEqualTo(content)
        assertThat(decrypt(hsPriv, moved.thumbWrappedKey!!, store.resolve(moved.tier, moved.thumbFile!!))).isNotEmpty()

        assertThat(manager().unhide(listOf(moved.id), hsPriv).restored).isEqualTo(1)
        assertThat(original.readBytes()).isEqualTo(content)
    }

    @Test fun moveTier_wrongKey_keepsOldCopy() = runBlocking<Unit> {
        indexed(1, bytes(3000))
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        val old = rows().single()

        val res = manager().moveTier(listOf(old.id), VaultTier.HIGH_SECURITY, unlock(VaultTier.HIGH_SECURITY))

        assertThat(res.moved).isEqualTo(0)
        assertThat(rows().single().id).isEqualTo(old.id)
        assertThat(store.resolve(VaultTier.STANDARD, old.vaultFile).exists()).isTrue()
        assertThat(encFiles(VaultTier.HIGH_SECURITY)).isEmpty()
    }

    // ---- store / startup --------------------------------------------------------------------

    @Test fun startup_removesPartialVaultFiles() = runBlocking<Unit> {
        indexed(1, bytes(3000))
        manager().hide(listOf(1), VaultTier.STANDARD, { _, _ -> })
        val kept = rows().single()
        store.ensure(VaultTier.HIGH_SECURITY)
        val partials = listOf(
            File(store.filesDir(VaultTier.STANDARD), "a.part"),
            File(store.thumbsDir(VaultTier.STANDARD), "a.thumb.part"),
            File(store.filesDir(VaultTier.HIGH_SECURITY), "b.part"),
            File(store.thumbsDir(VaultTier.HIGH_SECURITY), "c.tmp"),
        ).onEach { it.writeBytes(byteArrayOf(1)) }
        // Possibly the only copy of a file whose index row was lost: never deleted automatically.
        val orphan = File(store.filesDir(VaultTier.HIGH_SECURITY), "orphan.enc").apply { writeBytes(byteArrayOf(2)) }

        manager().cleanupPartials()

        partials.forEach { assertThat(it.exists()).isFalse() }
        assertThat(orphan.exists()).isTrue()
        assertThat(store.resolve(kept.tier, kept.vaultFile).exists()).isTrue()
        assertThat(store.resolve(kept.tier, kept.thumbFile!!).exists()).isTrue()
    }

    @Test fun standardTier_dirHasNomedia() {
        store.ensure(VaultTier.STANDARD)
        val root = store.root(VaultTier.STANDARD)
        assertThat(root.path).endsWith(".vault-gs")
        for (d in listOf(root, store.filesDir(VaultTier.STANDARD), store.thumbsDir(VaultTier.STANDARD))) {
            assertThat(File(d, ".nomedia").isFile).isTrue()
        }
        assertThat(VaultStore.STANDARD_ROOT).isEqualTo("/storage/emulated/0/.vault-gs")
    }

    // ---- key guard --------------------------------------------------------------------------

    @Test fun createVault_rejectsWeakSecretWithoutCreating() {
        val g = VaultKeyGuard(VaultStore(File(tmp.root, "s2"), File(tmp.root, "h2")), db.settingsDao()) { dir ->
            VaultKeys(dir, FakeKeyWrapper(), fast)
        }
        assertThrows(WeakSecretException::class.java) { runBlocking { g.createVault(VaultTier.STANDARD, "12345".toCharArray()) } }
        assertThrows(WeakSecretException::class.java) {
            runBlocking { g.createVault(VaultTier.HIGH_SECURITY, "short".toCharArray()) }
        }
        assertThat(g.keys(VaultTier.STANDARD).isCreated()).isFalse()
        assertThat(g.keys(VaultTier.HIGH_SECURITY).isCreated()).isFalse()
    }

    @Test fun createVault_crashBeforePin_repinsOnlyWithCorrectSecret() = runBlocking<Unit> {
        val s2 = VaultStore(File(tmp.root, "s3"), File(tmp.root, "h3"))
        val g = VaultKeyGuard(s2, db.settingsDao()) { dir -> VaultKeys(dir, FakeKeyWrapper(), fast) }
        g.keys(VaultTier.STANDARD).create("654321".toCharArray()) // keys written, pin never recorded
        db.settingsDao().put(SettingRow(VaultKeyGuard.pinKey(VaultTier.STANDARD), ""))
        assertThrows(VaultKeyMismatchException::class.java) { runBlocking { g.publicHandle(VaultTier.STANDARD) } }

        assertThrows(WrongSecretException::class.java) {
            runBlocking { g.createVault(VaultTier.STANDARD, "111111".toCharArray()) }
        }
        assertThrows(VaultKeyMismatchException::class.java) { runBlocking { g.publicHandle(VaultTier.STANDARD) } }

        g.createVault(VaultTier.STANDARD, "654321".toCharArray())
        assertThat(g.publicHandle(VaultTier.STANDARD)).isNotNull()
    }

    // ---- fakes ------------------------------------------------------------------------------

    private class FakeFileOps : FileOps {
        var free = Long.MAX_VALUE
        val undeletable = mutableSetOf<String>()
        val deleted = mutableListOf<String>()
        val scanned = mutableListOf<String>()
        val removedFromMediaStore = mutableListOf<Long>()
        val freeQueries = mutableListOf<File>()

        override fun exists(path: String) = File(path).exists()
        override fun freeBytes(dir: File): Long { freeQueries += dir; return free }
        override fun deleteOriginal(path: String): Boolean {
            if (path in undeletable) return false
            deleted += path
            return File(path).delete()
        }
        override fun scanIntoMediaStore(path: String) { scanned += path }
        override fun removeFromMediaStore(id: Long) { removedFromMediaStore += id }
    }

    private class FakeMediaSource : MediaSource {
        override suspend fun all() = emptyList<MediaItem>()
        override suspend fun changedSince(generation: Long) = emptyList<MediaItem>()
        override fun open(item: MediaItem): InputStream = error("HideManager reads files directly")
        override suspend fun thumbnail(item: MediaItem, px: Int): Bitmap? =
            Bitmap.createBitmap(px, px * 3 / 4, Bitmap.Config.ARGB_8888)
        override suspend fun exif(item: MediaItem): ExifData? = null
    }
}
