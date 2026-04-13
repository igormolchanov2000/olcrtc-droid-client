## Context

`olcrtc` already contains the core Go transport plus a mobile-friendly API in `olcrtc/mobile`. That package exposes exactly the primitives an Android app needs: socket protection hooks, start/stop lifecycle, readiness waiting, and local SOCKS5 listener startup. What is missing is a native Android application shell, a VPN service that can route device traffic into the local SOCKS endpoint, and a CLI-first build workflow.

The closest local reference is `../v2rayng`, which demonstrates a proven Android architecture for long-running proxy/VPN sessions: a foreground `VpnService`, explicit socket protection to avoid routing loops, a tun2socks bridge, and terminal-friendly Gradle builds. This change should reuse those patterns while keeping the `olcrtc` protocol/runtime in the existing Go codebase.

## Goals / Non-Goals

**Goals:**
- Create an Android app that accepts a room ID and secret key and can start/stop an olcrtc session.
- Route whole-device traffic through Android `VpnService` into the local SOCKS5 port created by `olcrtc/mobile`.
- Protect all olcrtc outbound sockets from the VPN so Telemost/WebRTC signaling and media do not loop back into the tunnel.
- Bootstrap and document a build/install/debug workflow that works entirely from the terminal without Android Studio.
- Keep the Android integration aligned with existing `olcrtc` code so desktop behavior remains unchanged.

**Non-Goals:**
- Rewriting the olcrtc transport, Telemost logic, or desktop UI.
- Adding profile subscriptions, complex multi-server management, or advanced routing UI in the first Android version.
- Depending on `../v2rayng` as a runtime component; it is a reference implementation, not a shipped dependency.
- Supporting every Android optimization on day one (for example per-app routing, split tunneling, or extensive background recovery policies).

## Decisions

### 1. Create a standalone CLI-first Android project inside this repository
- **Decision:** Add a dedicated Android app project generated with the terminal-first scaffold workflow from the `android-project-template` skill, using Kotlin, Jetpack Compose, Material 3, Gradle wrapper, and adb helper scripts.
- **Why:** The user explicitly does not use Android Studio. A generated, minimal project keeps the setup reproducible and makes emulator/device testing scriptable.
- **Alternatives considered:**
  - **Android Studio-generated project:** rejected because it does not match the required workflow.
  - **Manually writing the whole Gradle/Android layout from scratch:** possible, but slower and easier to misconfigure than the existing scaffold.

### 2. Integrate `olcrtc/mobile` as the Android runtime boundary
- **Decision:** Package `olcrtc/mobile` for Android with `gomobile bind -target=android` and consume it from Kotlin through a thin wrapper layer.
- **Why:** The package already exposes `SetProtector`, `SetLogWriter`, `Start`, `WaitReady`, `Stop`, and `IsRunning`, which closely match Android service needs without spawning external binaries.
- **Alternatives considered:**
  - **Run a cross-compiled Go binary from the app:** rejected due to worse lifecycle control, IPC complexity, and Android process restrictions.
  - **Rewrite the client in Kotlin/JNI:** rejected because it duplicates protocol logic already implemented in Go.

### 3. Use a foreground `VpnService` as the single long-lived tunnel owner
- **Decision:** Model the Android runtime after `v2rayng`: the UI collects configuration and delegates connect/disconnect work to a foreground `VpnService` (`OlcRtcVpnService`) that owns VPN permission state, the TUN interface, the tun2socks bridge, and the olcrtc runtime.
- **Why:** VPN routing and long-running background work belong in a service, not an activity. This matches Android platform expectations and the reference patterns in `../v2rayng`.
- **Alternatives considered:**
  - **Activity-owned connection:** rejected because it is fragile across rotation/backgrounding and cannot safely host VPN lifecycle.
  - **Separate proxy service plus VPN service from the start:** rejected for first version; one service is simpler and enough.

### 4. Start olcrtc first, then enable packet forwarding through tun2socks after readiness
- **Decision:** The service will establish VPN permission/context, register the socket protector, start `olcrtc/mobile`, wait for readiness, and only then start the tun2socks bridge that forwards TUN traffic to the local SOCKS port.
- **Why:** This avoids blackholing device traffic through a VPN whose upstream SOCKS endpoint is not ready yet while still ensuring the app has `VpnService.protect()` available before olcrtc opens outbound sockets.
- **Alternatives considered:**
  - **Start tun2socks immediately:** simpler, but risks temporary traffic loss during connection setup.
  - **Run proxy-only mode first and establish VPN later:** adds state complexity without benefit for the initial client.

