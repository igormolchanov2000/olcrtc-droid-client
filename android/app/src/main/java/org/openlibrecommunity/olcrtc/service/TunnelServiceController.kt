package org.openlibrecommunity.olcrtc.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.openlibrecommunity.olcrtc.routing.RoutingMode

object TunnelServiceController {
    private const val actionConnect = "org.openlibrecommunity.olcrtc.action.CONNECT"
    private const val actionDisconnect = "org.openlibrecommunity.olcrtc.action.DISCONNECT"
    private const val extraProvider = "provider"
    private const val extraSessionId = "session_id"
    private const val extraSecretKey = "secret_key"
    private const val extraSocksPort = "socks_port"
    private const val extraRoutingMode = "routing_mode"
    private const val extraSelectedPackages = "selected_packages"

    fun start(context: Context, config: TunnelConfig) {
        val intent = Intent(context, OlcRtcVpnService::class.java)
            .setAction(actionConnect)
            .putExtra(extraProvider, config.provider.runtimeId)
            .putExtra(extraSessionId, config.sessionId)
            .putExtra(extraSecretKey, config.secretKey)
            .putExtra(extraSocksPort, config.socksPort)
            .putExtra(extraRoutingMode, config.routingMode.storageValue)
            .putStringArrayListExtra(extraSelectedPackages, ArrayList(config.selectedPackages.sorted()))

        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, OlcRtcVpnService::class.java).setAction(actionDisconnect)
        context.startService(intent)
    }

    fun actionOf(intent: Intent?): String? = intent?.action

    fun configFrom(intent: Intent?): TunnelConfig? {
        val provider = TunnelProvider.fromRuntimeId(intent?.getStringExtra(extraProvider)) ?: return null
        val sessionId = intent?.getStringExtra(extraSessionId).orEmpty()
        val secretKey = intent?.getStringExtra(extraSecretKey).orEmpty()
        if (sessionId.isBlank() || secretKey.isBlank()) {
            return null
        }

        return TunnelConfig(
            provider = provider,
            sessionId = sessionId,
            secretKey = secretKey,
            socksPort = intent?.getIntExtra(extraSocksPort, 10808) ?: 10808,
            routingMode = RoutingMode.fromStorageValue(intent?.getStringExtra(extraRoutingMode)),
            selectedPackages = intent?.getStringArrayListExtra(extraSelectedPackages)?.toSet().orEmpty(),
        )
    }

    fun isConnectAction(intent: Intent?): Boolean = actionOf(intent) == actionConnect

    fun isDisconnectAction(intent: Intent?): Boolean = actionOf(intent) == actionDisconnect
}
