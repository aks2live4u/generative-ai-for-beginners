# Private Gallery Sorter — Design

Date: 2026-11-15 · Status: draft for owner approval

## 1. Intent

The owner has a vivo X200 (Android 15) holding a large, unsorted mix of photos and videos, including
intimate images, IDs and banking screenshots. They want one app that:

- automatically sorts everything by **person, place, and type**,
- lets them **hide whole categories in one tap** (e.g. "hide all nudes"),
- suggests **deletions for duplicates**, regardless of date,
- is **fast**, makes **no copies**, and **never sends data off the phone**.

Privacy is the top priority, then speed, then features.

## 2. Goals, non-goals, anti-goals

**Goals**
- Fully offline, on-device classification of all photos and videos.
- Categories (section 5), including separating the owner's own nudes from other adult content.
- One-tap bulk hide/unhide of any category, folder or selection into an encrypted vault.
- Duplicate detection (exact and visually identical) with keep/delete suggestions.
- A review queue for low-confidence results.
- Scan each file once; incremental after that.

**Non-goals (v1)**
- iPhone, Play Store distribution, cloud sync, sharing, photo editing, multi-user.

**Anti-goals (failure even if it "works")**
- Any network access, by any library, ever.
- Duplicating the user's files (storage use beyond thumbnails and the index).
- Deleting anything without an explicit user confirmation.
- Sensitive content leaking via the recent-apps preview, screenshots, notifications, other gallery apps, or
  Google Photos / vivo Cloud backup.

## 3. Constraints

- Device: vivo X200, Android 15 (Funtouch/OriginOS), MediaTek Dimensity 9400 with NPU.
- Distribution: sideloaded APK. minSdk 30 (Android 11), targetSdk 35.
- Language/UI: Kotlin, Jetpack Compose.
- Vivo kills background work aggressively; the app must guide the user to allow background power use.

## 4. Approaches considered

| Approach | Trade-off | Decision |
|---|---|---|
| **A. New native Kotlin app** with small on-device ML models | Fastest; every line is purpose-built and auditable for "no network". Viewer and player start basic. | **Chosen** (owner delegated the choice; best fit for privacy + speed + no copies). |
| B. Fork Fossify Gallery and add features | Polished viewer/editor for free, but large codebase to bend and to trust. | Rejected. |
| C. Flutter cross-platform | iPhone later, but slower file and ML work; only Android needed. | Rejected. |

## 5. Categories

An item can belong to several categories (e.g. a person + a place + "Camera").

- **Sensitive:** *My nudes* (sub-folders *Taken on my camera* and *Of me*) · *Other adult content*
- **Documents:** *IDs* (passport, licence, national ID / Aadhaar / PAN) · *Banking & finance*
  (statements, cards, payment/UPI screenshots, receipts) · *Other documents* (bills, tickets, forms, notes)
- **Source:** *Camera* · *Screenshots* · *Screen recordings* · *WhatsApp* · *Telegram* · *Other messaging* ·
  *Downloads / saved from web*
- **People:** one group per recognised face, named by the user; one group marked "Me".
- **Places:** by city, and by trip (contiguous days away from the most common home city).
- **Content:** *Selfies* · *Pets* · *Food* · *Nature* · *Memes* · *QR codes*
- **Cleanup:** *Duplicates* · *Blurry*
- **Review:** items whose classification confidence is uncertain.

## 6. How it works

### 6.1 Permissions and the no-network guarantee
- The manifest declares **no `INTERNET` permission**, and uses `tools:node="remove"` to strip any
  `INTERNET` / `ACCESS_NETWORK_STATE` that a dependency's manifest tries to merge in. A build check fails if
  the merged manifest contains them.
- Only libraries that work fully offline with bundled models. Nothing that downloads models at runtime.
- Permissions: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `ACCESS_MEDIA_LOCATION` (to read GPS from photos),
  `MANAGE_EXTERNAL_STORAGE` ("All files access", to move files without copying), `USE_BIOMETRIC`,
  `FOREGROUND_SERVICE` (+ `dataSync` type), `POST_NOTIFICATIONS`.
- `android:allowBackup="false"` and data-extraction rules exclude everything from Android backup.

### 6.2 Indexing (scan once)
- Enumerate MediaStore images and videos; store id, path, size, dates, MIME, dimensions and `GENERATION_MODIFIED`.
- The first scan runs as a foreground service with a progress notification (neutral text, no thumbnails),
  so vivo doesn't kill it. It's resumable, and it can be set to run "only while charging" (default on).
