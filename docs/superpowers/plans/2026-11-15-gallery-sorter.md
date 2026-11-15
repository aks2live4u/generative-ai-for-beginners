# Private Gallery Sorter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An offline Android gallery app for the owner's vivo X200 that sorts photos and videos into
categories on the device, hides any category in one tap into an encrypted two-tier vault, and suggests
duplicate deletions.

**Architecture:** A single-module Kotlin/Compose app in `gallery-sorter/`, organised by feature package.
Decision logic (rules, hashing, grouping, crypto, filtering) is pure Kotlin with JVM unit tests. Android-bound
parts (MediaStore, ML Kit, LiteRT, Keystore, UI) sit behind small interfaces so the pipeline can be tested with fakes.
A foreground scan service writes labels into an encrypted Room DB. The UI reads only from that DB.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.3, Gradle wrapper 8.11.1, compileSdk/targetSdk 35, minSdk 30,
Jetpack Compose (BOM 2024.12.01), Room 2.6.1 + `net.zetetic:sqlcipher-android:4.6.1`, WorkManager 2.10.0,
Media3 1.5.1, `androidx.biometric:biometric:1.1.0`, `androidx.exifinterface:exifinterface:1.3.7`,
Tink `com.google.crypto.tink:tink-android:1.15.0`, BouncyCastle `bcprov-jdk18on:1.79` (Argon2id),
ML Kit bundled: `face-detection:16.1.7`, `text-recognition:16.0.1`, `image-labeling:17.0.9`,
`barcode-scanning:17.3.0`, LiteRT `com.google.ai.edge.litert:litert:1.0.1` (+ gpu). Tests: JUnit 4, Truth 1.4.4,
Robolectric 4.14.1, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-15-gallery-sorter-design.md`

## Global Constraints

- The merged manifest must contain no `android.permission.INTERNET` and no `ACCESS_NETWORK_STATE`. Enforced by the Gradle task `verifyNoNetwork`, which `check` and `assemble*` depend on.
- No runtime model downloads. All ML models are bundled (ML Kit bundled artifacts only, never the `play-services-mlkit-*` unbundled ones).
- `android:allowBackup="false"`, plus data-extraction rules that exclude all domains.
- Package `com.aks.gallerysorter`; app label "Gallery Sorter"; normal icon (no disguise, no decoy).
- Never copy user files. Sorting is virtual (DB rows). Hiding is encrypt → verify → delete original, one file at a time.
- Never delete without explicit user confirmation. Duplicate deletes go through `MediaStore.createTrashRequest`.
- Thresholds: NSFW explicit ≥ 0.85 → Sensitive; 0.50–0.85 → Review. pHash Hamming ≤ 4 = visually identical. Video duration tolerance ±0.5 s.
- Standard vault: `/storage/emulated/0/.vault-gs/` + `.nomedia`, fingerprint or PIN of 6+ digits; survives uninstall.
- High-security vault: `context.filesDir/hsvault/`, passcode of 10+ characters (letters allowed) plus optional fingerprint; deleted on uninstall (shown at setup).
- Default tier: My nudes, IDs → high-security; Other adult content, Banking, Other documents → standard. User-editable per category.
- "Auto-hide new Sensitive items" defaults to off, and is offered on first run.
- While locked: Sensitive, Documents and Review-band items are excluded from every view; vaulted items appear only in the Vault. `FLAG_SECURE` on all lockable screens.
- Notifications use neutral text and never show thumbnails.

## Review Focus

1. **Hide interrupted** (process killed, storage full, power loss) → the original is never deleted unless the vault file has been fully written and verified. Leftover partial vault files are cleaned up on next start. *(Task 12: `hide_interruptedBeforeVerify_keepsOriginal`, `startup_removesPartialVaultFiles`, `hide_insufficientSpace_failsWithoutTouchingOriginal`)*
2. **Unreadable media** (0-byte, corrupt JPEG, missing EXIF, an unsupported or huge video) → the scanner marks it `UNREADABLE`, moves on, and never retries in a crash loop. *(Task 10: `pipeline_corruptFile_marksUnreadableAndContinues`)*
3. **File changed under us** (another app deleted or moved the file between indexing and an action) → hide, unhide and delete report "file no longer exists", remove the stale row, and don't crash. *(Task 12: `hide_missingFile_reportsAndDropsRow`)*
4. **Unhide target already occupied** (a new file now sits at the original path) → restore to `name (1).ext`; never overwrite. *(Task 12: `unhide_pathOccupied_restoresWithSuffix`)*
5. **Very large libraries** (50,000+ items) → duplicate grouping isn't O(n²), and grids page from the DB. *(Task 6: `grouping_50kItems_completesUnder2s`)*

---

## File Structure (all under `gallery-sorter/app/src/main/java/com/aks/gallerysorter/`)

| Package / file | Responsibility |
|---|---|
| `core/MediaItem.kt` | `MediaItem` data class, `MediaKind`, `Category` enum, `CategoryAssignment`. |
| `core/MediaSource.kt` | Interface over MediaStore, plus `MediaStoreSource` implementation. |
| `index/AppDatabase.kt`, `index/entities/*.kt`, `index/dao/*.kt` | Room schema on SQLCipher. |
| `index/DbKeyProvider.kt` | A Keystore-held key (no user auth) that opens the DB. |
| `classify/SourceRules.kt` | Path/EXIF → Source categories (pure). |
| `classify/DocumentRules.kt` | OCR text → IDs / Banking / Other docs (pure). |
| `classify/MineRule.kt` | Explicit item → My nudes (sub-folders) / Other adult (pure). |
| `classify/Categorizer.kt` | Merges all signals → `List<CategoryAssignment>` + review flag (pure). |
| `classify/ml/*.kt` | `NsfwClassifier`, `FaceEngine`, `TextReader`, `SceneLabeler`, `QrDetector` interfaces and implementations. |
| `classify/BlurDetector.kt`, `classify/FaceClusterer.kt` | Pure image/vector math. |
| `places/CityIndex.kt`, `places/TripDetector.kt` | Offline nearest city + trips (pure). |
| `dupes/Hashing.kt`, `dupes/DuplicateGrouper.kt`, `dupes/KeepSuggester.kt` | SHA-256, pHash, grouping, keep pick (pure). |
| `scan/ScanPipeline.kt`, `scan/ScanService.kt`, `scan/ScanWorker.kt`, `scan/MediaObserver.kt` | Indexing orchestration. |
| `vault/VaultKeys.kt`, `vault/VaultCrypto.kt`, `vault/VaultStore.kt`, `vault/HideManager.kt` | Crypto, storage tiers, hide/unhide/move. |
| `lock/LockState.kt`, `lock/LockFilter.kt`, `lock/BiometricGate.kt` | Lock session + query filtering. |
| `ui/**` | Compose screens and ViewModels. |
| `gallery-sorter/tools/build_cities.py` | Builds `assets/cities.bin` from GeoNames `cities15000.txt`. |

Tests mirror packages under `app/src/test/java/com/aks/gallerysorter/`.

---

### Task 1: Project scaffold, SDK and the no-network guard

**Files:**
- Create: `gallery-sorter/settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, wrapper files
- Create: `gallery-sorter/app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `MainActivity.kt` (empty Compose screen)
- Create: `gallery-sorter/scripts/setup-sdk.sh` (installs cmdline-tools from dl.google.com; installs `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`; writes `local.properties`)
- Test: `app/src/test/.../ManifestGuardTest.kt`

**Interfaces:**
- Produces: Gradle task `:app:verifyNoNetwork`, which reads each variant's merged manifest and fails if it lists `INTERNET` or `ACCESS_NETWORK_STATE`.

- [ ] **Step 1:** Run `gallery-sorter/scripts/setup-sdk.sh`. Expected: `sdkmanager --list_installed` shows platform 35 and build-tools 35.0.0.
- [ ] **Step 2:** Write the manifest with all permissions from spec 6.1, plus `<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>` and the same for `ACCESS_NETWORK_STATE`. Add `allowBackup="false"` and `dataExtractionRules`.
- [ ] **Step 3:** Write `verifyNoNetwork` in `app/build.gradle.kts`, wired via `androidComponents.onVariants` to the `processXManifest` output. Make every `assemble<Variant>` task and `check` depend on it.
- [ ] **Step 4:** Temporarily add a test dependency that merges `INTERNET` into the debug manifest. Run `./gradlew :app:verifyNoNetwork`. Expected: FAIL naming INTERNET. Remove the dependency.
- [ ] **Step 5:** Run `./gradlew :app:assembleDebug :app:verifyNoNetwork`. Expected: BUILD SUCCESSFUL and an APK in `app/build/outputs/apk/debug/`.
- [ ] **Step 6:** Commit `feat(gallery-sorter): scaffold app with no-network guard`.

### Task 2: Encrypted index database

**Files:**
- Create: `core/MediaItem.kt`, `index/AppDatabase.kt`, `index/DbKeyProvider.kt`, `index/entities/{MediaRow,LabelRow,FaceRow,PersonRow,OverrideRow,VaultRow,DupeRow,SettingRow}.kt`, `index/dao/{MediaDao,LabelDao,PeopleDao,VaultDao,DupeDao,SettingsDao}.kt`
- Test: `index/MediaDaoTest.kt` (Robolectric, in-memory, no SQLCipher)

**Interfaces:**
- Produces:
  - `enum class MediaKind { IMAGE, VIDEO }`
  - `enum class Category { MY_NUDES, MY_NUDES_CAMERA, MY_NUDES_OF_ME, OTHER_ADULT, IDS, BANKING, OTHER_DOCS, CAMERA, SCREENSHOTS, SCREEN_RECORDINGS, WHATSAPP, TELEGRAM, OTHER_MESSAGING, DOWNLOADS, SELFIES, PETS, FOOD, NATURE, MEMES, QR_CODES, BLURRY, DUPLICATES, REVIEW }` with `val group: CategoryGroup` (`SENSITIVE, DOCUMENTS, SOURCE, CONTENT, CLEANUP, REVIEW`)
  - `data class MediaItem(val id: Long, val path: String, val kind: MediaKind, val sizeBytes: Long, val mime: String, val width: Int, val height: Int, val dateTakenMs: Long?, val durationMs: Long?, val generation: Long)`
  - `data class CategoryAssignment(val category: Category, val confidence: Float)`
  - `MediaRow.status: ScanStatus` (`PENDING, DONE, UNREADABLE`); `MediaRow.sha256: String?`, `phash: Long?`, `personIds`, `lat/lon: Double?`, `city: String?`
  - `LabelDao.itemsIn(category: Category, includeLocked: Boolean): PagingSource<Int, MediaRow>`
  - `LabelDao.counts(): Flow<Map<Category, Int>>`
  - `OverrideRow(mediaId, category, isExcluded: Boolean)`: user corrections that are never re-classified
  - `VaultRow(id, originalPath, tier: VaultTier, vaultFile, kind, sizeBytes, categories, wrappedFileKey: ByteArray, thumbFile)`
  - `enum class VaultTier { STANDARD, HIGH_SECURITY }`
  - `DbKeyProvider.passphrase(): ByteArray`: AES key from Android Keystore alias `db_key`, `setUserAuthenticationRequired(false)`; wraps a random 32-byte passphrase stored in `noBackupFilesDir/db.key.enc`.

- [ ] **Step 1:** Write `MediaDaoTest` cases. `upsert_thenItemsIn_returnsItem`. `itemsIn_lockedExcludesSensitiveDocsReview` (an item labelled OTHER_ADULT plus CAMERA is absent from CAMERA when `includeLocked=false` and present when `true`). `override_excludedCategory_neverReturned`.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*MediaDaoTest*'`. Expected: FAIL (classes missing).
- [ ] **Step 3:** Implement the entities, DAOs and `AppDatabase.open(context)`. Use `SupportOpenHelperFactory(DbKeyProvider.passphrase())` in production and `Room.inMemoryDatabaseBuilder` in tests. The locked exclusion is SQL: `NOT EXISTS (label in SENSITIVE/DOCUMENTS group, or confidence in the review band)`.
- [ ] **Step 4:** Run the tests. Expected: PASS.
- [ ] **Step 5:** Commit `feat(index): encrypted Room schema with locked filtering`.

### Task 3: MediaStore source and incremental delta

**Files:**
- Create: `core/MediaSource.kt`, `scan/Delta.kt`
- Test: `scan/DeltaTest.kt`

**Interfaces:**
- Produces:
  - `interface MediaSource { suspend fun all(): List<MediaItem>; suspend fun changedSince(generation: Long): List<MediaItem>; fun open(item: MediaItem): InputStream; suspend fun thumbnail(item: MediaItem, px: Int = 320): Bitmap?; suspend fun exif(item: MediaItem): ExifData? }`
  - `data class ExifData(val make: String?, val model: String?, val lat: Double?, val lon: Double?, val lensFacingFront: Boolean?)`
  - `fun computeDelta(known: Map<Long, Long /*generation*/>, current: List<MediaItem>): Delta`, where `Delta(added, changed, removedIds)`
- `MediaStoreSource` queries both `Images` and `Video` `EXTERNAL_CONTENT_URI`. Location uses `MediaStore.setRequireOriginal`. Thumbnails use `ContentResolver.loadThumbnail`.

- [ ] **Step 1:** Write `DeltaTest`: `newItem_isAdded`, `higherGeneration_isChanged`, `missingFromCurrent_isRemoved`, `unchanged_isIgnored`.
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Implement `computeDelta` and `MediaStoreSource`.
- [ ] **Step 4:** Run it. Expected: PASS. Then run `assembleDebug`. Expected: SUCCESS.
- [ ] **Step 5:** Commit `feat(scan): MediaStore source and delta`.

### Task 4: Source rules

**Files:** Create `classify/SourceRules.kt`; Test `classify/SourceRulesTest.kt`

**Interfaces:**
- Produces: `fun sourceCategories(path: String, kind: MediaKind, exif: ExifData?, ownCameraModels: Set<String>): Set<Category>` and `fun isOwnCameraOriginal(path: String, exif: ExifData?, ownCameraModels: Set<String>): Boolean`

- [ ] **Step 1:** Write the tests:
  - `DCIM/Camera/IMG_1.jpg` with make "vivo", model "V2405A" in own models → `{CAMERA}`, and `isOwnCameraOriginal` is true
  - `Pictures/Screenshots/x.png` → `{SCREENSHOTS}`; `DCIM/Screenshots/x.jpg` → `{SCREENSHOTS}` (vivo)
  - a video under `Movies/ScreenRecord/` or `DCIM/ScreenRecorder/` → `{SCREEN_RECORDINGS}`
  - `Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/a.jpg` → `{WHATSAPP}`
  - `Pictures/Telegram/a.jpg` and `Telegram/Telegram Images/a.jpg` → `{TELEGRAM}`
  - `Download/a.jpg` → `{DOWNLOADS}`
  - `Pictures/Instagram/a.jpg` → `{OTHER_MESSAGING}`
  - `DCIM/Camera/a.jpg` with EXIF model not in the own set → `{CAMERA}` but `isOwnCameraOriginal` false
  - path matching is case-insensitive
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Implement with an ordered list of `(Regex, Category)`. Other-messaging folders: Instagram, Messenger, Signal, Snapchat, Facebook. If the own-models set is empty, the current `Build.MODEL` is supplied by the caller.
- [ ] **Step 4:** Run it. Expected: PASS.
- [ ] **Step 5:** Commit `feat(classify): source rules`.

### Task 5: Document rules

**Files:** Create `classify/DocumentRules.kt`; Test `classify/DocumentRulesTest.kt`

**Interfaces:**
- Produces: `fun documentCategory(ocrText: String): CategoryAssignment?` (IDS beats BANKING beats OTHER_DOCS), `fun luhnValid(digits: String): Boolean`

- [ ] **Step 1:** Write the tests:
  - `"4111 1111 1111 1111"` → BANKING; `"4111 1111 1111 1112"` (fails Luhn) alone → not BANKING
  - `"IFSC: HDFC0001234"` → BANKING; `"Paid to rahul@okaxis via UPI"` → BANKING; `"Account Statement ... Closing Balance"` → BANKING
  - `"P<INDDOE<<JOHN<<<<<<<<"` + a second MRZ line → IDS; `"ABCDE1234F"` with "Income Tax Department" → IDS; `"1234 5678 9012"` with "Aadhaar" or "Government of India" → IDS; "Driving Licence" + DL number → IDS
  - 60+ words of general text → OTHER_DOCS with confidence ≥ 0.6
  - `"lol"` → null
  - Aadhaar-like 12 digits without any ID keyword → not IDS (avoids flagging phone numbers)
- [ ] **Step 2:** Run it. Expected: FAIL.
- [ ] **Step 3:** Implement as regex plus keyword scoring. Confidence: two or more signals → 0.95; one strong pattern (Luhn card, IFSC, MRZ) → 0.9; one keyword only → 0.6 (Review band).
- [ ] **Step 4:** Run it. Expected: PASS.
- [ ] **Step 5:** Commit `feat(classify): document rules`.

### Task 6: Hashing, duplicate grouping, keep suggestion

**Files:** Create `dupes/Hashing.kt`, `dupes/DuplicateGrouper.kt`, `dupes/KeepSuggester.kt`; Test `dupes/*Test.kt`

**Interfaces:**
- Produces:
  - `fun sha256(input: InputStream): String`
  - `fun pHash(gray32x32: IntArray): Long` (DCT 32×32 → top-left 8×8 excluding DC → bits vs median)
  - `fun hamming(a: Long, b: Long): Int`
  - `data class DupeCandidate(val id: Long, val kind: MediaKind, val sizeBytes: Long, val sha256: String?, val phash: Long?, val durationMs: Long?, val framePhashes: List<Long>, val width: Int, val height: Int, val isOwnCameraOriginal: Boolean, val exifDateMs: Long?)`
  - `fun groupDuplicates(items: List<DupeCandidate>): List<List<DupeCandidate>>`
  - `fun suggestKeep(group: List<DupeCandidate>): DupeCandidate` (highest width×height, then camera original, then earliest EXIF date, then lowest id)

- [ ] **Step 1:** Write the tests:
  - `sha256` of "abc" equals `ba7816bf...15ad`
  - pHash of an image vs the same image downscaled and noise-added → hamming ≤ 4; vs a different image → > 10
  - two items with the same sha → one group; same phash ±3 bits → one group; dates ignored (items 3 years apart still group)
  - videos group only if duration is within 500 ms *and* all 3 frame phashes are within 4
  - `suggestKeep` picks 4000×3000 over 1600×1200 and the camera original on ties
  - `grouping_50kItems_completesUnder2s`: 50,000 random candidates with 200 planted pairs → finds exactly those pairs in < 2 s
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement. Exact matches: bucket by size, then sha. Near matches: split each 64-bit pHash into 5 bands (13/13/13/13/12 bits) and bucket by each band. By pigeonhole, any pair ≤ 4 bits apart is identical in at least one band. Compare only within buckets, then merge with union-find.
- [ ] **Step 4:** Run them. Expected: PASS.
- [ ] **Step 5:** Commit `feat(dupes): hashing, grouping, keep suggestion`.

### Task 7: Offline places and trips

**Files:** Create `places/CityIndex.kt`, `places/TripDetector.kt`, `tools/build_cities.py`, `app/src/main/assets/cities.bin`; Test `places/*Test.kt`

**Interfaces:**
- Produces:
  - `class CityIndex(bin: InputStream) { fun nearest(lat: Double, lon: Double): City? }`, with `City(name, country, lat, lon)`. Returns null if more than 50 km away.
  - `fun detectTrips(points: List<Pair<Long /*dateMs*/, City>>): List<Trip>`, with `Trip(name: String, startMs, endMs, itemIds)`. Home = the most frequent city. A trip is a run of consecutive days whose items are > 100 km from home, split by a gap of more than 2 days. Name = most frequent city + month/year.
- `cities.bin` format: int count, then per city: float lat, float lon, UTF name, UTF country code. Sorted by lat for a binary-search window.

- [ ] **Step 1:** Run `python3 tools/build_cities.py`. It downloads `https://download.geonames.org/export/dump/cities15000.zip` into the scratchpad and writes `cities.bin`. If the host is blocked, ask the owner before using another source. Expected: the file is around 1.5 MB. Add a `CC-BY GeoNames` credit line in the app's About screen.
- [ ] **Step 2:** Write the tests. `nearest(19.07, 72.88)` → Mumbai; `nearest(28.61, 77.21)` → Delhi or New Delhi; mid-ocean → null. A trip test: 20 home days, then 4 Goa days, then home → one trip "Panaji · Mon YYYY" (or the nearest Goa city in the data).
- [ ] **Step 3:** Run them. Expected: FAIL.
- [ ] **Step 4:** Implement.
- [ ] **Step 5:** Run them. Expected: PASS.
- [ ] **Step 6:** Commit `feat(places): offline city lookup and trips`.

