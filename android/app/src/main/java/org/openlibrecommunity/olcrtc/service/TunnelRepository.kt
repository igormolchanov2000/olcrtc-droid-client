package org.openlibrecommunity.olcrtc.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object TunnelRepository {
    private const val maxLogLines = 200

    private val _state = MutableStateFlow(TunnelServiceState())
    val state: StateFlow<TunnelServiceState> = _state.asStateFlow()

    fun setConnecting(config: TunnelConfig) {
        _state.update {
            it.copy(
                status = TunnelStatus.CONNECTING,
                roomId = config.roomId,
                message = "Connecting to ${config.roomId}",
                errorMessage = null,
                logs = listOf("Preparing Android VPN service…"),
            )
        }
    }

    fun setConnected(config: TunnelConfig) {
        _state.update {
            it.copy(
                status = TunnelStatus.CONNECTED,
                roomId = config.roomId,
                message = "Connected to ${config.roomId}",
                errorMessage = null,
            )
        }
    }

    fun setStopping(message: String = "Stopping tunnel…") {
        _state.update {
            it.copy(
                status = TunnelStatus.STOPPING,
                message = message,
            )
        }
    }

    fun setIdle(message: String = "Disconnected") {
        _state.update {
            it.copy(
                status = TunnelStatus.IDLE,
                message = message,
                errorMessage = null,
            )
        }
    }

    fun setError(config: TunnelConfig? = null, message: String) {
        _state.update {
            it.copy(
                status = TunnelStatus.ERROR,
                roomId = config?.roomId ?: it.roomId,
                message = message,
                errorMessage = message,
            )
        }
        appendLog("ERROR: $message")
    }

    fun appendLog(message: String) {
        val sanitized = message
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .toList()

        if (sanitized.isEmpty()) {
            return
        }

        _state.update { current ->
            val merged = (current.logs + sanitized).takeLast(maxLogLines)
            current.copy(logs = merged)
        }
    }
}