### 5. Protect all olcrtc-created sockets through the mobile protector bridge
- **Decision:** Implement a Kotlin protector object that calls `VpnService.protect(fd)` and pass it to `mobile.SetProtector(...)` before startup.
- **Why:** `olcrtc/internal/protect` already routes HTTP/WebRTC dialers through this hook, which is the correct Android mechanism for excluding control traffic from the VPN tunnel.
- **Alternatives considered:**
  - **Rely on disallowing the app package in VPN rules alone:** insufficient because native sockets must still be explicitly protected.
  - **Custom network selection APIs only:** less direct and less reliable than `protect(fd)` for this case.

### 6. Reuse the `v2rayng` tun2socks pattern, but vendor the needed implementation into this repo
- **Decision:** Mirror `v2rayng`'s architecture of a small Android wrapper around a native tun2socks component, but bring the required source/build outputs under this repository so the app is self-contained.
- **Why:** `v2rayng` demonstrates a working Android path from TUN to local SOCKS. The app must not depend on a sibling checkout at runtime.
- **Alternatives considered:**
  - **Depend directly on `../v2rayng` or its binaries:** rejected because it is non-reproducible for other environments.
  - **Implement tun2socks in pure Kotlin/Java:** rejected as too complex and performance-sensitive.

### 7. Keep first-version state management intentionally small
- **Decision:** Provide a single-screen configuration flow, foreground notification, basic status states (`idle`, `connecting`, `connected`, `stopping`, `error`), and log streaming from Go to the UI. Advanced profile management stays out of scope.
- **Why:** The requested behavior is operationally simple: accept room ID + secret key, connect, and expose VPN through the olcrtc SOCKS port.
- **Alternatives considered:**
  - **Multi-profile manager from day one:** rejected as unnecessary scope.
  - **No logs/status:** rejected because Android networking failures are hard to debug without observability.

## Risks / Trade-offs

- **[gomobile/AAR generation may be fragile across SDK/NDK versions]** → Mitigation: add explicit build scripts, pin tool versions where possible, and document the full terminal workflow.
- **[WebRTC/Telemost behavior may differ on Android once VPN is active]** → Mitigation: register the protector before startup, validate both emulator and physical device flows, and log readiness/failure boundaries clearly.
- **[Native tun2socks packaging increases ABI/build complexity]** → Mitigation: start with the minimum ABI set required for the user's target devices plus emulator support, then expand if needed.
- **[Foreground service and VPN permissions can be revoked or killed by the OS]** → Mitigation: centralize lifecycle in `VpnService`, stop cleanly on `onRevoke`, and expose clear reconnection UX.
- **[Security of the secret key on-device]** → Mitigation: avoid broad sharing/export features in the first version and keep storage strategy explicit; if persisted later, use Android-secure storage primitives.

## Migration Plan

1. Scaffold the Android project in-repo using the CLI-first template and generate the Gradle wrapper/scripts.
2. Add a build script that packages `olcrtc/mobile` for Android and makes the output consumable from the app module.
3. Implement the Compose UI, foreground notification, and `VpnService` lifecycle.
4. Integrate socket protection, readiness waiting, tun2socks startup, and clean shutdown.
5. Validate with terminal builds plus install/run/logcat on emulator and the user's Wi-Fi-debug device.
6. Document the exact commands needed to rebuild native pieces, assemble the APK, install, launch, and collect logs.

Rollback is straightforward because the change is additive: remove the Android project, native packaging scripts, and related docs without impacting the existing desktop client.

## Open Questions

- What final application ID and user-visible app name should be used for the Android package?
- Should the first version persist the last-used room ID/key, or require manual re-entry on each launch?
- Which ABIs should be first-class in the repository artifacts (`arm64-v8a` only vs. `arm64-v8a` + `x86_64` for emulator support)?
- Should the tun2socks component be imported from source in this change, or staged initially with reproducible prebuilt binaries plus follow-up source integration?