### Task 8: ML wrappers, blur and face clustering

**Files:** Create `classify/ml/{NsfwClassifier,FaceEngine,TextReader,SceneLabeler,QrDetector}.kt`, `classify/BlurDetector.kt`, `classify/FaceClusterer.kt`, plus model files `assets/models/nsfw.tflite` and `assets/models/face_embed.tflite`, and `assets/models/LICENSES.md`; Test `classify/BlurDetectorTest.kt`, `classify/FaceClustererTest.kt`

**Interfaces:**
- Produces:
  - `interface NsfwClassifier { fun score(bmp: Bitmap): NsfwScore }`, with `NsfwScore(explicit: Float, suggestive: Float)`
  - `interface FaceEngine { suspend fun faces(bmp: Bitmap): List<FaceHit> }`, with `FaceHit(box: RectF, embedding: FloatArray /*L2-normalised*/)`
  - `interface TextReader { suspend fun read(bmp: Bitmap): String }`
  - `interface SceneLabeler { suspend fun labels(bmp: Bitmap): Map<String, Float> }`
  - `interface QrDetector { suspend fun hasCode(bmp: Bitmap): Boolean }`
  - `fun blurScore(gray: IntArray, w: Int, h: Int): Double` (variance of Laplacian); `BLUR_THRESHOLD = 60.0`
  - `class FaceClusterer(threshold: Float = 0.6f) { fun assign(embedding: FloatArray, centroids: Map<Long, FloatArray>): Long? }` (cosine similarity ≥ threshold → nearest person id, else null → new person)
