## Why

olcrtc already has a desktop-oriented client and a gomobile-friendly entrypoint, but it does not provide a usable Android application that can tunnel the whole device through the Telemost-based transport. Adding a native Android client is needed now so the project can run on phones without Android Studio-dependent tooling and with an operator workflow similar to existing Android proxy apps.

## What Changes

- Add a new Android client application that accepts a secret key and Telemost room ID, starts/stops an olcrtc session, and exposes connection state and logs.
- Integrate the existing `olcrtc/mobile` Go API into Android so the app can start the Telemost client, wait for readiness, stop gracefully, and protect outbound sockets from VPN capture.
- Add Android VPN mode that routes device traffic through a local SOCKS5 listener created by olcrtc using `VpnService` plus a tun-to-SOCKS bridge, following proven patterns from `../v2rayng`.
- Bootstrap the Android project and terminal-first build/install/debug workflow without Android Studio, including Gradle wrapper and adb helper scripts.
- Add packaging/build support for the native Go/mobile and Android artifacts required to run on emulator and physical devices.

## Capabilities

### New Capabilities
- `android-client-app`: Android UI and app lifecycle for entering credentials, controlling the tunnel, and observing status.
- `android-vpn-tunnel`: Whole-device VPN routing that forwards traffic into the local olcrtc SOCKS endpoint while excluding olcrtc control sockets from the VPN loop.
- `android-cli-build-workflow`: Command-line Android project scaffold, build, install, and debug workflow that does not rely on Android Studio.

### Modified Capabilities
- None.

## Impact

- Affected code: `olcrtc/mobile`, Android-specific project files to be added under the repository, build scripts, native artifact packaging, and developer documentation.
- External references: architecture and service patterns from `../v2rayng`, Android `VpnService`, tun2socks/native bridge choices, and terminal-first Android scaffolding.
- Dependencies/systems: gomobile or equivalent Go-to-Android packaging, Android SDK/NDK toolchain, Gradle wrapper, adb-based install/debug flow, emulator and Wi-Fi debugging setup.
