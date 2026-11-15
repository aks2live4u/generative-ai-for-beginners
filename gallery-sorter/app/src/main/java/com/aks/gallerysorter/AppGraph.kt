package com.aks.gallerysorter

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.aks.gallerysorter.classify.ml.LiteRtNsfwClassifier
import com.aks.gallerysorter.classify.ml.MlKitFaceEngine
import com.aks.gallerysorter.classify.ml.MlKitQrDetector
import com.aks.gallerysorter.classify.ml.MlKitSceneLabeler
import com.aks.gallerysorter.classify.ml.MlKitTextReader
import com.aks.gallerysorter.core.CategoryAssignment
import com.aks.gallerysorter.core.MediaSource
import com.aks.gallerysorter.core.MediaStoreSource
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.index.AppDatabase
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.lock.BiometricGate
import com.aks.gallerysorter.lock.LockState
import com.aks.gallerysorter.places.CityIndex
import com.aks.gallerysorter.scan.RetrieverFrameSource
import com.aks.gallerysorter.scan.ScanPipeline
import com.aks.gallerysorter.scan.ScanService
import com.aks.gallerysorter.scan.ScanSettings
import com.aks.gallerysorter.ui.Thumbnails
import com.aks.gallerysorter.vault.AndroidFileOps
import com.aks.gallerysorter.vault.HideManager
import com.aks.gallerysorter.vault.KeystoreKeyWrapper
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.TierSettings
import com.aks.gallerysorter.vault.VaultKeyGuard
import com.aks.gallerysorter.vault.VaultKeys
import com.aks.gallerysorter.vault.VaultStore
import com.aks.gallerysorter.vault.defaultTierFor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * The app's object graph (manual constructor injection), built once by [GallerySorterApp].
 * Holds the single [HideManager] (its Mutex serialises every vault operation app-wide) and the
 * single [LockState]. Everything touching the database is lazy, so nothing opens on the main
 * thread during `Application.onCreate`.
 */
class AppGraph(private val app: Application) {
    val appContext: Context get() = app
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: AppDatabase by lazy { AppDatabase.open(app) }
    val mediaSource: MediaSource = MediaStoreSource(app.contentResolver)
    val cityIndex: CityIndex by lazy { app.assets.open("cities.bin").use { CityIndex(it) } }
    val scanSettings: ScanSettings by lazy { ScanSettings(db.settingsDao()) }
    val tierSettings: TierSettings by lazy { TierSettings(db.settingsDao()) }
    val vaultStore: VaultStore = VaultStore.forDevice(app)
    val lockState = LockState()

    val guard: VaultKeyGuard by lazy {
        VaultKeyGuard(vaultStore, db.settingsDao()) { dir ->
            val tier = VaultTier.entries.first { VaultKeys.keysDir(vaultStore.root(it)) == dir }
            VaultKeys(dir, KeystoreKeyWrapper("vault_bio_${tier.name.lowercase()}"))
        }
    }

    val hideManager: HideManager by lazy {
        HideManager(
            store = vaultStore, guard = guard, vaultDao = db.vaultDao(), mediaDao = db.mediaDao(),
            labelDao = db.labelDao(), settings = db.settingsDao(), media = mediaSource, fileOps = AndroidFileOps(app),
        )
    }

    val biometricGate: BiometricGate by lazy { BiometricGate(guard, lockState, ::afterUnlock) }

    val thumbnails: Thumbnails by lazy { Thumbnails(mediaSource, vaultStore, lockState) }

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Short user-facing notices (no paths or names), shown as snackbars by the activity. */
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    fun notify(message: String) {
        _notices.tryEmit(message)
    }

    private val startup = CompletableDeferred<Unit>()

    /** Completes once [HideManager.cleanupPartials] has run; scans and hides wait for it. */
    suspend fun awaitStartup() = startup.await()

