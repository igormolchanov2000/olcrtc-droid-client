## ADDED Requirements

### Requirement: Android client persists routing mode and selected application choices
The Android client SHALL persist the chosen routing mode and any selected application package names so the same routing policy is restored across app restarts and future VPN sessions.

#### Scenario: Confirmed selected-app routing is restored after restart
- **WHEN** the user confirms selected-app routing with one or more chosen applications and later restarts the app
- **THEN** the app SHALL restore selected-app mode together with the previously confirmed package set

#### Scenario: Confirmed all-traffic routing is restored after restart
- **WHEN** the user confirms all-traffic routing and later restarts the app
- **THEN** the app SHALL restore all-traffic mode without requiring the user to reconfigure routing

### Requirement: Android client can generate recommended app selections automatically
The Android client SHALL generate a recommended selected-app list automatically by matching installed packages against a maintained recommendation source, and it SHALL support offline initialization through a bundled fallback list.

#### Scenario: Bundled recommendation list seeds first-run selection offline
- **WHEN** first-run routing setup begins without a reachable remote recommendation source
- **THEN** the app SHALL create the initial recommended selection from the bundled local recommendation list

#### Scenario: Recommended selection includes matching installed packages only
- **WHEN** the recommendation source contains package names that are not installed on the device
- **THEN** the app SHALL include only the matching installed applications in the automatically prepared selection shown to the user

### Requirement: Android client lets the user inspect and edit the installed-app selection
The Android client SHALL provide an installed-app picker that lists available applications for routing review and lets the user manually adjust the selected-app package set before saving.

#### Scenario: Installed-app picker shows current selection state
- **WHEN** the user opens the routing app picker
- **THEN** the app SHALL display installed applications together with their current selected or unselected state for VPN routing

#### Scenario: User changes app selection and saves it
- **WHEN** the user selects or deselects applications in the routing app picker and confirms the changes
- **THEN** the app SHALL persist the updated package set and use it for subsequent selected-app VPN sessions
