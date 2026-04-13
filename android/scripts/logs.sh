#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/adb_helpers.sh

serial="$(build_adb_cmd)"
adb_cmd=(adb -s "$serial")

echo "Streaming logs from: $serial"
"${adb_cmd[@]}" logcat -s olcrtc:D AndroidRuntime:E ActivityManager:I *:S