    internal fun runStartupCleanup() {
        appScope.launch(Dispatchers.IO) {
            try {
                hideManager.cleanupPartials()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "startup cleanup failed: ${e.javaClass.name}")
            } finally {
                startup.complete(Unit)
            }
        }
    }

    /** A pipeline on the real MediaStore and on-device models (closed after each run). Throws if a model can't load. */
    fun newScanPipeline(): ScanPipeline {
        val nsfw = LiteRtNsfwClassifier.create(app) ?: error("NSFW model unavailable")
        val faces = MlKitFaceEngine.create(app) ?: run { nsfw.close(); error("face model unavailable") }
        return ScanPipeline(
            source = mediaSource, db = db, nsfw = nsfw, faces = faces,
            text = MlKitTextReader(), scenes = MlKitSceneLabeler(), qr = MlKitQrDetector(),
            cityIndex = cityIndex, frames = RetrieverFrameSource(app), settings = scanSettings,
            autoHide = ::autoHide,
        )
    }

    /** Auto-hide: the item's default tier; locked mode when that tier is locked; skipped if its vault isn't set up. */
    private suspend fun autoHide(row: MediaRow, assignments: List<CategoryAssignment>) {
        val tier = defaultTierFor(assignments.map { it.category }, tierSettings.overrides())
        if (!guard.keys(tier).isCreated()) {
            Log.i(TAG, "auto-hide skipped for item ${row.id}: vault not set up")
            return
        }
        val res = hideManager.hide(listOf(row.id), tier, { _, _ -> }, priv = lockState.privateHandle(tier))
        if (res.failed.isNotEmpty() || res.aborted != null) Log.w(TAG, "auto-hide did not complete for item ${row.id}")
    }

    private val unlockJobs = mutableMapOf<VaultTier, Job>()

    /**
     * After an unlock: verify locked-mode hides, then rebuild rows from `.meta` if the tier has
     * vault files without rows (reinstall). Runs in the background and is cancelled when the
     * tier locks again, so the handle is not kept after lockAll.
     */
    private fun afterUnlock(tier: VaultTier, priv: PrivateHandle) {
        val job = appScope.launch(Dispatchers.IO) {
            try {
                if (lockState.privateHandle(tier) !== priv) return@launch
                val v = hideManager.verifyPending(tier, priv)
                if (v.failed.isNotEmpty()) notify("${v.failed.size} hidden items could not be verified")
                if (lockState.privateHandle(tier) !== priv) return@launch
                if (hasRowlessFiles(tier)) {
                    val n = hideManager.rebuildIndex(tier, priv)
                    if (n > 0) notify("Recovered $n hidden items")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "post-unlock check failed: ${e.javaClass.name}")
                notify("Vault check failed; it will run again at the next unlock")
            }
        }
        synchronized(unlockJobs) { unlockJobs.put(tier, job)?.cancel() }
    }

    internal fun watchLock() {
        appScope.launch {
            lockState.unlocked.collect { open ->
                synchronized(unlockJobs) {
                    unlockJobs.keys.filter { it !in open }.forEach { unlockJobs.remove(it)?.cancel() }
                }
                if (open.isEmpty()) thumbnails.clear()
            }
        }
    }

    private suspend fun hasRowlessFiles(tier: VaultTier): Boolean {
        val onDisk = vaultStore.filesDir(tier).listFiles().orEmpty()
            .filter { it.name.endsWith(VaultStore.ENC) }.map { "${VaultStore.FILES}/${it.name}" }.toSet()
        if (onDisk.isEmpty()) return false
        val known = db.vaultDao().fileRefs().filter { it.tier == tier }.map { it.vaultFile }.toSet()
        return (onDisk - known).isNotEmpty()
    }

    fun isCharging(): Boolean = app.getSystemService(BatteryManager::class.java)?.isCharging ?: false

    /** The first full scan waits for the charger when the user asked for that (spec 6.2). */
    suspend fun scanNeedsCharger(): Boolean = scanSettings.requiresCharging() && !isCharging()

    fun startScan() = ScanService.start(app)

    private companion object {
        const val TAG = "AppGraph"
    }
}

val Context.graph: AppGraph get() = (applicationContext as GallerySorterApp).graph
