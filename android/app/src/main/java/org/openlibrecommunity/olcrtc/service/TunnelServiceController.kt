package org.openlibrecommunity.olcrtc.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object TunnelServiceController {
    private const val actionConnect = "org.openlibrecommunity.olcrtc.action.CONNECT"
    private const val actionDisconnect = "org.openlibrecommunity.olcrtc.action.DISCONNECT"
    private const val extraRoomId = "room_id"
    private const val extraSecretKey = "secret_key"
    private const val extraSocksPort = "socks_port"
    private const val extraDuo = "duo"

    fun start(context: Context, config: TunnelConfig) {
        val intent = Intent(context, OlcRtcVpnService::class.java)
            .setAction(actionConnect)
            .putExtra(extraRoomId, config.roomId)
            .putExtra(extraSecretKey, config.secretKey)
            .putExtra(extraSocksPort, config.socksPort)
            .putExtra(extraDuo, config.duo)

        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, OlcRtcVpnService::class.java).setAction(actionDisconnect)
        context.startService(intent)
    }

    fun actionOf(intent: Intent?): String? = intent?.action

    fun configFrom(intent: Intent?): TunnelConfig? {
        val roomId = intent?.getStringExtra(extraRoomId).orEmpty()
        val secretKey = intent?.getStringExtra(extraSecretKey).orEmpty()
        if (roomId.isBlank() || secretKey.isBlank()) {
            return null
        }

        return TunnelConfig(
            roomId = roomId,
            secretKey = secretKey,
            socksPort = intent?.getIntExtra(extraSocksPort, 10808) ?: 10808,
            duo = intent?.getBooleanExtra(extraDuo, false) ?: false,
        )
    }

    fun isConnectAction(intent: Intent?): Boolean = actionOf(intent) == actionConnect

    fun isDisconnectAction(intent: Intent?): Boolean = actionOf(intent) == actionDisconnect
}
