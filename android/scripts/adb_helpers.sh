#!/usr/bin/env bash
set -euo pipefail

pick_device_serial() {
  python3 - <<'PY'
import subprocess
lines = subprocess.check_output(['adb', 'devices']).decode().splitlines()
serials = []
for line in lines[1:]:
    if not line.strip():
        continue
    parts = line.split('	')
    if len(parts) >= 2 and parts[1] == 'device':
        serials.append(parts[0])

if not serials:
    raise SystemExit('No adb device in state=device found')

preferred = [s for s in serials if ' (2).' not in s]
print((preferred or serials)[0])
PY
}

build_adb_cmd() {
  local serial="${ANDROID_SERIAL:-}"
  if [[ -z "$serial" ]]; then
    serial="$(pick_device_serial)"
  fi
  printf '%s
' "$serial"
}