- After that it's incremental: a MediaStore `ContentObserver` plus a periodic WorkManager job process only
  new or changed items.
- Images are analysed from a ~320px decoded thumbnail. Videos use up to 5 sampled frames (1s, 25%, 50%,
  75%, end) and take the maximum score for each label.
- Index stored in Room on SQLCipher. The DB key is held in Android Keystore. Labels such as "nude" are sensitive
  and never stored in plaintext.

### 6.3 Classifiers (all on-device; run via TFLite/LiteRT with NNAPI/GPU delegate, CPU fallback)
- **Nudity/explicit:** an open-source NSFW image model (MobileNet-class, permissive licence), output
  explicit / suggestive / safe. Thresholds: explicit ≥ 0.85 → Sensitive; 0.5–0.85 → Review.
- **Mine vs other (owner chose both rules):** *My nudes* has two sub-folders that may overlap:
  *Taken on my camera*, an explicit camera original (in `DCIM/Camera` with EXIF Make/Model matching this phone, or a
  camera model the user confirms as their own past phone in settings); and *Of me*, any explicit item where
  a face matches the "Me" person. An item in either goes to *My nudes*. Otherwise → *Other adult content*. If
  the result is ambiguous → Review.
- **Faces:** ML Kit face detection (bundled), then a MobileFaceNet-class embedding model. Embeddings are
  clustered incrementally (cosine similarity threshold), and the user names clusters, merges them, and marks "Me".
- **Text/documents:** ML Kit Text Recognition v2 (bundled, Latin script), run only on items that the
  image labeller or aspect ratio suggests are documents or screenshots. Rules on the OCR text:
  card numbers (Luhn check), IFSC, UPI IDs, account/statement keywords, and bank names → Banking. Passport
  MRZ, licence keywords, Aadhaar (12-digit pattern), PAN regex → IDs. Otherwise dense text → Other documents.
- **Scenes/content:** ML Kit Image Labeling (bundled) maps labels to Pets/Food/Nature/etc.
  ML Kit barcode scanning (bundled) handles QR codes. Selfie = front camera EXIF, or a single large face.
- **Blurry:** variance of Laplacian on the thumbnail, below a threshold.
- **Source:** from the path (`DCIM/Camera`, `Pictures/Screenshots`, `Movies/ScreenRecorder` or vivo
  equivalents, `Android/media/com.whatsapp/...`, `Telegram`, `Download`) plus EXIF presence.
- **Places:** GPS from EXIF (via `setRequireOriginal`), then a nearest-city lookup against a bundled
  offline city list (GeoNames cities ≥ 15k population). No online geocoder.

### 6.4 Folders, without copying or moving
- Category folders are **virtual**: database groupings shown in the app. Nothing is copied or moved
  to sort, which is instant and allows one item to be in several folders.
- Users can create their own albums the same way.

### 6.5 Hide (vault) — move, not copy
- Unlock: fingerprint (BiometricPrompt, Class 3), with a PIN fallback (6+ digits). Option 1 from the
  discussion: the app has a normal name and icon; no disguise and no decoy.
