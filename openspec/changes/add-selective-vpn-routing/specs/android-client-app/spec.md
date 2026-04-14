## ADDED Requirements

### Requirement: Android client exposes routing configuration in the tunnel workflow
The Android client SHALL display the active routing mode in its UI and SHALL let the user review and change routing settings before starting a tunnel session.

#### Scenario: User reopens routing settings after onboarding
- **WHEN** the user opens routing settings from the main client UI after the initial setup has been completed
- **THEN** the app SHALL show the currently saved routing mode and selected applications so the user can update them for future connections

#### Scenario: Selected-app mode with no selected apps blocks startup
- **WHEN** the user attempts to start the tunnel while selected-app routing is active and no applications are selected
- **THEN** the app SHALL keep the tunnel stopped and present an action that lets the user select applications or switch to all-traffic routing

### Requirement: Android client requests routing confirmation on first launch
The Android client SHALL present a routing setup flow the first time routing settings are missing, and it SHALL ask the user to confirm either the automatically recommended application selection, an edited application selection, or all-traffic routing.

#### Scenario: First launch offers recommended selected-app routing
- **WHEN** the app starts for the first time and routing settings have not yet been confirmed
- **THEN** the app SHALL open the routing setup flow with a recommended selected-app list prepared automatically and a clear option to accept that recommendation

#### Scenario: First launch allows editing before confirmation
- **WHEN** the user chooses to edit the automatically prepared selection during first-run routing setup
- **THEN** the app SHALL show the installed-app editor, persist the user-confirmed selection, and mark first-run routing setup as complete only after the user confirms the final choice

#### Scenario: First launch allows choosing all-traffic routing instead
- **WHEN** the user chooses all-traffic routing during first-run setup
- **THEN** the app SHALL persist all-traffic mode, mark first-run routing setup as complete, and allow subsequent tunnel starts without requiring an app selection
