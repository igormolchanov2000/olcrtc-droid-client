## ADDED Requirements

### Requirement: Android project can be scaffolded and built without Android Studio
The repository SHALL contain an Android project layout and supporting scripts that allow developers to scaffold, build, and maintain the Android client entirely from the command line.

#### Scenario: Project bootstrap is reproducible from the terminal
- **WHEN** a developer initializes or refreshes the Android project using the documented scaffold workflow
- **THEN** the repository SHALL produce a Gradle-based Android project with wrapper support and without requiring Android Studio

#### Scenario: Debug APK can be assembled from the terminal
- **WHEN** a developer runs the documented build command in the Android project
- **THEN** Gradle SHALL produce a debug APK for the Android client

### Requirement: Android developer workflow includes install, launch, and log collection commands
The repository SHALL document and provide terminal-first commands or scripts for installing the app on a connected emulator/device, launching it, and collecting logs for debugging.

#### Scenario: App can be installed and launched from adb-oriented scripts
- **WHEN** a compatible emulator or Android device is connected
- **THEN** the documented workflow SHALL allow a developer to install and launch the debug build without Android Studio

#### Scenario: Runtime logs can be collected during debugging
- **WHEN** the developer needs to diagnose Android app or tunnel behavior
- **THEN** the documented workflow SHALL provide a repeatable command for collecting relevant logcat output

### Requirement: Native Android artifacts required by the client are built or staged through repository-controlled steps
The repository SHALL define how required native/mobile artifacts, including the Android-consumable olcrtc mobile package and the tun2socks integration assets, are generated or staged as part of the command-line workflow.

#### Scenario: Mobile binding generation is documented
- **WHEN** a developer needs to rebuild the Android-facing olcrtc runtime package
- **THEN** the repository SHALL provide a script or documented command that generates the required Android artifact from `olcrtc/mobile`

#### Scenario: Tun2socks assets are reproducible
- **WHEN** a developer prepares the Android app for a supported ABI
- **THEN** the repository SHALL provide repository-controlled steps for obtaining the tun2socks assets needed by that build
