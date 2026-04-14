## Context

The current Android client establishes a `VpnService` interface in `OlcRtcVpnService` and always adds the default route (`0.0.0.0/0`), which effectively sends all device traffic into tun2socks once `olcrtc` is ready. There is no persisted routing policy, no per-app selection UI, and no first-run onboarding for routing decisions.

The requested change spans multiple layers:
- UI and onboarding in `MainActivity` / Compose
- persisted routing state and installed-app discovery
- `VpnService.Builder` app allow/disallow configuration in `OlcRtcVpnService`
- packaging of a recommended app list plus optional remote refresh

Reference analysis of `../../V2rayNG/V2rayNG` shows a practical Android implementation we can adapt:
- `V2RayVpnService.configurePerAppProxy(...)` uses `addAllowedApplication(...)` for selected-app proxy mode and `addDisallowedApplication(...)` for bypass mode.
- `PerAppProxyActivity` stores a selected package set, loads installed apps from `PackageManager`, and offers an automatic selection action.
- Automatic selection is driven by a plain-text package list downloaded from `AppConfig.ANDROID_PACKAGE_NAME_LIST_URL` with asset fallback (`app/src/main/assets/proxy_package_name`) plus a few hard-coded heuristics for Google packages.

Our client needs only two user-facing routing modes:
1. **Selected apps through VPN**
2. **All traffic through VPN**

Unlike v2rayNG, we do not need a separate “bypass selected apps” mode in the initial scope.

## Goals / Non-Goals

**Goals:**
- Add a persisted routing mode that the VPN service applies every time it starts.
- Add a persisted selected-app package set for selected-app mode.
- Provide automatic recommendation-based package selection with bundled offline fallback.
- Show a first-run routing setup prompt that pre-fills recommended apps and lets the user accept, edit, or choose all-traffic mode.
- Keep olcrtc control sockets protected with `VpnService.protect(...)` and avoid self-routing loops.
- Keep the implementation compatible with the current Compose-based single-activity app.

**Non-Goals:**
- Reproducing every v2rayNG routing feature, including bypass-selected-app mode, routing rulesets, or import/export of package lists.
- Building a server-synced recommendation service in this change.
- Introducing packet-level routing decisions inside tun2socks or olcrtc.
- Redesigning the entire main screen beyond the routing flow and routing settings entry points.

## Decisions

### 1. Use `VpnService.Builder` application rules as the routing enforcement point
We will enforce selected-app routing through Android’s VPN APIs instead of trying to filter traffic after packets reach tun2socks.

- **Selected apps mode**: call `addAllowedApplication(...)` for each selected package.
- **All traffic mode**: do not set an allow-list; continue using the full-device routes already configured.
- **Self package handling**: never include `BuildConfig.APPLICATION_ID` in the selected-app set; in all-traffic mode, add the app itself to `addDisallowedApplication(...)` unless testing shows that doing so interferes with required UX flows.
- **Control traffic**: keep the existing `client.setSocketProtector { fd -> protect(fd.toInt()) }` hook so olcrtc-created sockets bypass the VPN regardless of routing mode.

**Why:** this matches the proven v2rayNG pattern, keeps routing semantics inside the Android VPN boundary, and avoids custom packet classification logic.

**Alternatives considered:**
- Filter traffic inside tun2socks: rejected because Android already exposes per-app routing primitives and post-capture filtering is harder to reason about.
- Route only by process/package inside olcrtc: rejected because the VPN interface sees packets, not Android app identity.

### 2. Introduce a routing settings repository backed by Preferences DataStore
Create a new repository layer, e.g. `RoutingSettingsRepository`, to persist and expose routing state as flows.

Proposed stored fields:
- `routing_mode` = `SELECTED_APPS | ALL_TRAFFIC`
- `selected_packages` = string set of package names
- `routing_onboarding_completed` = boolean
- `routing_recommendation_seeded` = boolean (prevents destructive reseeding once the user has confirmed choices)

Use `androidx.datastore:datastore-preferences` for storage.

**Why:** the app already uses coroutine flows and Compose state collection, so DataStore fits the current architecture better than ad-hoc static preferences.

**Alternatives considered:**
- In-memory ViewModel state only: rejected because service startup must work across process death and app restarts.
- `SharedPreferences`: workable, but less consistent with Flow-based UI state and easier to misuse from multiple threads.

### 3. Add a routing domain model separate from tunnel credentials
Add routing-specific models instead of overloading `TunnelConfig` with raw UI state.

Proposed types:
- `enum class RoutingMode { SELECTED_APPS, ALL_TRAFFIC }`
- `data class RoutingSettings(...)`
- `data class InstalledAppInfo(label, packageName, isSystemApp, icon?)`
- `data class RoutingOnboardingState(...)`

`TunnelConfig` can gain an embedded immutable `routingSettings` snapshot passed to `OlcRtcVpnService` when starting the tunnel, or the service can read the repository directly before `builder.establish()`.

Preferred approach: embed a routing snapshot into `TunnelConfig` at start time, while the repository remains the source of truth for UI editing.

