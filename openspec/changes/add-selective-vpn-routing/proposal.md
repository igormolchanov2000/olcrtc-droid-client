## Why

The Android client currently routes all device traffic through the VPN once a tunnel is started, which makes it unusable for people who only want selected apps to use the tunnel. We also need a clearer first-run experience that helps users choose a sensible routing mode up front instead of expecting them to discover low-level VPN behavior later.

## What Changes

- Add a routing mode setting for the Android VPN with two user-facing modes:
  - route only selected applications through the VPN
  - route all device traffic through the VPN
- Add per-app selection management so users can review, edit, and persist the list of applications that should use the VPN in selected-app mode.
- Add automatic app selection that prepopulates the selected-app list from a maintained recommendation source, with local fallback behavior when the source is unavailable.
- Show a routing setup flow on first launch that automatically prepares a recommended app selection and asks the user to either accept it, edit it, or switch to full-device VPN routing.
- Keep the tunnel service implementation compatible with Android `VpnService` app allow/disallow rules so routing policy is enforced by the VPN builder rather than only by UI state.
- Preserve safe defaults so the client app and olcrtc control traffic do not self-route into the VPN.

## Capabilities

### New Capabilities
- `android-routing-configuration`: Manage routing mode selection, automatic recommended app selection, persisted per-app choices, and first-run routing setup UX for the Android client.

### Modified Capabilities
- `android-vpn-tunnel`: Expand VPN behavior from whole-device-only routing to support both full-device routing and selected-app-only routing through `VpnService` application rules.
- `android-client-app`: Extend the Android client workflow so routing configuration is surfaced in the app UI and requested during first-run onboarding before normal tunnel usage.

## Impact

- Affected code: `android/app/src/main/java/org/openlibrecommunity/olcrtc/MainActivity.kt`, `MainViewModel.kt`, `service/OlcRtcVpnService.kt`, new routing/app-selection state and UI classes, and related resources.
- Affected systems: Android `VpnService.Builder` configuration, persisted app settings, package discovery/package visibility, and first-run onboarding.
- External/reference inputs: technical approach should be validated against the existing implementation in `../../V2rayNG/V2rayNG`, especially its use of `addAllowedApplication(...)`, `addDisallowedApplication(...)`, package enumeration, and automatic app-selection heuristics.
