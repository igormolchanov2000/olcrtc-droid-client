package org.openlibrecommunity.olcrtc.service

import org.openlibrecommunity.olcrtc.routing.RoutingMode

enum class TunnelProvider(
    val runtimeId: String,
    val displayName: String,
) {
    TELEMOST(
        runtimeId = "telemost",
        displayName = "Telemost",
    ),
    SALUTE_JAZZ(
        runtimeId = "jazz",
        displayName = "SaluteJazz",
    ),
    ;

    companion object {
        fun fromRuntimeId(value: String?): TunnelProvider? {
            return when (value?.trim()?.lowercase()) {
                TELEMOST.runtimeId -> TELEMOST
                SALUTE_JAZZ.runtimeId,
                "salutejazz" -> SALUTE_JAZZ
                else -> null
            }
        }
    }
}

data class TunnelConfig(
    val provider: TunnelProvider = TunnelProvider.TELEMOST,
    val sessionId: String,
    val secretKey: String,
    val socksPort: Int = 10808,
    val routingMode: RoutingMode = RoutingMode.ALL_TRAFFIC,
    val selectedPackages: Set<String> = emptySet(),
)

enum class TunnelStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    STOPPING,
    ERROR,
}

data class TunnelServiceState(
    val status: TunnelStatus = TunnelStatus.IDLE,
    val provider: TunnelProvider = TunnelProvider.TELEMOST,
    val sessionId: String = "",
    val message: String = "Disconnected",
    val errorMessage: String? = null,
    val logs: List<String> = emptyList(),
)
