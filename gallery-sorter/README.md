# Gallery Sorter

An offline, private gallery organiser for Android. It sorts your photos and videos into categories
(people, places, documents, sensitive items, duplicates) on the phone itself, and can hide private items in
an encrypted vault. The app has **no internet permission at all**: it cannot send anything anywhere.

Version 1.0.0. Made for the vivo X200 (Android 11 or newer, 64-bit ARM).

## Three things you must know

Read these before you install.

1. **A forgotten PIN means the data is gone.** The vault is encrypted and there is no reset, no recovery
   email, and no back door. If you forget the PIN (and lose the fingerprint), the hidden files cannot be opened.
   "Start a new vault" (below) gives you a fresh, empty vault; it does not open the old one.
2. **The high-security vault is deleted when you uninstall the app.** It lives inside the app's private
   storage. The standard vault survives an uninstall, but the high-security one does not. Unhide anything you
   want to keep before uninstalling, and be careful with "Clear data" for the same reason.
3. **Delete already-uploaded cloud copies yourself.** Hiding a photo stops future uploads, but copies that
   Google Photos or vivo Cloud already uploaded stay there until you delete them in those apps.

Also:

- **Never delete the hidden `.vault-gs` folder** (in the phone's main storage), and never let a phone cleaner
  or "optimizer" remove it: it holds the standard vault's encrypted files and keys.
- **Check that the phone's cleaner or optimizer excludes Gallery Sorter** (on vivo: i Manager, and any
  "clean up" or "phone optimisation" feature), so it never clears the app's data or that folder.

## Install on your vivo X200

1. Copy `GallerySorter-1.0.0.apk` to the phone (USB cable, or any file transfer you already use). If you built
   it yourself, it is in `app/build/outputs/apk/release/`.
2. Open the **Files** app (or whichever browser or file manager you will use to open the APK) and tap the APK.
3. Android will say it cannot install unknown apps. Tap **Settings**, switch on **Allow from this source**
   for that file manager or browser, then go back.
4. Tap **Install**. If vivo's security scan asks for confirmation, choose **Continue installing**.
5. Once installed, you can switch "Allow from this source" off again.

## First run

The first-run wizard walks you through these steps, in this order:

1. **Privacy.** A short explainer: no internet permission, every model runs on the phone, sorting never
   copies or moves your files.
2. **Photos and videos.** Allow photo and video access (and their location tags, used for Places). Optionally
   allow notifications: they use neutral text and never show pictures.
3. **All files access.** Needed to move private files into the vault and back without leaving a copy behind.
4. **Background power.** vivo stops background apps aggressively, which would pause the scan. The wizard opens
   the right screen: allow **Background power consumption** for Gallery Sorter. If you need to do it by hand:
   Settings, Battery, Background power consumption management, Gallery Sorter, Allow.
5. **Standard vault PIN.** A PIN of 6 or more digits, plus fingerprint if you like.
6. **High-security vault (optional).** A passcode of 10 or more characters (letters allowed). It can be set up
   later in Settings.
7. **Backup notice.** Shown only if Google Photos or vivo Cloud is installed: copies already in the cloud stay
   there until you delete them.
8. **Auto-hide.** "Automatically hide Sensitive items" is **off** by default. If you turn it on:
   - it applies to everything the first scan finds, **including all the photos and videos already on your
     phone**, not only new ones;
   - each item detected as Sensitive goes to its category's vault: by default My nudes (and its camera /
     "of me" folders) go to the high-security vault, Other adult content to the standard vault. Documents are
     never hidden automatically (when you hide them: IDs go to high-security, Banking and Other documents to
     standard). You can change each category's vault in Settings;
   - if that vault isn't set up, or hasn't been unlocked in the last 30 days, the item goes to the standard
     vault instead; if the standard vault isn't usable either, the item stays in the gallery and you get one
     neutral notification a day ("Some items couldn't be hidden");
   - anything you unhide is never hidden again automatically (you can still hide it yourself).
9. **Start the scan.** The first scan of about 10,000 photos takes 15 to 40 minutes. It is fastest while the
   phone is charging, and you can keep using the app meanwhile.

## Reinstalling, and forgotten codes

- **Reinstalling the app.** The standard vault stays on the phone. During setup the wizard says the vault is
  from an earlier install: enter your **existing PIN**, then tap **Recover hidden items** to bring its items
  back into the Vault screen. (The high-security vault does not survive an uninstall.)
- **Forgot the PIN? "Start a new vault".** The old standard vault is moved, untouched, to a backup folder
  next to it (`.vault-gs.old-<date>`), and a new empty vault is set up. The old items stay encrypted and can
  only be opened with the old PIN. The same option exists for the high-security vault in Settings ("Forgot
  passcode? Start a new vault"); its backup folder is inside the app and is deleted if you uninstall.

## Check that the app really has no internet

- Settings, Apps, Gallery Sorter, Permissions: there is no "Network" or "Internet" entry. (Android does not
  ask for internet permission, so it is simply not listed.)
- Optional: Settings, Apps, Gallery Sorter, **Data usage** should show 0 B of mobile data and Wi-Fi.
- Advanced: building the app from source fails automatically if the internet permission ever appears
  (the `verifyNoNetwork` check).

## Keep your signing key safe

Every Android app is signed with a private key. Android only installs an update over an existing app if
it is signed with the **same key**. If the key is lost, the next version cannot be installed as an update:
you would have to uninstall first, and **uninstalling deletes the high-security vault**.

- The release script creates the key once, in `~/.gallery-sorter-keys/` (a file named
  `gallery-sorter-release.jks`, plus `keystore-passwords.txt` with its passwords).
- Back up both files to somewhere safe and offline (for example a USB stick or a password manager).
  Never put them in the repository, and never share them.
- Anyone holding the key and passwords can publish an update that Android will accept as yours.

## Build from source

You need a computer with Java 17 and an Android SDK (`scripts/setup-sdk.sh` downloads it on Linux).

```bash
cd gallery-sorter
./scripts/setup-sdk.sh        # one-off: installs the Android SDK pieces
./scripts/make-release.sh     # creates the signing key on first run, then builds
```

The signed APK appears at `app/build/outputs/apk/release/GallerySorter-<version>.apk` (for this version,
`GallerySorter-1.0.0.apk`). The script also runs the no-network check. The key location can be changed with
the `GALLERY_SORTER_KEYS` environment variable, and the key details are read from the git-ignored
`keystore.properties` file.

## Models and data credits

- On-device models and their licences are listed in
  [LICENSES.md](./app/src/main/assets/models/LICENSES.md) (NSFW classifier, MobileFaceNet face embeddings;
  Google ML Kit bundled models for faces, text, labels and barcodes).
- Place names come from [GeoNames](https://www.geonames.org/), licensed under
  [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/).
- Encryption uses Google Tink and BouncyCastle (Argon2id); the encrypted index uses SQLCipher.
