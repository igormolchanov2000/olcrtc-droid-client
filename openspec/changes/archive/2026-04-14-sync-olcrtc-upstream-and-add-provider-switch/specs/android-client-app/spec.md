## MODIFIED Requirements

### Requirement: Android client collects tunnel credentials and validates them before startup
The Android client SHALL provide a provider selector together with the provider-specific session identifier and secret key fields, and it SHALL reject startup until the selected provider has a complete, valid configuration accepted by `olcrtc/mobile`.

#### Scenario: Missing or malformed provider configuration blocks startup
- **WHEN** the user attempts to start the tunnel with no provider selected, an empty provider-specific session identifier, an empty key, or a key that is not a 64-character hexadecimal string accepted by `olcrtc/mobile`
- **THEN** the app SHALL keep the tunnel stopped and present a validation error explaining which provider-specific input must be corrected

#### Scenario: Valid Telemost configuration is accepted for connection setup
- **WHEN** the user selects Telemost and enters a non-empty Telemost room ID and a valid 64-character hexadecimal secret key
- **THEN** the app SHALL allow the user to start the tunnel workflow

#### Scenario: Valid SaluteJazz configuration is accepted for connection setup
- **WHEN** the user selects SaluteJazz and enters the required SaluteJazz session identifier accepted by the rebased runtime together with a valid 64-character hexadecimal secret key
- **THEN** the app SHALL allow the user to start the tunnel workflow

### Requirement: Android client can start and stop an olcrtc session
The Android client SHALL let the user start and stop an `olcrtc` session for the selected provider from the app UI, and it SHALL delegate the active session lifecycle to a foreground Android service.

#### Scenario: User starts a selected-provider session successfully
- **WHEN** the user starts the tunnel with a valid provider configuration and the underlying Android service successfully launches `olcrtc` for that provider
- **THEN** the app SHALL transition through a connecting state and eventually expose a connected state sourced from the service

#### Scenario: User switches providers between sessions
- **WHEN** the tunnel is idle or has been stopped and the user selects a different provider before starting again
- **THEN** the next session SHALL start with the newly selected provider without requiring the app to be rebuilt or reinstalled

#### Scenario: User stops a running session
- **WHEN** the user stops the tunnel while a provider session is active
- **THEN** the app SHALL stop the foreground service, stop `olcrtc`, and return the UI to an idle state