- Implementations: `LiteRtNsfwClassifier` (224×224 input, GPU delegate with CPU fallback), `MlKitFaceEngine` (ML Kit detect → crop to 112×112 → `face_embed.tflite`), `MlKitTextReader`, `MlKitSceneLabeler`, `MlKitQrDetector`.
- Model sourcing: NSFW = the MobileNetV2 NSFW model from GantMan/nsfw_model (MIT), converted to TFLite if no TFLite release exists (classes drawings/hentai/neutral/porn/sexy → explicit = porn + hentai, suggestive = sexy). Face embeddings = a MobileFaceNet TFLite under a permissive licence. Record the source URL, licence and SHA-256 of each in `LICENSES.md`. If no permissive model can be obtained, stop and report to the owner.

- [ ] **Step 1:** Write the tests. `blurScore` is lower for a box-blurred checkerboard than for a sharp one, and a sharp one is > 60. `FaceClusterer`: embeddings at cosine 0.9 → the same id; at 0.3 → null.
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement the pure classes and the Android wrappers. Fetch the models, and add `noCompress "tflite"` to the build.
- [ ] **Step 4:** Run the tests, then `assembleDebug`. Expected: PASS and SUCCESS. Then `verifyNoNetwork` still passes, because ML Kit's manifests are stripped.
- [ ] **Step 5:** Commit `feat(classify): ML wrappers, blur and face clustering`.

