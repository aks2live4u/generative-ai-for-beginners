#!/usr/bin/env bash
# Builds the signed release APK.
# First run: creates a signing keystore (outside the repo) and keystore.properties (git-ignored).
# KEEP THE KEYSTORE AND PASSWORDS SAFE - future updates must be signed with the same key.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYDIR="${GALLERY_SORTER_KEYS:-$HOME/.gallery-sorter-keys}"
JKS="$KEYDIR/gallery-sorter-release.jks"
PROPS="$ROOT/keystore.properties"

if [ ! -f "$PROPS" ]; then
  if [ ! -f "$JKS" ]; then
    mkdir -p "$KEYDIR"; chmod 700 "$KEYDIR"
    # PKCS12 keystores use one password for store and key.
    STORE_PW="$(openssl rand -base64 24)"; KEY_PW="$STORE_PW"
    keytool -genkeypair -keystore "$JKS" -storetype PKCS12 -alias gallery-sorter \
      -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Gallery Sorter" \
      -storepass "$STORE_PW" -keypass "$KEY_PW"
    printf 'keystore: %s\nalias: gallery-sorter\nstorePassword: %s\nkeyPassword: %s\n' \
      "$JKS" "$STORE_PW" "$KEY_PW" > "$KEYDIR/keystore-passwords.txt"
    chmod 600 "$JKS" "$KEYDIR/keystore-passwords.txt"
    echo "Created keystore in $KEYDIR - BACK IT UP (see README: Keep your signing key safe)."
    printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=gallery-sorter\nkeyPassword=%s\n' \
      "$JKS" "$STORE_PW" "$KEY_PW" > "$PROPS"
    chmod 600 "$PROPS"
  else
    echo "Keystore exists at $JKS but $PROPS is missing." >&2
    echo "Create $PROPS with storeFile, storePassword, keyAlias, keyPassword (see keystore-passwords.txt)." >&2
    exit 1
  fi
fi

cd "$ROOT"
./gradlew "$@" :app:assembleRelease :app:verifyNoNetwork
VERSION="$(sed -n 's/^ *versionName *= *"\(.*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -n 1)"
[ -n "$VERSION" ] || { echo "Cannot read versionName from app/build.gradle.kts" >&2; exit 1; }
OUT="$ROOT/app/build/outputs/apk/release"
APK="$OUT/GallerySorter-$VERSION.apk"
cp "$OUT/app-release.apk" "$APK"
echo "Release APK: $APK"
