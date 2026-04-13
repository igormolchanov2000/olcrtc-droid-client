# olcrtc Android

CLI-first Android client for `olcrtc`.

This project is designed to build, install, launch, and debug entirely from the terminal without Android Studio.

## Package

- **Application ID:** `org.openlibrecommunity.olcrtc`
- **App name:** `olcrtc`
- **Min SDK:** 26
- **Compile / target SDK:** 36

## Architecture

- Jetpack Compose UI for room ID + secret key input
- `OlcRtcVpnService` foreground VPN service
- `gomobile` AAR generated from `olcrtc/mobile`
- `hev-socks5-tunnel` JNI bridge for tun2socks

## First-time setup

The project auto-detects the Android SDK from `local.properties`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, or `~/Library/Android/sdk`.

The Android build also expects an installed Android NDK under the SDK root (for example `~/Library/Android/sdk/ndk/<version>`).

## Main commands

### Build debug APK

```bash
cd android
./gradlew assembleDebug
```

This automatically runs:

- `./scripts/build-mobile-bindings.sh`
- `./scripts/build-tun2socks.sh`

before the normal Android build.

### Rebuild only the Go mobile binding

```bash
cd android
./scripts/build-mobile-bindings.sh
```

Default targets:

- `android/arm64`
- `android/amd64`

Override them if needed:

```bash
OLCRTC_GOMOBILE_TARGETS='android/arm64,android/386' ./scripts/build-mobile-bindings.sh
```

### Rebuild only tun2socks JNI libraries

```bash
cd android
./scripts/build-tun2socks.sh
```

Default ABIs:

- `arm64-v8a`
- `x86_64`

Override them if needed:

```bash
OLCRTC_TUN_ABIS='arm64-v8a x86_64 armeabi-v7a' ./scripts/build-tun2socks.sh
```

### Install and launch

If a single adb device is connected:

```bash
cd android
./scripts/install-and-launch.sh
```

If multiple devices are connected, pick one explicitly:

```bash
cd android
ANDROID_SERIAL=emulator-5554 ./scripts/install-and-launch.sh
ANDROID_SERIAL='<wifi-device-serial>' ./scripts/install-and-launch.sh
```

### Logcat

```bash
cd android
./scripts/logs.sh
```

Or pin a specific device:

```bash
cd android
ANDROID_SERIAL=emulator-5554 ./scripts/logs.sh
```

The filter includes:

- `olcrtc`
- `AndroidRuntime`
- `ActivityManager`

## Manual adb equivalents

### Verify devices

```bash
adb devices
```

### Install APK

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch app

```bash
adb -s emulator-5554 shell am start -n org.openlibrecommunity.olcrtc/.MainActivity
```

### Read filtered logs

```bash
adb -s emulator-5554 logcat -s olcrtc:D AndroidRuntime:E ActivityManager:I *:S
```

## Suggested validation flow

1. Build with `./gradlew assembleDebug`
2. Install with `ANDROID_SERIAL=emulator-5554 ./scripts/install-and-launch.sh`
3. Confirm the Compose screen appears with room ID / secret key fields
4. Start logcat with `ANDROID_SERIAL=emulator-5554 ./scripts/logs.sh`
5. Attempt a connection and confirm:
   - VPN permission prompt appears
   - foreground notification appears
   - in-app runtime logs update
6. Repeat on the Wi-Fi debug device with its `ANDROID_SERIAL`

## Native asset locations

Generated outputs:

- `app/libs/olcrtc-mobile.aar`
- `app/src/main/jniLibs/arm64-v8a/libhev-socks5-tunnel.so`
- `app/src/main/jniLibs/x86_64/libhev-socks5-tunnel.so`

Vendored native source:

- `third_party/hev-socks5-tunnel/`
