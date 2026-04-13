#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$ROOT_DIR/third_party/hev-socks5-tunnel"
OUT_DIR="$ROOT_DIR/app/src/main/jniLibs"
ABIS="${OLCRTC_TUN_ABIS:-arm64-v8a x86_64}"
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

resolve_ndk_build() {
  if [[ -n "${ANDROID_NDK_HOME:-}" && -x "${ANDROID_NDK_HOME}/ndk-build" ]]; then
    printf '%s\n' "${ANDROID_NDK_HOME}/ndk-build"
    return
  fi

  local sdk_dir="$1"
  local latest_ndk
  latest_ndk="$(find "$sdk_dir/ndk" -maxdepth 2 -name ndk-build | sort | tail -1)"
  if [[ -n "$latest_ndk" && -x "$latest_ndk" ]]; then
    printf '%s\n' "$latest_ndk"
    return
  fi

  echo "Unable to locate ndk-build" >&2
  exit 1
}

if [[ ! -d "$SRC_DIR" ]]; then
  echo "tun2socks source directory is missing: $SRC_DIR" >&2
  exit 1
fi

SDK_DIR="$(resolve_sdk_dir)"
NDK_BUILD="$(resolve_ndk_build "$SDK_DIR")"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

mkdir -p "$TEMP_DIR/jni" "$OUT_DIR"
echo 'include $(call all-subdir-makefiles)' > "$TEMP_DIR/jni/Android.mk"
ln -s "$SRC_DIR" "$TEMP_DIR/jni/hev-socks5-tunnel"

"$NDK_BUILD" \
  NDK_PROJECT_PATH="$TEMP_DIR" \
  APP_BUILD_SCRIPT="$TEMP_DIR/jni/Android.mk" \
  "APP_ABI=$ABIS" \
  APP_PLATFORM="android-$ANDROID_API" \
  NDK_LIBS_OUT="$OUT_DIR" \
  NDK_OUT="$TEMP_DIR/obj" \
  "APP_CFLAGS=-O3 -DPKGNAME=org/openlibrecommunity/olcrtc/tunnel -DCLSNAME=Tun2SocksNative" \
  "APP_LDFLAGS=-Wl,--build-id=none -Wl,--hash-style=gnu"

echo "Built tun2socks JNI libraries in: $OUT_DIR"
