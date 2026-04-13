#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/adb_helpers.sh

serial="$(build_adb_cmd)"
adb_cmd=(adb -s "$serial")
apk="app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$apk" ]]; then
  ./gradlew assembleDebug
fi

"${adb_cmd[@]}" install -r "$apk"
"${adb_cmd[@]}" shell am start -n org.openlibrecommunity.olcrtc/.MainActivity

echo "Installed and launched on: $serial"