- Locked filtering (builder's call): while locked, items labelled Sensitive, Documents, or in the Review
  band are **left out of every open view**: People, Places, Source, Content, Cleanup, search and the viewer.
  Vaulted items appear only in the Vault.
- What requires unlock: the Vault, the *Sensitive* and *Documents* category views, and the Review queue.
  Everything else opens freely. Auto-locks when the app goes to the background. `FLAG_SECURE` on those
  screens blocks screenshots and recent-apps previews.
- Encryption: Google Tink streaming AEAD (AES-256-GCM-HKDF, 1MB segments), so videos can be decrypted
  while streaming in the player (custom Media3 DataSource) and images can be decoded on the fly.
- Keys (builder's call, so the app can work while locked): the vault uses a **public/private key pair**
  (Tink hybrid encryption wrapping per-file streaming keys). The **public key** is always available, so
  background scanning and auto-hide can encrypt new items into the vault without unlocking. The **private
  key** is wrapped twice: by a key derived from the PIN (Argon2id) and by a Keystore key unlocked by
  biometrics. Only unlocking can decrypt.
- Labels and counts: the index DB (SQLCipher) is keyed by a Keystore key that needs **no** user unlock, so
  labels are encrypted at rest against file browsing, and the background scanner can still write them. Home can show
  sensitive counts (behind a lock icon) while locked.
- **Two vault tiers (owner chose both):**
  - **Standard vault:** stored in `/storage/emulated/0/.vault-gs/` with `.nomedia`. Other galleries, Google
    Photos and vivo Cloud ignore it. The wrapped key lives alongside the files, so it **survives an uninstall or
    reinstall** with the PIN. Unlocked with fingerprint or a 6+ digit PIN. Protects against casual users of the phone.
  - **High-security vault:** stored in the app's private internal storage, unreachable by other apps,
    file managers or USB. It needs its own passcode (at least 10 characters, letters allowed), plus an optional fingerprint.
    **Deleted permanently if the app is uninstalled**, which is stated at setup and before any uninstall-risky action.
  - The user chooses the tier when hiding, and sets a default tier per category. Proposed defaults: *My nudes* and
    *IDs* → high-security; *Other adult content*, *Banking*, *Other documents* → standard. Items can be
    moved between tiers (re-encrypted, one file at a time, no lasting copy).
- Hide = stream-encrypt into the vault, verify, then delete the original (directly, via all-files access), and
  remove its MediaStore entry. Peak extra space = one file at a time. The vault index records the original path.
- Unhide = decrypt back to the original path (or `Pictures/Restored` if that path is gone), then rescan
  MediaStore. For Sensitive items, a confirm dialog warns that the file will be visible to other gallery apps
  and cloud backup again (builder's call).
- **Bulk hide:** any category, folder or multi-selection → "Hide all (N items)" → confirm → progress.
  Also a setting: "Automatically hide new items detected as Sensitive" (default off; offered at first run).
- Vault thumbnails are encrypted too.
- Forgetting the PIN and losing biometrics means the vault is unrecoverable. This is stated clearly at setup.

### 6.6 Duplicates and delete suggestions
- Exact: group by file size, then SHA-256 of the content (computed only for size collisions).
- Visually identical: a 64-bit perceptual hash (pHash) on images; Hamming distance ≤ 4 counts as the same picture
  (catches re-compression and resizing). Video: duration ±0.5s plus the pHash of 3 sampled frames.
- Dates are ignored.
- Suggestion: keep the highest resolution, then the camera original, then the earliest EXIF date. Mark the others
  "suggested delete". The user reviews groups side by side and confirms.
- Delete uses `MediaStore.createTrashRequest` (system trash, about 30 days, undoable). Never auto-deletes.

### 6.7 Backup awareness
- On first run, detect whether Google Photos or vivo Cloud is installed, and show a one-time notice:
  previously uploaded sensitive photos must be removed from the cloud by the user. Vaulted items stop
  being uploaded because they leave MediaStore and live in a `.nomedia` hidden folder.

### 6.8 UI (Compose)
- Home: category tiles with counts (sensitive tiles show a lock, never thumbnails, until unlocked).
- Grid → full-screen viewer (pinch zoom, swipe) and video player (Media3).
- Selection mode: hide, add to album, delete (to trash), move out of category ("this is wrong" →
  feeds a per-item override so it's never re-classified).
- People screen: name, merge, mark "Me". Review screen: swipe to confirm or reject a suggested category.
- First-run wizard: permissions → the vivo "Allow background power consumption" deep-link and instructions →
  PIN + fingerprint → backup notice → start scan.

## 7. Performance targets
- First scan of 10,000 photos: ~15–40 min on the X200 while charging; the UI stays usable throughout.
- Incremental processing of a new photo: < 1 s. Opening any category: < 300 ms.
- Index + thumbnails < 1% of the gallery size.

## 8. Testing
- Unit tests: classifier rule logic (mine vs other, document rules), hashing and duplicate grouping, key
  wrapping, vault round-trip (encrypt → decrypt is byte-identical), and the merged-manifest no-INTERNET check.
- Instrumented tests on an emulator: hide/unhide restores the original path, and trash delete works.
- Manual on-device check on the X200 with a test set of mixed media.

## 9. Left to the builder
- The exact NSFW / face-embedding model files (permissive licence, TFLite, < 20 MB each), and threshold tuning.
- Module/package structure, DI, and visual styling.
- Exact vivo folder paths for screenshots and recordings (verify on device).
