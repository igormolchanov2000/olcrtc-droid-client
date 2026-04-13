package org.openlibrecommunity.olcrtc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.openlibrecommunity.olcrtc.service.TunnelConfig
import org.openlibrecommunity.olcrtc.service.TunnelRepository
import org.openlibrecommunity.olcrtc.service.TunnelServiceState
import org.openlibrecommunity.olcrtc.service.TunnelStatus

data class MainUiState(
    val roomId: String = "",
    val secretKey: String = "",
    val roomIdError: String? = null,
    val secretKeyError: String? = null,
    val serviceState: TunnelServiceState = TunnelServiceState(),
    val canStart: Boolean = false,
    val canStop: Boolean = false,
)

class MainViewModel : ViewModel() {
    private val roomId = MutableStateFlow("")
    private val secretKey = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        roomId,
        secretKey,
        TunnelRepository.state,
    ) { room, key, serviceState ->
        val roomError = if (room.isBlank()) "Room ID is required" else null
        val keyError = when {
            key.isBlank() -> "Secret key is required"
            !hexKey.matches(key) -> "Secret key must be a 64-character hex string"
            else -> null
        }

        MainUiState(
            roomId = room,
            secretKey = key,
            roomIdError = roomError,
            secretKeyError = keyError,
            serviceState = serviceState,
            canStart = roomError == null && keyError == null && serviceState.status in setOf(TunnelStatus.IDLE, TunnelStatus.ERROR),
            canStop = serviceState.status == TunnelStatus.CONNECTING || serviceState.status == TunnelStatus.CONNECTED,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun updateRoomId(value: String) {
        roomId.update { value.trim() }
    }

    fun updateSecretKey(value: String) {
        secretKey.update { value.trim() }
    }

    fun validatedConfigOrNull(): TunnelConfig? {
        val state = uiState.value
        if (!state.canStart) {
            return null
        }

        return TunnelConfig(
            roomId = state.roomId,
            secretKey = state.secretKey,
        )
    }

    companion object {
        private val hexKey = Regex("^[0-9a-fA-F]{64}$")
    }
}
