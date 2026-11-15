package com.aks.gallerysorter

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.aks.gallerysorter.ui.summary
import com.aks.gallerysorter.vault.AndroidFileOps
import com.aks.gallerysorter.vault.AutoHider
import com.aks.gallerysorter.vault.HideManager
import com.aks.gallerysorter.vault.KeystoreKeyWrapper
import com.aks.gallerysorter.vault.PrivateHandle
import com.aks.gallerysorter.vault.TierSettings
import com.aks.gallerysorter.vault.VaultKeyGuard
import com.aks.gallerysorter.vault.VaultKeys
import com.aks.gallerysorter.vault.VaultStore
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            labelDao = db.labelDao(), settings = db.settingsDao(), unhidden = db.unhiddenDao(), media = mediaSource,
            fileOps = AndroidFileOps(app), privateDirs = appPrivateDirs(),
        )
    }

    /** Folders an unhide must never write into: the app's internal data and its external app folders. */
    private fun appPrivateDirs(): List<File> =
        listOf(app.dataDir) + listOf("Android/data", "Android/obb", "Android/media").map {
            File(VaultStore.EXTERNAL_ROOT, "$it/${app.packageName}")
        }

    private val autoHider: AutoHider by lazy {
        AutoHider(
            hideManager = hideManager, guard = guard, tierSettings = tierSettings, settings = db.settingsDao(),
            unhidden = db.unhiddenDao(), privateHandle = lockState::privateHandle,
            blocked = ::postAutoHideBlocked, incomplete = { tier, res -> notify(res.summary(tier, verb = "Auto-hide: hid")) },
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

    /** A flow over the index whose lazy database is resolved on IO (never on the main thread). */
    fun <T> dbFlow(query: (AppDatabase) -> Flow<T>): Flow<T> = flow { emitAll(query(db)) }.flowOn(Dispatchers.IO)

    /**
     * Which vault tiers are set up in this install (keys exist and are pinned); checked on IO.
     * Keys left by an earlier install don't count until they are re-pinned with their secret.
     */
    suspend fun createdTiers(): Set<VaultTier> = withContext(Dispatchers.IO) {
        VaultTier.entries.filter { guard.isSetUp(it) }.toSet()
    }

    private val startup = CompletableDeferred<Unit>()

    /** Completes once [HideManager.cleanupPartials] has run; scans and hides wait for it. */
    suspend fun awaitStartup() = startup.await()

    /**
     * Startup, on IO: opens the index (Keystore passphrase + SQLCipher) so the UI never does it on
     * the main thread, then [HideManager.cleanupPartials]; completes [awaitStartup] either way.
     */
    internal fun runStartup() {
        appScope.launch(Dispatchers.IO) {
            try {
                db.openHelper.writableDatabase
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

    /**
     * Auto-hide ([AutoHider]): the item's default tier if it is in use (unlocked in the last 30
     * days), else the standard tier, else skipped with one neutral notification a day. Locked
     * mode when the tier is locked. Content the user unhid before is never re-hidden.
     */
    private suspend fun autoHide(row: MediaRow, assignments: List<CategoryAssignment>) = autoHider.autoHide(row, assignments)

    /** Neutral text only: no thumbnail, no names, no counts. Needs the notification permission. */
    private fun postAutoHideBlocked() {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = app.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(NOTICES_CHANNEL, app.getString(R.string.notices_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { setShowBadge(false) },
        )
        val open = PendingIntent.getActivity(
            app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(app, NOTICES_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(app.getString(R.string.autohide_blocked_title))
            .setContentText(app.getString(R.string.autohide_blocked_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(AUTO_HIDE_BLOCKED_ID, n)
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
                guard.recordUnlock(tier) // auto-hide only targets tiers unlocked recently
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
        const val NOTICES_CHANNEL = "notices"
        const val AUTO_HIDE_BLOCKED_ID = 2001
    }
}

val Context.graph: AppGraph get() = (applicationContext as GallerySorterApp).graph
