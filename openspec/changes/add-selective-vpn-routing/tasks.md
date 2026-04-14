## 1. Routing state foundation

- [x] 1.1 Add routing domain models for routing mode, persisted routing settings, onboarding state, and installed-app metadata in the Android app module
- [x] 1.2 Add the persistence layer for routing settings (including onboarding-complete and selected package set) and wire it into Flow-based UI state
- [x] 1.3 Add the bundled recommended package-list asset and a loader that can seed recommendations from the local fallback source
- [x] 1.4 Add package-discovery support and any required manifest/package-visibility declarations needed to enumerate installed apps on supported Android versions

## 2. Routing recommendation and app-picker logic

- [x] 2.1 Implement installed-app loading, filtering, and sorting for the routing editor while excluding the client app from selection
- [x] 2.2 Implement automatic recommendation matching from the recommendation source against installed packages
- [x] 2.3 Ensure confirmed user selections are persisted and are not silently overwritten by later recommendation seeding or refreshes
- [x] 2.4 Add validation logic for selected-app mode so an empty selected package set is treated as non-startable

## 3. Main UI and first-run onboarding

- [x] 3.1 Extend `MainViewModel` and the main screen state to expose current routing mode, routing summary text, and routing validation errors
- [x] 3.2 Add a reusable routing settings UI that lets the user switch between selected-app routing and all-traffic routing and open the installed-app picker
- [x] 3.3 Add the first-run routing setup modal/flow that preloads recommended apps and lets the user accept, edit, or choose all-traffic mode
- [x] 3.4 Block tunnel startup until first-run routing setup is confirmed, and keep Connect blocked in selected-app mode when no apps are selected

## 4. VPN service integration

- [x] 4.1 Extend tunnel startup so the service receives an immutable snapshot of the active routing settings together with tunnel credentials
- [x] 4.2 Update `OlcRtcVpnService` to apply selected-app routing via `VpnService.Builder.addAllowedApplication(...)` and all-traffic routing via the existing full-device VPN configuration
- [x] 4.3 Preserve loop-safety by excluding the client package from selected-app allow-lists and keeping olcrtc socket protection active in both routing modes
- [x] 4.4 Verify tunnel establishment and teardown behavior still works correctly after routing-mode changes and repeated reconnects

## 5. Validation and polish

- [x] 5.1 Add or update strings, labels, and user-facing copy for routing mode selection, onboarding actions, and validation messages
- [x] 5.2 Manually validate the expected flows on Android: first-run accept recommendation, first-run edit selection, first-run all-traffic choice, selected-app reconnect, and all-traffic reconnect
- [x] 5.3 Compare the finished behavior against the referenced v2rayNG implementation to confirm the intended `VpnService` app-routing semantics and auto-selection behavior are matched where planned
