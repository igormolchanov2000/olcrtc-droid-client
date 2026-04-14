#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/adb_helpers.sh

serial="$(build_adb_cmd)"
adb_cmd=(adb -s "$serial")
variant="${OLCRTC_BUILD_TYPE:-debug}"

case "$variant" in
  debug)
    gradle_task="assembleDebug"
    apk="app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    gradle_task="assembleRelease"
    apk="app/build/outputs/apk/release/app-release.apk"
    ;;
  releaseForTesting)
    gradle_task="assembleReleaseForTesting"
    apk="app/build/outputs/apk/releaseForTesting/app-releaseForTesting.apk"
    ;;
  *)
    echo "Unsupported OLCRTC_BUILD_TYPE: $variant" >&2
    echo "Supported values: debug, release, releaseForTesting" >&2
    exit 1
    ;;
esac

if [[ ! -f "$apk" ]]; then
  ./gradlew "$gradle_task"
fi

"${adb_cmd[@]}" install -r "$apk"
"${adb_cmd[@]}" shell am start -n org.openlibrecommunity.olcrtc/.MainActivity

echo "Installed $variant build and launched on: $serial"