### Task 9: Mine rule and categorizer

**Files:** Create `classify/MineRule.kt`, `classify/Categorizer.kt`; Test `classify/MineRuleTest.kt`, `classify/CategorizerTest.kt`

**Interfaces:**
- Consumes: `NsfwScore`, `sourceCategories`, `isOwnCameraOriginal`, `documentCategory`, the scene labels map, `blurScore`, and `FaceHit` person ids.
- Produces:
  - `fun mineCategories(isOwnCameraOriginal: Boolean, facePersonIds: Set<Long>, mePersonId: Long?): Set<Category>`, which returns any of `MY_NUDES_CAMERA`, `MY_NUDES_OF_ME` (plus `MY_NUDES` if either applies), else `{OTHER_ADULT}`
  - `data class Signals(val nsfw: NsfwScore?, val sources: Set<Category>, val ownCamera: Boolean, val doc: CategoryAssignment?, val labels: Map<String, Float>, val hasQr: Boolean, val blur: Double?, val faces: List<Long>, val frontCamera: Boolean?, val largeSingleFace: Boolean)`
  - `fun categorize(s: Signals, mePersonId: Long?, overrides: List<OverrideRow>): List<CategoryAssignment>`
- Rules:
  - explicit ≥ 0.85 → mine categories at that confidence
  - 0.50 ≤ explicit < 0.85 → `REVIEW` plus a tentative mine category at that confidence
  - selfie = front camera, or a single face covering > 20% of the area
  - label map: Dog/Cat/Pet → PETS; Food/Dish/Cuisine → FOOD; Plant/Sky/Mountain/Beach/Flower/Tree → NATURE (≥ 0.7)
  - meme = has text and is not from Camera, with the label "Poster" or "Cartoon"
  - `blur < 60` → BLURRY
  - a doc confidence of 0.6 → also REVIEW
  - an excluded override removes its category; an included override forces it at 1.0

