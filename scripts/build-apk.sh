#!/usr/bin/env bash
# Builds Bourget's APK and copies it to dist/ named with the version and build type.
# Usage: scripts/build-apk.sh [debug|release]   (default: debug)
#
# Release builds are unsigned until a signing config is added to app/build.gradle.kts, so they
# cannot be installed as they are; debug builds can.
set -euo pipefail
cd "$(dirname "$0")/.."

type="${1:-debug}"
case "$type" in
  debug) task=assembleDebug ;;
  release) task=assembleRelease ;;
  *) echo "usage: $0 [debug|release]" >&2; exit 1 ;;
esac

./gradlew "$task"

version=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
src=$(ls "app/build/outputs/apk/$type/"*.apk | head -n 1)
suffix=""
[[ "$src" == *-unsigned.apk ]] && suffix="-unsigned"
mkdir -p dist
out="dist/bourget-$version-$type$suffix.apk"
cp "$src" "$out"
echo "APK: $out"
