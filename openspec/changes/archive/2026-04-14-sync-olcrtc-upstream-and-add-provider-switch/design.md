## Context

The repository currently embeds a local `olcrtc` fork on branch `changes-for-android`, with Android-specific fixes in `mobile/` and socket protection code that the app depends on. The Android client and the gomobile wrapper still assume Telemost-only startup, while upstream `openlibrecommunity/olcrtc` has moved forward and now contains new SaluteJazz-related work that should become usable from the client.

This change touches Git history, the Go runtime boundary, and the Android UI/service flow, so it benefits from an explicit design before implementation. The user already has Wi-Fi debugging enabled, which makes real-device validation part of the expected workflow.

## Goals / Non-Goals

**Goals:**
- Move the local `olcrtc` fork onto the current upstream history without losing Android-specific fixes.
- Keep the preserved fork-only changes easy to understand and replay on future upstream syncs.
- Add a provider-aware runtime contract so Android can start either Telemost or SaluteJazz.
- Let the Android client switch providers from the UI while reusing the same VPN/tun2socks lifecycle.
- Validate the rebased build on the connected Android device over Wi-Fi debugging.

**Non-Goals:**
- Rewriting the VPN architecture, tun2socks integration, or Android service model.
- Building a full multi-profile manager or advanced provider-specific configuration UI in this change.
- Replacing the existing secret-key model with a different authentication scheme.
- General cleanup of unrelated upstream differences beyond what is needed for the rebase and provider selection.

## Decisions

### 1. Rebase the fork onto upstream and keep local Android work as a small patch stack
- **Decision:** Update `olcrtc` by rebasing/cherry-picking the fork-only Android commits onto the current `upstream/master` (or the upstream branch chosen for the sync), instead of carrying a large merge-only divergence.
- **Why:** The current fork-specific work is concentrated in a small set of Android-related files. Keeping those changes as clean commits on top of upstream makes conflicts easier to resolve now and on the next sync.
- **Alternatives considered:**
  - **Merge upstream into the fork branch:** simpler short-term, but preserves a noisy history and makes it harder to see which changes are local Android patches.
  - **Vendor a snapshot without Git ancestry cleanup:** rejected because it makes future upstream syncs more expensive.

### 2. Introduce a provider field in the shared session config used by CLI/mobile/Android
- **Decision:** Add an explicit provider value (`telemost` or `salutejazz`) to the runtime configuration path, and thread it through the Android `TunnelConfig`, gomobile wrapper, and Go startup entrypoints.
- **Why:** Provider choice is now part of session identity, not just UI state. A shared config model avoids Android-only branching and keeps the runtime behavior testable from Go and CLI entrypoints.
- **Alternatives considered:**
  - **Encode provider only in Android UI state:** rejected because the Go runtime would still be Telemost-specific.
  - **Infer provider from room ID format:** rejected as brittle and hard to validate.

### 3. Keep provider-specific join/room resolution inside Go, not in Kotlin
- **Decision:** Extend `olcrtc/mobile` and the underlying Go startup path so Android passes the provider plus user-entered session identifier, while Go resolves provider-specific URLs, clients, or connectors.
- **Why:** The provider protocols belong to the `olcrtc` runtime. Keeping that logic in Go avoids duplicating provider-specific behavior in the Android app and keeps CLI/mobile behavior aligned.
- **Alternatives considered:**
  - **Construct provider-specific URLs in Android:** rejected because it leaks transport details into the app layer and would diverge from CLI behavior.
  - **Keep separate mobile entrypoints per provider:** rejected because it creates parallel APIs that the Android service must special-case.

### 4. Keep the VPN service provider-agnostic
- **Decision:** Preserve the current Android `VpnService` flow: validate config, register socket protection, start `olcrtc`, wait for readiness, then start packet forwarding to the local SOCKS listener. Only the runtime startup parameters and UI labels change by provider.
- **Why:** Both Telemost and SaluteJazz should still present the same Android contract to the VPN layer: a ready local SOCKS endpoint plus protected control sockets. This limits regression risk in the already-working tunnel path.
- **Alternatives considered:**
  - **Fork the service into per-provider implementations:** rejected because the VPN mechanics do not fundamentally differ at the Android boundary.
  - **Make provider selection a compile-time flavor:** rejected because the user wants runtime switching in one client.

### 5. Make the UI explicitly provider-aware, but keep the first version minimal
- **Decision:** Add a provider selector to the main screen, adjust field labels/help text to the selected provider, and start/stop the chosen backend without adding profile management.
- **Why:** The user asked for switching between Telemost and SaluteJazz in the client, and the smallest useful UX is a per-session selector plus provider-aware validation.
- **Alternatives considered:**
  - **Hard-code a default provider with hidden switching:** rejected because it does not meet the requirement.
  - **Add multi-profile storage now:** rejected as scope creep.

### 6. Treat real-device validation as a required part of the migration
- **Decision:** Rebuild the gomobile artifact after the rebase, install the Android app on the Wi-Fi-debug device, and validate connect/disconnect for both providers with log capture.
- **Why:** This change crosses Git history, native/mobile bindings, and runtime behavior. Emulator-only checks are not enough for VPN and socket-protection regressions.
- **Alternatives considered:**
  - **Emulator-only validation:** rejected because VPN/protection behavior can differ from physical devices.

## Risks / Trade-offs

- **[Rebase conflicts in `mobile/` and `internal/protect/`]** → Mitigation: isolate the current fork-only commits first, replay them one by one on upstream, and verify each preserved patch after conflict resolution.
- **[Upstream SaluteJazz work may be incomplete or not yet wired into the Go runtime used by Android]** → Mitigation: treat provider integration as part of this change, and keep a fallback plan to expose only the subset already supported by the rebased runtime if gaps are discovered.
- **[Provider-specific input requirements may diverge beyond a single room ID field]** → Mitigation: introduce a provider enum now and keep the Android config model extensible so extra fields can be added without redesigning the service boundary.
- **[Rebuilt gomobile bindings may break Kotlin integration or ABI packaging]** → Mitigation: regenerate the AAR as part of the change and validate the app build before device testing.
- **[Changing startup config can regress the existing Telemost flow]** → Mitigation: keep Telemost as the default option and run Telemost regression tests before validating SaluteJazz.

## Migration Plan

1. Fetch the latest upstream `olcrtc` history and identify the local Android-only commits that must survive the sync.
2. Rebase or replay those commits onto the new upstream base, resolving conflicts in `mobile/`, protection hooks, and any provider-related startup code.
3. Extend the Go runtime and gomobile boundary with provider-aware startup, then rebuild the Android-consumable artifact.
4. Update the Android client config model, UI, validation, and service/controller flow to pass the selected provider.
5. Run CLI and Android build checks, then validate Telemost and SaluteJazz on the Wi-Fi-debug device with log capture.
6. Document the new sync/rebuild/run flow and the exact preserved local patches.

Rollback is straightforward: reset the `olcrtc` fork to the pre-sync commit, restore the previous AAR, and revert the Android UI/provider changes if validation fails.

## Open Questions

- What exact user-entered identifier should the Android client request for SaluteJazz after the upstream sync: room ID, meeting ID, join token, or another provider-specific value?
- Should the app persist the last selected provider, or always default to Telemost on launch?
- Do we need a temporary compatibility shim if the upstream SaluteJazz support is not yet exposed through the same runtime path as Telemost?
