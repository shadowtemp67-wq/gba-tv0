#!/bin/sh
# Download the mGBA libretro core for supported Android ABIs.
set -eu
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

for abi in armeabi-v7a arm64-v8a; do
  echo "Fetching mGBA core for $abi..."
  mkdir -p "$BASE_DIR/src/main/jniLibs/$abi"
  curl -fL --retry 3 \
    "https://buildbot.libretro.com/nightly/android/latest/$abi/mgba_libretro_android.so.zip" \
    -o "$TMP_DIR/mgba-$abi.zip"
  mkdir -p "$TMP_DIR/extract-$abi"
  unzip -oq "$TMP_DIR/mgba-$abi.zip" -d "$TMP_DIR/extract-$abi"
  CORE=$(find "$TMP_DIR/extract-$abi" -type f -name 'mgba_libretro_android.so' -print -quit)
  if [ -z "$CORE" ]; then
    echo "Could not find mgba_libretro_android.so in the $abi archive" >&2
    exit 1
  fi
  cp "$CORE" "$BASE_DIR/src/main/jniLibs/$abi/libmgba_libretro_android.so"
done
echo "mGBA cores installed."
