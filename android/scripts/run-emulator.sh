#!/usr/bin/env bash
set -euo pipefail

if [[ $# -eq 0 ]]; then
  echo "Available AVDs:"
  emulator -list-avds
  echo
  echo "Usage: $0 <AVD_NAME>"
  exit 1
fi

emulator -avd "$1"
