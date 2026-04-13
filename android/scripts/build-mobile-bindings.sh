#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools/mobilebind"
OUTPUT_AAR="$ROOT_DIR/app/libs/olcrtc-mobile.aar"
TARGETS="${OLCRTC_GOMOBILE_TARGETS:-android/arm64,android/amd64}"
ANDROID_API="${OLCRTC_ANDROID_API:-26}"

resolve_sdk_dir() {
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "${ANDROID_SDK_ROOT}" ]]; then
    printf '%s\n' "$ANDROID_SDK_ROOT"
    return
  fi
  if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}" ]]; then
    printf '%s\n' "$ANDROID_HOME"
    return
  fi
  if [[ -f "$ROOT_DIR/local.properties" ]]; then
    local sdk_dir
    sdk_dir="$(awk -F= '/^sdk.dir=/{print $2}' "$ROOT_DIR/local.properties")"
    sdk_dir="${sdk_dir//\\:/:}"
    sdk_dir="${sdk_dir//\\/}"
    if [[ -n "$sdk_dir" && -d "$sdk_dir" ]]; then
      printf '%s\n' "$sdk_dir"
      return
    fi
  fi
  if [[ -d "$HOME/Library/Android/sdk" ]]; then
    printf '%s\n' "$HOME/Library/Android/sdk"
    return
  fi

  echo "Unable to locate Android SDK" >&2
  exit 1
}

resolve_ndk_dir() {
  if [[ -n "${ANDROID_NDK_HOME:-}" && -d "${ANDROID_NDK_HOME}" ]]; then
    printf '%s\n' "$ANDROID_NDK_HOME"
    return
  fi
  if [[ -n "${ANDROID_NDK_ROOT:-}" && -d "${ANDROID_NDK_ROOT}" ]]; then
    printf '%s\n' "$ANDROID_NDK_ROOT"
    return
  fi

  local sdk_dir="$1"
  local latest_ndk
  latest_ndk="$(find "$sdk_dir/ndk" -maxdepth 1 -mindepth 1 -type d | sort | tail -1)"
  if [[ -n "$latest_ndk" && -d "$latest_ndk" ]]; then
    printf '%s\n' "$latest_ndk"
    return
  fi

  echo "Unable to locate Android NDK" >&2
  exit 1
}

mkdir -p "$ROOT_DIR/app/libs"
chmod +x "$TOOLS_DIR/gobind"

SDK_DIR="$(resolve_sdk_dir)"
NDK_DIR="$(resolve_ndk_dir "$SDK_DIR")"

pushd "$TOOLS_DIR" >/dev/null
GOOS="$(go env GOHOSTOS)" GOARCH="$(go env GOHOSTARCH)" go mod download
PATH="$TOOLS_DIR:$PATH" \
ANDROID_SDK_ROOT="$SDK_DIR" \
ANDROID_HOME="$SDK_DIR" \
ANDROID_NDK_HOME="$NDK_DIR" \
go run golang.org/x/mobile/cmd/gomobile bind \
  -target="$TARGETS" \
  -androidapi "$ANDROID_API" \
  -javapkg=org.openlibrecommunity.olcrtc \
  -ldflags=-checklinkname=0 \
  -o "$OUTPUT_AAR" \
  github.com/openlibrecommunity/olcrtc/mobile
popd >/dev/null

echo "Built gomobile AAR: $OUTPUT_AAR"
