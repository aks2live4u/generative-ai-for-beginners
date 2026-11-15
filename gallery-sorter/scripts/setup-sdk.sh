#!/usr/bin/env bash
# Idempotent Android SDK setup. SDK lives outside the repo.
set -euo pipefail
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
CMDLINE_ZIP="commandlinetools-linux-11076708_latest.zip"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"

mkdir -p "$SDK"
if [ ! -x "$SDKM" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/$CMDLINE_ZIP" "https://dl.google.com/android/repository/$CMDLINE_ZIP"
  unzip -q "$tmp/$CMDLINE_ZIP" -d "$tmp"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDKM" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDKM" --sdk_root="$SDK" "platforms;android-35" "build-tools;35.0.0" "platform-tools"

echo "sdk.dir=$SDK" > "$ROOT/local.properties"
"$SDKM" --sdk_root="$SDK" --list_installed