- [ ] **Step 1:** Write the tests:
  - own camera + no face → `{MY_NUDES, MY_NUDES_CAMERA}`
  - not own camera + "Me" face → `{MY_NUDES, MY_NUDES_OF_ME}`
  - both → all three
  - neither → `{OTHER_ADULT}`
  - a WhatsApp image with explicit 0.95 and no Me face → OTHER_ADULT + WHATSAPP
  - explicit 0.6 → REVIEW is present and the tentative category confidence is 0.6
  - an override excluding OTHER_ADULT → absent
  - a dog photo → PETS
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run them. Expected: PASS.
- [ ] **Step 5:** Commit `feat(classify): mine rule and categorizer`.

### Task 10: Scan pipeline, service and observer

**Files:** Create `scan/ScanPipeline.kt`, `scan/ScanService.kt`, `scan/ScanWorker.kt`, `scan/MediaObserver.kt`, `scan/ScanSettings.kt`; Test `scan/ScanPipelineTest.kt` (fakes for every interface, plus an in-memory DB)

**Interfaces:**
- Consumes: Tasks 2–9.
- Produces:
  - `class ScanPipeline(source, db, nsfw, faces, text, scenes, qr, cityIndex, autoHide: (MediaRow, List<CategoryAssignment>) -> Unit)`, with `suspend fun runIncremental(progress: (done: Int, total: Int) -> Unit)` and `suspend fun process(item: MediaItem)`
  - `ScanService`: a foreground service of type `dataSync`. The notification says "Organising your gallery — N%" (no thumbnails). Started from the UI and by `ScanWorker`.
  - `ScanWorker`: periodic every 6 h, with `requiresCharging` per the `ScanSettings.onlyWhileCharging` setting (default true for the first full scan, false for incremental scans).
  - `MediaObserver`: registers on the Images and Video URIs, and enqueues a debounced (10 s) one-off `ScanWorker`.
