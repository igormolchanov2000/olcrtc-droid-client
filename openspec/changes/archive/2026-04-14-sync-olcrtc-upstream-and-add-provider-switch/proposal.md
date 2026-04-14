## Why

The local `olcrtc` fork has diverged from `github.com/openlibrecommunity/olcrtc`, while the Android client depends on fork-only mobile and socket-protection fixes that must not be lost. At the same time, the Android app is still hard-wired to Telemost, so it cannot use the newer SaluteJazz direction now present upstream.

## What Changes

- Rebase/sync the repository’s `olcrtc` fork to the current upstream history while preserving the Android-specific fixes already made in the fork.
- Reshape the preserved fork-only fixes into a clean, replayable patch set so future upstream syncs are less risky.
- Extend the `olcrtc` runtime boundary used by Android so a session can be started for either Telemost or SaluteJazz instead of Telemost only.
- Update the Android client UI, validation, and service flow to let the user choose the provider before connecting.
- Refresh the Android build/debug workflow and validation steps so the rebased runtime can be rebuilt, installed, and tested on the Wi-Fi-debug device.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `android-client-app`: change session configuration from Telemost-only input to provider-aware input that can start either Telemost or SaluteJazz.

## Impact

- Affected code: `olcrtc` fork history, `olcrtc/mobile`, provider-specific runtime/config code, Android Compose UI, service/controller/state classes, strings/resources, and build/debug scripts.
- Affected systems: upstream Git synchronization workflow, gomobile binding generation, Android VPN startup, and on-device validation over Wi-Fi debugging.
- External references: `github.com/openlibrecommunity/olcrtc` upstream history and the upstream SaluteJazz work that must be surfaced through the Android client.
