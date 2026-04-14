# olcrtc upstream sync and Android provider-switch notes

## Rebased baseline

- `olcrtc` upstream base: `1646637` (`upstream/master` on 2026-04-14)
- Android fork branch: `changes-for-android`
- Preserved pre-sync safety branch: `backup/changes-for-android-pre-sync-2026-04-14`
- Rebased Android patch commit: `8d5678a` (`feat: Make changes for Android`)

## Preserved local Android patches

The upstream sync kept the Android-specific runtime changes that were already required by the mobile client:

- `internal/protect/protect.go`
  - keeps protected dialers and DNS resolution for Android VPN socket protection
- `internal/protect/transport_net.go`
  - provides a protected `pion/transport` net implementation so WebRTC sockets can bypass the VPN loop
- `mobile/mobile.go`
  - keeps the gomobile runtime boundary used by the Android app
  - now accepts a provider name and routes startup through the shared provider config path

## Shared provider runtime path

The rebase introduced upstream provider abstraction, and Android now uses the same provider path as the CLI:

- shared helper: `olcrtc/internal/runtimecfg/runtimecfg.go`
- supported provider IDs:
  - `telemost`
  - `jazz` (`salutejazz` is normalized to `jazz`)

Provider-specific session identifiers are resolved through the shared runtime config helper before `client.RunWithReady(...)` is called.

## Rebuild commands

### Rebuild gomobile bindings

```bash
cd android
./scripts/build-mobile-bindings.sh
```

### Rebuild tun2socks JNI assets

```bash
cd android
./scripts/build-tun2socks.sh
```

### Assemble debug APK

```bash
cd android
./gradlew assembleDebug
```

## Install and launch

### Emulator

```bash
cd android
ANDROID_SERIAL=emulator-5554 ./scripts/install-and-launch.sh
```

### Wi-Fi debugging device

```bash
cd android
ANDROID_SERIAL='adb-34281FDH20058C-FU1hA1._adb-tls-connect._tcp' ./scripts/install-and-launch.sh
```

## Provider-switch workflow

1. Launch the app.
2. Pick **Telemost** or **SaluteJazz** from the provider chips.
3. Enter the provider-specific session identifier:
   - **Telemost**: room ID
   - **SaluteJazz**: `roomId` or `roomId:password`
4. Enter the shared 64-character hexadecimal secret key.
5. Tap **Connect**.
6. Watch the status card and runtime logs.
7. Tap **Disconnect** to stop the tunnel when the session is active.

## Validation notes from this sync

### Emulator

Validated after the rebase:

- `./gradlew assembleDebug` succeeds
- app installs and launches on `emulator-5554`
- UI now shows a provider selector for **Telemost** and **SaluteJazz**
- switching providers changes the session field label and validation text
- invalid secret keys still block startup

### Wi-Fi debugging device

Validated on device `adb-34281FDH20058C-FU1hA1._adb-tls-connect._tcp`:

- app installs and launches successfully
- provider switching works in the live UI
- SaluteJazz startup succeeds end-to-end after wiring the protected `transport.Net` path into the Jazz provider for Android VPN mode
- Android status moves through `CONNECTING` to `CONNECTED`
- disconnect returns the UI to `IDLE`
- captured logcat shows:
  - `Starting olcrtc mobile runtime`
  - `Jazz joining room: 7kyb2x`
  - `Connecting peer 0 to jazz...`
  - `Peer 0 connected`
  - `SOCKS5 proxy listening on 127.0.0.1:10808`
  - repeated provider traffic over the active Jazz session
  - clean shutdown with the publisher data channel closing after disconnect

### Current blocker

Telemost was validated with a live room ID (`13727907290505`) and a generated shared key, using a local `olcrtc -mode srv -provider telemost` server plus the Android client on the Wi‑Fi-debug device.

Observed result:

- server side connects to Telemost and reports peer connection / reconnect activity
- Android client starts the mobile runtime and attempts to connect
- client then fails with:
  - `addPeer failed: connect peer 0: datachannel timeout`

This strongly suggests that the current Telemost client path in the rebased runtime is still waiting for a DataChannel-based readiness condition, while current Telemost behavior has moved away from that assumption. The Android provider-switch implementation itself is transport-agnostic, but Telemost runtime support now needs a follow-up migration to the new upstream channel model.