**Why:** service startup should not depend on racing asynchronous repository reads after the start intent is dispatched.

**Alternatives considered:**
- Let the service read DataStore synchronously at startup: possible, but more fragile and harder to test.
- Store only package names in the service intent without a mode enum: rejected because the service needs explicit policy semantics.

### 4. Build app discovery around `PackageManager` with an explicit package-visibility decision
Create an app discovery helper similar to v2rayNG’s `AppManagerUtil`, loading installed packages on a background dispatcher and returning a sorted list for UI display.

Implementation notes:
- Prefer package names and labels from `PackageManager`.
- Keep system apps visible but sort them after user apps unless selected.
- Exclude the client app itself from normal selection.
- Add the required manifest declarations for package visibility, most likely `QUERY_ALL_PACKAGES`, if testing on target SDK 36 confirms broad enumeration is otherwise blocked.

**Why:** selected-app routing is only useful if the user can inspect and edit a real installed-app list.

**Alternatives considered:**
- Only show launchable apps: rejected because some relevant apps may not have a launcher activity.
- Hard-code a tiny curated list: rejected because users must be able to audit and change actual routing targets.

### 5. Ship a bundled recommendation list and optionally refresh it from a remote text source
Adopt the v2rayNG-style recommendation format: one package name per line.

Implementation plan:
- Bundle a local asset such as `android/app/src/main/assets/recommended_proxy_packages.txt`.
- Add a small recommendation loader that first reads the local asset and can optionally fetch a remote replacement on demand or in the background.
- Seed first-run selection from the best available list, then apply small deterministic heuristics (for example, optionally include common Google packages except `com.google.android.webview`, following the v2rayNG precedent).
- Once the user confirms onboarding, never overwrite their selection automatically; later auto-refreshes may only update future recommendations, not the saved chosen set.

**Why:** offline-first seeding is required for the very first app launch, while a remote source lets the recommendation list evolve without shipping a new APK for every change.

**Alternatives considered:**
- Remote-only list: rejected because first launch may happen without connectivity.
- Asset-only list: rejected because it becomes stale quickly.

### 6. Implement first-run onboarding as a blocking modal flow on missing routing confirmation
When the app launches and `routing_onboarding_completed == false`, show a routing setup UI before normal tunnel usage.

Flow:
1. Load recommended packages in background.
2. Default the draft mode to `SELECTED_APPS` and preselect recommended packages.
3. Present three user actions:
   - **Accept recommended apps**
   - **Edit app list**
   - **Use all traffic through VPN**
4. Persist the final choice and mark onboarding complete.

The edit path can reuse the same app picker UI used later from settings/main screen.

**Why:** this directly matches the requested UX and ensures new users make an explicit routing decision early.

**Alternatives considered:**
- Passive banner/snackbar on first run: rejected because it is easy to dismiss without understanding routing behavior.
- Force the user into the editor immediately: rejected because accepting a good default should be one tap.

### 7. Keep the main screen simple and add routing state visibility
Extend the main Compose screen with:
- a summary card/row showing current routing mode
- an action to re-open routing settings
- validation for selected-app mode when the selection is empty

Start behavior:
- In selected-app mode with an empty confirmed app set, the tunnel start action must stay blocked until the user selects at least one app or switches to all-traffic mode.
- In all-traffic mode, start behavior remains unchanged.

**Why:** the current single-screen architecture should remain understandable, but routing state must be visible and editable.

## Risks / Trade-offs

- **[Package visibility restrictions on Android 11+]** → Verify whether target devices require `QUERY_ALL_PACKAGES`; if yes, document Play policy justification because VPN/per-app routing is a legitimate use case.
- **[Empty or stale recommendation list]** → Always ship a bundled asset; treat remote refresh as best-effort only.
- **[Self-routing regressions]** → Keep socket protection for olcrtc runtime, exclude the app package from selection, and test both routing modes with connection establishment and reconnection.
- **[Large app lists causing sluggish UI]** → Load packages off the main thread, cache the last loaded list in memory during the process lifetime, and render via lazy Compose lists.
- **[User confusion between selected-app and all-traffic modes]** → Keep copy explicit in onboarding and in the routing summary on the main screen.
- **[Overwriting user intent during auto-selection updates]** → Seed recommendations once and never silently replace a confirmed selection.

## Migration Plan

1. Add new persisted routing settings with defaults representing an unconfirmed onboarding state.
2. On first launch after install—or first launch after upgrade where routing settings are absent—seed a draft recommendation from the local asset and show the routing onboarding modal.
3. Preserve current tunnel behavior for users who explicitly choose all-traffic mode.
4. If rollout reveals issues with selected-app routing, the app can fall back to all-traffic mode by changing the stored routing mode while leaving the saved package set intact for later reuse.

## Open Questions

- What remote location should host the recommendation list for this project, and do we want it versioned in this repository first?
- Do we want to expose system apps in the initial editor by default, or behind a “show system apps” filter?
- Should the main screen block all interaction until routing onboarding is confirmed, or only block the Connect action while still showing credentials/logs?