- Order per item:
  1. thumbnail (videos: up to 5 frames at 1 s, 25%, 50%, 75%, end; the max score per label is used)
  2. NSFW
  3. faces → cluster
  4. scene labels and QR
  5. OCR only if the labels include Document/Text/Paper/Receipt, or it's a screenshot, or the aspect ratio is within 1.3–1.5 or > 1.9
  6. blur
  7. sha256 (only for size collisions; computed lazily in the dupes pass)
  8. pHash
  9. EXIF → city
  10. categorize → write labels
  11. if auto-hide is on and Sensitive with confidence ≥ 0.85 → call `autoHide`

  Any exception marks the item `UNREADABLE` and the scan continues. `UNREADABLE` items are retried only if their generation changes.
- After each run: recompute duplicate groups (Task 6) and trips (Task 7), and write `DupeRow`.

- [ ] **Step 1:** Write the tests:
  - `pipeline_classifiesAndWritesLabels` (fake NSFW 0.95, own camera → MY_NUDES_CAMERA row)
  - `pipeline_corruptFile_marksUnreadableAndContinues` (the fake thumbnail throws for item 2 of 3 → 1 and 3 are DONE, 2 is UNREADABLE; a second run doesn't reprocess 2)
  - `pipeline_skipsOcrForCameraPhotoWithoutDocLabels`
  - `pipeline_autoHideCalledOnlyWhenEnabledAndConfident`
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement the pipeline, then the service, worker and observer.
- [ ] **Step 4:** Run the tests and `assembleDebug`. Expected: PASS and SUCCESS.
- [ ] **Step 5:** Commit `feat(scan): pipeline, foreground service, observer`.

### Task 11: Vault keys and crypto

**Files:** Create `vault/VaultKeys.kt`, `vault/VaultCrypto.kt`; Test `vault/VaultCryptoTest.kt`, `vault/VaultKeysTest.kt` (JVM; Tink and BouncyCastle are pure Java)

**Interfaces:**
- Produces:
  - `class VaultKeys(dir: File, keystore: KeyWrapper)`, one instance per tier. `fun create(secret: CharArray)` makes a Tink hybrid keyset (`DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM`). It writes `public.keyset` in plain form, and `private.pin.enc` = the private keyset encrypted with AES-256-GCM using an Argon2id-derived key (t=3, m=64 MiB, p=1, 16-byte salt).
  - `fun unlockWithSecret(secret: CharArray): PrivateHandle` (throws `WrongSecretException`), `fun enableBiometric(handle: PrivateHandle)`, `fun unlockWithBiometric(cipher: Cipher): PrivateHandle`, `fun changeSecret(handle: PrivateHandle, newSecret: CharArray)`
  - `interface KeyWrapper` is the Android Keystore AES key with `setUserAuthenticationRequired(true)` and biometric strong. Tests use a fake.
  - `object VaultCrypto { fun encrypt(pub: PublicHandle, input: InputStream, out: OutputStream): ByteArray /*wrapped per-file key*/; fun decryptingStream(priv: PrivateHandle, wrappedKey: ByteArray, file: File): SeekableByteChannel }`. Per file: a random Tink `StreamingAead` keyset (`AES256_GCM_HKDF_1MB`), wrapped with hybrid encryption.
  - Key file locations: standard = `.vault-gs/keys/`; high-security = `filesDir/hsvault/keys/`.
- Secret rules: standard needs ≥ 6 digits; high-security needs ≥ 10 characters. Enforced by `fun validateSecret(tier: VaultTier, s: CharArray): Boolean`.

- [ ] **Step 1:** Write the tests:
  - round trip for 0 bytes, 1 byte, 3 MB random → byte-identical
  - seeking into the middle of the decrypting channel returns the correct bytes
  - wrong secret → `WrongSecretException`
  - after `create`, encrypting needs only the public file; deleting `private.pin.enc` doesn't stop `encrypt`
  - a tampered ciphertext byte → decrypt throws
  - `validateSecret(STANDARD, "12345")` false, `"123456"` true; `validateSecret(HIGH_SECURITY, "abc123defg")` true, `"abc123def"` false
  - `changeSecret` → old secret fails, new one works, and old files still decrypt
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement. Zero out secret char arrays after use.
- [ ] **Step 4:** Run them. Expected: PASS.
- [ ] **Step 5:** Commit `feat(vault): two-tier keys and streaming crypto`.

### Task 12: Vault store and hide manager

**Files:** Create `vault/VaultStore.kt`, `vault/HideManager.kt`, `vault/FileOps.kt`; Test `vault/HideManagerTest.kt` (temp dirs, fake `FileOps`/MediaStore)

**Interfaces:**
- Consumes: `VaultKeys`, `VaultCrypto`, `VaultDao`, `MediaDao`.
- Produces:
  - `interface FileOps { fun exists(path: String): Boolean; fun freeBytes(dir: File): Long; fun deleteOriginal(path: String): Boolean; fun scanIntoMediaStore(path: String); fun removeFromMediaStore(id: Long) }`
  - `class HideManager(...)`, with `suspend fun hide(ids: List<Long>, tier: VaultTier, progress: (Int, Int) -> Unit): HideResult`, `suspend fun unhide(vaultIds: List<Long>, priv: PrivateHandle): UnhideResult`, `suspend fun moveTier(vaultIds: List<Long>, to: VaultTier, priv: PrivateHandle)`, and `fun cleanupPartials()`
  - `HideResult(hidden: Int, missing: List<Long>, failed: List<Pair<Long, String>>)`
- Per-file hide:
  1. check the file exists, else add it to `missing` and delete its row
  2. check `freeBytes ≥ size + 50 MB`, else fail that item
  3. write `<uuid>.part`
  4. fsync
  5. decrypt-verify (compare SHA-256 with the source)
  6. rename `.part` → `.enc`
  7. insert `VaultRow` (including an encrypted 320px thumbnail)
  8. `deleteOriginal`
  9. `removeFromMediaStore`
- `cleanupPartials()` deletes `*.part` files at startup.
- Unhide decrypts to the original path. If that's occupied, it uses `name (1).ext`, `(2)`, and so on. If the parent folder is gone, it uses `Pictures/Restored/`. It then calls `scanIntoMediaStore` and deletes the `.enc` and the row.
- `VaultStore` ensures each tier directory and `.nomedia` exist.

- [ ] **Step 1:** Write the tests:
  - `hide_roundTrip_unhideRestoresIdenticalBytesAtOriginalPath`
  - `hide_interruptedBeforeVerify_keepsOriginal` (the fake crypto throws mid-stream → the original exists, no `.enc`, no row)
  - `startup_removesPartialVaultFiles`
  - `hide_insufficientSpace_failsWithoutTouchingOriginal`
  - `hide_missingFile_reportsAndDropsRow`
  - `unhide_pathOccupied_restoresWithSuffix`
  - `unhide_parentGone_restoresToPicturesRestored`
  - `moveTier_reencryptsAndRemovesOldFile`
  - `standardTier_dirHasNomedia`
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run them. Expected: PASS.
- [ ] **Step 5:** Commit `feat(vault): hide/unhide/move with verify-before-delete`.

### Task 13: Lock session

**Files:** Create `lock/LockState.kt`, `lock/BiometricGate.kt`, `ui/SecureScreen.kt`; Test `lock/LockStateTest.kt`

**Interfaces:**
- Produces:
  - `class LockState(clock: () -> Long)`, with `val unlocked: StateFlow<Set<VaultTier>>`, `fun unlock(tier, handle)`, `fun lockAll()`, `fun privateHandle(tier): PrivateHandle?`, and `val includeLocked: Boolean` (true if any tier is unlocked)
  - `lockAll()` is called from `ProcessLifecycleOwner` `ON_STOP`
  - `BiometricGate.prompt(activity, tier): PrivateHandle?` (BiometricPrompt `BIOMETRIC_STRONG`, falling back to an in-app PIN/passcode dialog)
  - `@Composable fun SecureScreen(content)` sets `FLAG_SECURE` while composed
- Opening the Sensitive, Documents or Review views, or "show locked items", needs the standard tier unlocked at least. Opening high-security vault items needs the high-security tier.

- [ ] **Step 1:** Write the tests: `lockAll_clearsHandles`, `includeLocked_falseUntilUnlock`, and `unlockStandard_doesNotUnlockHighSecurity`.
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement, and wire `LabelDao.itemsIn(..., includeLocked = lockState.includeLocked)` in ViewModels.
- [ ] **Step 4:** Run them. Expected: PASS.
- [ ] **Step 5:** Commit `feat(lock): session lock, biometric gate, secure screens`.

### Task 14: UI

**Files:** Create `ui/nav/AppNav.kt`, `ui/onboarding/*`, `ui/home/*`, `ui/grid/*`, `ui/viewer/{ImageViewer,VideoPlayer,VaultDataSource}.kt`, `ui/people/*`, `ui/review/*`, `ui/dupes/*`, `ui/vault/*`, `ui/settings/*`; Test `ui/viewer/VaultDataSourceTest.kt`, `ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: everything above.
- Screens:
  - **Onboarding** (in this order):
    1. a privacy explainer
    2. media permissions and `ACCESS_MEDIA_LOCATION`
    3. All files access (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`)
    4. the vivo background step: try the `com.vivo.permissionmanager` / `com.iqoo.secure` battery activity intents, falling back to `ACTION_APPLICATION_DETAILS_SETTINGS`, with text: "Settings → Battery → Background power consumption management → Gallery Sorter → Allow"
    5. standard PIN and fingerprint
    6. high-security passcode (with the uninstall warning; "set up later" allowed)
    7. the backup notice if `com.google.android.apps.photos` or a vivo cloud package is installed
    8. the auto-hide offer
    9. start the scan (only-while-charging toggle)
  - **Home:** tiles grouped by `CategoryGroup` with counts. Sensitive and Documents tiles show a lock icon and never a thumbnail while locked. Also People, Places/Trips, Duplicates, Review and Vault entries.
  - **Grid:** a paged `LazyVerticalGrid`. Selection mode: Hide (tier picker defaulting to the category's tier), Add to album, Delete (trash), "Not this category" (excluded override). The top bar has "Hide all (N)".
  - **Viewer:** pinch zoom and swipe. Video in a Media3 `ExoPlayer`; vault items play through `VaultDataSource`.
  - **People:** name, merge, mark "Me". Settings → "My past phones": pick from the camera models found in DCIM.
  - **Review:** one item at a time with the suggested category. Confirm → included override; Reject → excluded override.
  - **Duplicates:** groups side by side, with the suggested keep marked. "Delete N suggested" → `createTrashRequest` system dialog.
  - **Vault:** tabs for Standard and High-security; Unhide, Move tier.
  - **Settings:** default tier per category, the auto-hide toggle, only-while-charging, change PIN/passcode, a longer standard passcode option, and About (licences, GeoNames credit).
- Produces: `class VaultDataSource(priv, row): androidx.media3.datasource.DataSource`, backed by `VaultCrypto.decryptingStream` with seek support.

- [ ] **Step 1:** Write the tests:
  - `VaultDataSourceTest`: reading from offset 1,500,000 of an encrypted 3 MB file equals the plaintext slice
  - `HomeViewModelTest`: while locked, Sensitive tiles expose `count` but `previewIds` is empty
- [ ] **Step 2:** Run them. Expected: FAIL.
- [ ] **Step 3:** Implement the screens and ViewModels.
- [ ] **Step 4:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:verifyNoNetwork`. Expected: all PASS and SUCCESS.
- [ ] **Step 5:** Commit `feat(ui): onboarding, home, grids, viewer, people, review, dupes, vault, settings`.

### Task 15: Release APK and install guide

**Files:** Create `gallery-sorter/README.md`, `gallery-sorter/scripts/make-release.sh`; Modify `app/build.gradle.kts` (release signing from `keystore.properties`, which is git-ignored; R8 on, with keep rules for Tink, ML Kit, LiteRT, SQLCipher)

- [ ] **Step 1:** `make-release.sh` generates a keystore once with `keytool` if it's missing, outside the repo in `~/.gallery-sorter-keys/`, then runs `./gradlew :app:assembleRelease :app:verifyNoNetwork`.
- [ ] **Step 2:** Run it. Expected: `app/build/outputs/apk/release/app-release.apk`. Then `aapt2 dump permissions` on the APK shows no INTERNET.
- [ ] **Step 3:** Write the README:
  - install on the vivo X200 (allow "Install unknown apps" for the browser or file manager, then open the APK)
  - the first-run steps
  - how to verify no internet (Settings → Apps → Gallery Sorter → Permissions)
  - the warnings: a forgotten PIN means data is gone; the high-security vault is deleted on uninstall; delete already-uploaded cloud copies yourself
- [ ] **Step 4:** Commit the README and script (never the keystore). Push to `ccr-7dc44b41-aumksx`.
