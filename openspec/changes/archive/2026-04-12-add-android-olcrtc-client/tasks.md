## 1. Android project bootstrap

- [x] 1.1 Scaffold a new in-repo Android project with the terminal-first template, Gradle wrapper, and helper scripts
- [x] 1.2 Configure application id, app name, SDK levels, signing/debug defaults, and repository-local build settings for CLI use
- [x] 1.3 Add README/runbook commands for assemble, install, launch, and logcat without Android Studio

## 2. Go mobile packaging

- [x] 2.1 Add a reproducible script that builds the Android-consumable `olcrtc/mobile` artifact from the Go sources
- [x] 2.2 Wire the generated mobile artifact into the Android app module and verify Kotlin can call its public API
- [x] 2.3 Add Kotlin wrapper classes/interfaces for log forwarding, socket protection, startup, readiness waiting, and shutdown

## 3. VPN and native tunnel integration

- [x] 3.1 Add the Android foreground `VpnService` skeleton, notification channel, and service manifest entries
- [x] 3.2 Implement VPN permission handling and TUN interface establishment for a whole-device tunnel
- [x] 3.3 Integrate a tun2socks component and define repository-controlled steps to stage or build the required ABI assets
- [x] 3.4 Connect the VPN service to the local olcrtc SOCKS listener so packet forwarding starts only after olcrtc reports ready
- [x] 3.5 Register and verify the socket protector bridge so olcrtc outbound sockets bypass the VPN loop

## 4. Android client UI and state flow

- [x] 4.1 Implement the Compose screen for room ID, secret key, connect/disconnect actions, and input validation
- [x] 4.2 Add app state management that reflects idle, connecting, connected, stopping, and error states from the service
- [x] 4.3 Stream olcrtc/runtime logs into the UI so startup progress and failures are visible to the user

## 5. Shutdown, failure handling, and lifecycle polish

- [x] 5.1 Ensure disconnect tears down olcrtc, tun2socks, foreground notification, and VPN interface cleanly
- [x] 5.2 Handle service revocation, startup failure, and unexpected runtime termination with user-visible error reporting

## 6. Validation

- [x] 6.1 Build and install the debug app from the terminal on the configured emulator
- [x] 6.2 Validate a full connect/disconnect cycle on the Android device over Wi-Fi debugging, including VPN permission flow
- [x] 6.3 Capture the final developer workflow and troubleshooting notes for rebuilding native pieces and collecting logs
