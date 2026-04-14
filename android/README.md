# olcrtc Android

CLI-first Android client for `olcrtc`.

This project is designed to build, install, launch, and debug entirely from the terminal without Android Studio.

## Package

- **Application ID:** `org.openlibrecommunity.olcrtc`
- **App name:** `olcrtc`
- **Min SDK:** 26
- **Compile / target SDK:** 36

## Architecture

- Jetpack Compose UI for provider selection, provider-specific session ID input, and secret key input
- `OlcRtcVpnService` foreground VPN service
- `gomobile` AAR generated from `olcrtc/mobile`
- `hev-socks5-tunnel` JNI bridge for tun2socks

## First-time setup

The project auto-detects the Android SDK from `local.properties`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, or `~/Library/Android/sdk`.

The Android build also expects an installed Android NDK under the SDK root (for example `~/Library/Android/sdk/ndk/<version>`).

## Main commands

### Configure release signing

For a production-signed release, create a local config file:

```bash
cd android
cp release-signing.properties.example release-signing.properties
```

Then fill in:

- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

The actual `release-signing.properties` file and `keystore/` contents are ignored by Git.
You can also provide the same values through environment variables:

- `OLCRTC_RELEASE_STORE_FILE`
- `OLCRTC_RELEASE_STORE_PASSWORD`
- `OLCRTC_RELEASE_KEY_ALIAS`
- `OLCRTC_RELEASE_KEY_PASSWORD`

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

### Build production-signed release APK

```bash
cd android
./gradlew assembleRelease
```

If release signing is configured, the signed APK is written to:

- `app/build/outputs/apk/release/app-release.apk`

If release signing is not configured, Gradle will fall back to the unsigned release output.

### Build release-for-testing APK (debug-signed)

```bash
cd android
./gradlew assembleReleaseForTesting
```

This produces a release-like APK signed with the standard Android debug key:

- `app/build/outputs/apk/releaseForTesting/app-releaseForTesting.apk`

Use it when you want release behavior but still want to reinstall quickly over other debug-signed builds.

### Install and launch

If a single adb device is connected:

```bash
cd android
./scripts/install-and-launch.sh
```

Choose a different build variant with `OLCRTC_BUILD_TYPE`:

```bash
cd android
OLCRTC_BUILD_TYPE=releaseForTesting ./scripts/install-and-launch.sh
OLCRTC_BUILD_TYPE=release ./scripts/install-and-launch.sh
```

If multiple devices are connected, pick one explicitly:

```bash
cd android
ANDROID_SERIAL=emulator-5554 ./scripts/install-and-launch.sh
ANDROID_SERIAL='<wifi-device-serial>' ./scripts/install-and-launch.sh
ANDROID_SERIAL='<wifi-device-serial>' OLCRTC_BUILD_TYPE=releaseForTesting ./scripts/install-and-launch.sh
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
adb -s emulator-5554 install -r app/build/outputs/apk/releaseForTesting/app-releaseForTesting.apk
adb -s emulator-5554 install -r app/build/outputs/apk/release/app-release.apk
```

### Signature behavior

- `debug` and `releaseForTesting` use the debug signing key, so they can replace each other with `adb install -r`
- `release` uses your release keystore, so it cannot replace `debug`/`releaseForTesting` without uninstalling first
- `release` also cannot be replaced by `debug`/`releaseForTesting` without uninstalling first

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
3. Confirm the Compose screen appears with a provider selector plus session ID / secret key fields
4. Switch between **Telemost** and **SaluteJazz** and confirm the session field label changes
5. Start logcat with `ANDROID_SERIAL=emulator-5554 ./scripts/logs.sh`
6. Attempt a connection and confirm:
   - VPN permission prompt appears when needed
   - foreground notification appears
   - in-app runtime logs update
7. Repeat on the Wi-Fi debug device with its `ANDROID_SERIAL`
8. See `OLCRTC_UPSTREAM_SYNC.md` for the current upstream baseline, preserved Android patch set, and provider-switch validation notes

## Native asset locations

Generated outputs:

- `app/libs/olcrtc-mobile.aar`
- `app/src/main/jniLibs/arm64-v8a/libhev-socks5-tunnel.so`
- `app/src/main/jniLibs/x86_64/libhev-socks5-tunnel.so`

Vendored native source:

- `third_party/hev-socks5-tunnel/`
