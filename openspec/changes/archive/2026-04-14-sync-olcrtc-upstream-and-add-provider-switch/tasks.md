## 1. Upstream sync and patch preservation

- [x] 1.1 Identify the fork-only Android/mobile commits in `olcrtc` and capture the current baseline before rebasing
- [x] 1.2 Fetch the latest `upstream` history and rebase or replay the preserved Android commits onto the new upstream base
- [x] 1.3 Resolve conflicts in the rebased fork and verify the preserved mobile/protection changes still build and behave correctly

## 2. Provider-aware olcrtc runtime

- [x] 2.1 Add a shared provider field to the Go runtime configuration path used by CLI and `olcrtc/mobile`
- [x] 2.2 Implement provider-specific startup/join resolution for Telemost and SaluteJazz in the rebased runtime
- [x] 2.3 Rebuild the Android gomobile artifact and update the repository’s Android-consumable `olcrtc` bindings

## 3. Android client provider switching

- [x] 3.1 Extend Android tunnel models, repository/controller flow, and service startup to carry the selected provider
- [x] 3.2 Update the Compose UI, strings, and validation rules so the user can switch between Telemost and SaluteJazz before connecting
- [x] 3.3 Verify the VPN/tun2socks lifecycle remains provider-agnostic and still starts only after `olcrtc` reports ready

## 4. Validation and documentation

- [ ] 4.1 Run Telemost regression checks after the rebase, including build, connect, disconnect, and log review
- [x] 4.2 Validate SaluteJazz session startup and teardown on the Wi-Fi-debug Android device with captured logs
- [x] 4.3 Document the rebased fork baseline, preserved local patches, rebuild commands, and provider-switch test workflow
