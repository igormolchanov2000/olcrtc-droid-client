# android-vpn-tunnel Specification

## Purpose
TBD - created by archiving change add-android-olcrtc-client. Update Purpose after archive.
## Requirements
### Requirement: Android client provides whole-device VPN routing through the local olcrtc SOCKS endpoint
The Android client SHALL create an Android VPN tunnel that forwards device traffic into a local tun2socks bridge, and that bridge SHALL use the local SOCKS5 listener started by olcrtc as its upstream proxy.

#### Scenario: VPN routing starts after olcrtc becomes ready
- **WHEN** the Android service confirms that olcrtc is ready and listening on the configured local SOCKS port
- **THEN** the service SHALL start packet forwarding from the VPN interface to that SOCKS endpoint

#### Scenario: VPN routing stops when the session ends
- **WHEN** the user disconnects or the active olcrtc session terminates
- **THEN** the service SHALL stop packet forwarding and tear down the VPN interface cleanly

### Requirement: olcrtc control traffic is excluded from the VPN loop
The Android client SHALL protect all olcrtc-created outbound sockets with `VpnService.protect(...)` before they connect so Telemost signaling, ICE, and related control traffic are not routed back into the VPN tunnel.

#### Scenario: Protected sockets bypass the VPN
- **WHEN** the embedded olcrtc runtime opens outbound sockets after the protector has been registered
- **THEN** those sockets SHALL be marked with Android VPN protection before connect is attempted

#### Scenario: Tunnel startup fails when socket protection cannot be applied
- **WHEN** the service cannot register or apply the required socket protection hook
- **THEN** it SHALL fail startup instead of running a VPN session that would self-route its own control traffic

### Requirement: VPN service follows Android long-running service rules
The Android client SHALL run the active VPN session inside a foreground `VpnService`, and it SHALL stop the session when VPN permission is revoked or the service is explicitly terminated.

#### Scenario: Foreground notification exists during an active tunnel
- **WHEN** the VPN tunnel is active or connecting
- **THEN** the app SHALL keep a foreground service notification visible for the session duration

#### Scenario: Permission revocation stops the tunnel
- **WHEN** Android revokes VPN permission for the app
- **THEN** the service SHALL stop olcrtc, stop tun2socks, and report a disconnected state

