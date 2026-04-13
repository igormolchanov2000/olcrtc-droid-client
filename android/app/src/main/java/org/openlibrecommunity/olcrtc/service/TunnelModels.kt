package org.openlibrecommunity.olcrtc.service

data class TunnelConfig(
    val roomId: String,
    val secretKey: String,
    val socksPort: Int = 10808,
    val duo: Boolean = false,
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
    val roomId: String = "",
    val message: String = "Disconnected",
    val errorMessage: String? = null,
    val logs: List<String> = emptyList(),
)
