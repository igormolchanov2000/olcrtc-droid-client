# android-client-app Specification

## Purpose
TBD - created by archiving change add-android-olcrtc-client. Update Purpose after archive.
## Requirements
### Requirement: Android client collects tunnel credentials and validates them before startup
The Android client SHALL provide input fields for a Telemost room ID and a secret key, and it SHALL reject startup until both values are present and the secret key is a 64-character hexadecimal string accepted by `olcrtc/mobile`.

#### Scenario: Missing or malformed credentials block startup
- **WHEN** the user attempts to start the tunnel with an empty room ID, an empty key, or a key that is not a 64-character hexadecimal string
- **THEN** the app SHALL keep the tunnel stopped and present a validation error explaining what must be corrected

#### Scenario: Valid credentials are accepted for connection setup
- **WHEN** the user enters a non-empty room ID and a valid 64-character hexadecimal secret key
- **THEN** the app SHALL allow the user to start the tunnel workflow

### Requirement: Android client can start and stop an olcrtc session
The Android client SHALL let the user start and stop the olcrtc session from the app UI, and it SHALL delegate the active session lifecycle to a foreground Android service.

#### Scenario: User starts a session successfully
- **WHEN** the user starts the tunnel and the underlying Android service successfully launches olcrtc
- **THEN** the app SHALL transition through a connecting state and eventually expose a connected state sourced from the service

#### Scenario: User stops a running session
- **WHEN** the user stops the tunnel while a session is active
- **THEN** the app SHALL stop the foreground service, stop olcrtc, and return the UI to an idle state

### Requirement: Android client exposes runtime status and logs
The Android client SHALL display the current tunnel state and stream runtime log output produced by the embedded olcrtc runtime so the user can inspect connection progress and failures.

#### Scenario: Connection progress is visible
- **WHEN** the service changes state during startup, connection, or shutdown
- **THEN** the app SHALL show the latest state to the user without requiring an app restart

#### Scenario: Runtime failure is surfaced to the user
- **WHEN** olcrtc startup or connection establishment fails
- **THEN** the app SHALL show an error state and include recent log output that helps explain the failure

