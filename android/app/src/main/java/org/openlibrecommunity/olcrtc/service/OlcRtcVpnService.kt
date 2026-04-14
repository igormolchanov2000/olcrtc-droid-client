package org.openlibrecommunity.olcrtc.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openlibrecommunity.olcrtc.BuildConfig
import org.openlibrecommunity.olcrtc.MainActivity
import org.openlibrecommunity.olcrtc.R
import org.openlibrecommunity.olcrtc.bridge.OlcRtcMobileClient
import org.openlibrecommunity.olcrtc.routing.RoutingMode
import org.openlibrecommunity.olcrtc.tunnel.Tun2SocksNative
import java.io.File

class OlcRtcVpnService : VpnService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionJob: Job? = null
    private var monitorJob: Job? = null
    private var tunInterface: ParcelFileDescriptor? = null
    private var mobileClient: OlcRtcMobileClient? = null
    private var activeConfig: TunnelConfig? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            TunnelServiceController.isConnectAction(intent) -> {
                val config = TunnelServiceController.configFrom(intent)
                if (config == null) {
                    TunnelRepository.setError(message = getString(R.string.invalid_config_message))
                    stopSelf()
                } else {
                    startTunnel(config)
                }
            }

            TunnelServiceController.isDisconnectAction(intent) -> {
                serviceScope.launch {
                    stopTunnel(getString(R.string.status_stopping))
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onRevoke() {
        TunnelRepository.setError(activeConfig, getString(R.string.vpn_permission_revoked))
        serviceScope.launch {
            stopTunnel(getString(R.string.vpn_permission_revoked), updateState = false)
        }
    }

    override fun onDestroy() {
        sessionJob?.cancel()
        monitorJob?.cancel()
        runCatching { Tun2SocksNative.stop() }
        runCatching { mobileClient?.stop() }
        runCatching { tunInterface?.close() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTunnel(config: TunnelConfig) {
        if (sessionJob?.isActive == true) {
            TunnelRepository.appendLog("Tunnel already active; ignoring duplicate start request")
            return
        }

        sessionJob = serviceScope.launch {
            try {
                connect(config)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                handleFailure(config, error)
            }
        }
    }

    private suspend fun connect(config: TunnelConfig) {
        activeConfig = config
        TunnelRepository.setConnecting(config)
        TunnelRepository.appendLog("Routing mode: ${describeRouting(config)}")
        startForeground(notificationId, buildNotification(getString(R.string.status_connecting)))

        tunInterface = establishTunnelInterface(config)
            ?: error(getString(R.string.vpn_establish_failed))

        val client = OlcRtcMobileClient().also { mobileClient = it }
        client.setDebug(BuildConfig.DEBUG)
        client.setLogWriter { message ->
            TunnelRepository.appendLog(message)
            Log.i(logTag, message)
        }
        client.setSocketProtector { fd -> protect(fd.toInt()) }

        Log.i(logTag, "Starting olcrtc mobile runtime")
        TunnelRepository.appendLog("Starting olcrtc mobile runtime")
        client.start(config)
        client.waitReady(startTimeoutMillis)
        TunnelRepository.appendLog("olcrtc SOCKS proxy is ready on 127.0.0.1:${config.socksPort}")

        startTun2Socks(config)
        TunnelRepository.setConnected(config)
        updateForegroundNotification(getString(R.string.status_connected))
        monitorJob = launchRuntimeMonitor(config)
    }

    private fun establishTunnelInterface(config: TunnelConfig): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .setMtu(vpnMtu)
            .addAddress(vpnAddress, vpnPrefixLength)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")

        configureApplicationRouting(builder, config)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        return builder.establish()
    }

    private fun configureApplicationRouting(builder: Builder, config: TunnelConfig) {
        val selfPackage = BuildConfig.APPLICATION_ID
        when (config.routingMode) {
            RoutingMode.ALL_TRAFFIC -> {
                try {
                    builder.addDisallowedApplication(selfPackage)
                } catch (error: PackageManager.NameNotFoundException) {
                    Log.w(logTag, "Unable to disallow self package for all-traffic mode", error)
                }
            }

            RoutingMode.SELECTED_APPS -> {
                val allowedPackages = config.selectedPackages
                    .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                    .filterNot { it == selfPackage }
                    .distinct()

                require(allowedPackages.isNotEmpty()) {
                    getString(R.string.routing_selection_required_message)
                }

                allowedPackages.forEach { packageName ->
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (error: PackageManager.NameNotFoundException) {
                        Log.w(logTag, "Skipping unknown selected package: $packageName", error)
                    }
                }
            }
        }
    }

    private fun startTun2Socks(config: TunnelConfig) {
        val vpnFd = tunInterface ?: error(getString(R.string.vpn_establish_failed))
        val configFile = File(filesDir, "tun2socks-olcrtc.yaml")
        configFile.writeText(buildTun2SocksConfig(config))
        Tun2SocksNative.start(configFile.absolutePath, vpnFd.fd)
        TunnelRepository.appendLog("tun2socks forwarding started")
    }

    private fun buildTun2SocksConfig(config: TunnelConfig): String = buildString {
        appendLine("tunnel:")
        appendLine("  mtu: $vpnMtu")
        appendLine("  ipv4: $vpnAddress")
        appendLine("socks5:")
        appendLine("  port: ${config.socksPort}")
        appendLine("  address: 127.0.0.1")
        appendLine("  udp: 'udp'")
        appendLine("misc:")
        appendLine("  log-level: ${if (BuildConfig.DEBUG) "info" else "warn"}")
        appendLine("  tcp-read-write-timeout: 300000")
        appendLine("  udp-read-write-timeout: 60000")
    }

    private fun launchRuntimeMonitor(config: TunnelConfig): Job = serviceScope.launch {
        while (isActive) {
            delay(2_000)
            val running = mobileClient?.isRunning() == true
            if (!running && TunnelRepository.state.value.status == TunnelStatus.CONNECTED) {
                handleFailure(config, IllegalStateException(getString(R.string.runtime_stopped_unexpectedly)))
                break
            }
        }
    }

    private suspend fun stopTunnel(message: String, updateState: Boolean = true) {
        if (updateState && TunnelRepository.state.value.status != TunnelStatus.IDLE) {
            TunnelRepository.setStopping(message)
        }

        monitorJob?.cancelAndJoin()
        monitorJob = null

        runCatching { Tun2SocksNative.stop() }
        runCatching { mobileClient?.stop() }
        mobileClient = null

        runCatching { tunInterface?.close() }
        tunInterface = null
        activeConfig = null

        if (updateState) {
            TunnelRepository.setIdle(getString(R.string.status_idle))
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun handleFailure(config: TunnelConfig, error: Throwable) {
        val message = error.message ?: error.javaClass.simpleName
        Log.e(logTag, "Tunnel failure: $message", error)
        TunnelRepository.setError(config, message)
        updateForegroundNotification(message)
        stopTunnel(getString(R.string.status_idle), updateState = false)
    }

    private fun updateForegroundNotification(content: String) {
        NotificationManagerCompat.from(this).notify(notificationId, buildNotification(content))
    }

    private fun buildNotification(content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            notificationChannelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun describeRouting(config: TunnelConfig): String {
        return when (config.routingMode) {
            RoutingMode.ALL_TRAFFIC -> "all traffic"
            RoutingMode.SELECTED_APPS -> "${config.selectedPackages.size} selected apps"
        }
    }

    companion object {
        private const val notificationId = 42
        private const val notificationChannelId = "olcrtc-vpn"
        private const val logTag = "olcrtc"
        private const val startTimeoutMillis = 60_000
        private const val vpnAddress = "10.77.0.2"
        private const val vpnPrefixLength = 30
        private const val vpnMtu = 1500
    }
}
