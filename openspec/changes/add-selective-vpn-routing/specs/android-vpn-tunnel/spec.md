## MODIFIED Requirements

### Requirement: Android client provides whole-device VPN routing through the local olcrtc SOCKS endpoint
The Android client SHALL create an Android VPN tunnel that forwards device traffic into a local tun2socks bridge, and that bridge SHALL use the local SOCKS5 listener started by olcrtc as its upstream proxy. The tunnel SHALL support both all-traffic routing and selected-application routing, with the active routing mode applied through Android `VpnService.Builder` application rules before the VPN interface is established.

#### Scenario: All-traffic mode routes device traffic after olcrtc becomes ready
- **WHEN** the Android service confirms that olcrtc is ready, the user-selected routing mode is all-traffic, and the VPN interface is established successfully
- **THEN** the service SHALL start packet forwarding from the VPN interface to the local SOCKS endpoint for device traffic until the session stops

#### Scenario: Selected-app mode routes only chosen applications after olcrtc becomes ready
- **WHEN** the Android service confirms that olcrtc is ready, the user-selected routing mode is selected-apps, and at least one application has been selected
- **THEN** the service SHALL establish the VPN using allowed-application rules for the selected packages, and traffic from unselected applications SHALL bypass the VPN

#### Scenario: VPN routing stops when the session ends
- **WHEN** the user disconnects or the active olcrtc session terminates
- **THEN** the service SHALL stop packet forwarding and tear down the VPN interface cleanly

## ADDED Requirements

### Requirement: Android VPN startup applies persisted routing policy safely
The Android client SHALL apply the persisted routing policy on every VPN startup and SHALL prevent unsafe or invalid application-rule configurations from causing self-routing or undefined tunnel behavior.

#### Scenario: Client application is excluded from selected-app routing
- **WHEN** the VPN service starts in selected-app mode
- **THEN** the client application's own package SHALL NOT be added to the selected application allow-list used for VPN routing

#### Scenario: Full-device routing preserves control-traffic safety
- **WHEN** the VPN service starts in all-traffic mode
- **THEN** olcrtc-created outbound sockets SHALL continue to bypass the VPN through `VpnService.protect(...)` so control traffic does not loop back into the tunnel
