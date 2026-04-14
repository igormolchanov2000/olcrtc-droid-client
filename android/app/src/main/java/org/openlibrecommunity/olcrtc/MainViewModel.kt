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
import org.openlibrecommunity.olcrtc.service.TunnelProvider
import org.openlibrecommunity.olcrtc.service.TunnelRepository
import org.openlibrecommunity.olcrtc.service.TunnelServiceState
import org.openlibrecommunity.olcrtc.service.TunnelStatus

data class MainUiState(
    val provider: TunnelProvider = TunnelProvider.TELEMOST,
    val sessionId: String = "",
    val secretKey: String = "",
    val providerNotice: String? = null,
    val sessionIdError: String? = null,
    val secretKeyError: String? = null,
    val serviceState: TunnelServiceState = TunnelServiceState(),
    val canStart: Boolean = false,
    val canStop: Boolean = false,
)

class MainViewModel : ViewModel() {
    private val provider = MutableStateFlow(TunnelProvider.TELEMOST)
    private val sessionId = MutableStateFlow("")
    private val secretKey = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        provider,
        sessionId,
        secretKey,
        TunnelRepository.state,
    ) { selectedProvider, session, key, serviceState ->
        val providerNotice = when (selectedProvider) {
            TunnelProvider.TELEMOST -> "Telemost is currently unavailable in the Android client"
            TunnelProvider.SALUTE_JAZZ -> null
        }
        val sessionError = when {
            session.isBlank() && selectedProvider == TunnelProvider.SALUTE_JAZZ -> "SaluteJazz room ID is required"
            else -> null
        }
        val keyError = when {
            key.isBlank() -> "Secret key is required"
            !hexKey.matches(key) -> "Secret key must be a 64-character hex string"
            else -> null
        }

        MainUiState(
            provider = selectedProvider,
            sessionId = session,
            secretKey = key,
            providerNotice = providerNotice,
            sessionIdError = sessionError,
            secretKeyError = keyError,
            serviceState = serviceState,
            canStart = providerNotice == null && sessionError == null && keyError == null && serviceState.status in setOf(TunnelStatus.IDLE, TunnelStatus.ERROR),
            canStop = serviceState.status == TunnelStatus.CONNECTING || serviceState.status == TunnelStatus.CONNECTED,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun updateProvider(value: TunnelProvider) {
        provider.update { value }
    }

    fun updateSessionId(value: String) {
        sessionId.update { value.trim() }
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
            provider = state.provider,
            sessionId = state.sessionId,
            secretKey = state.secretKey,
        )
    }

    companion object {
        private val hexKey = Regex("^[0-9a-fA-F]{64}$")
    }
}
