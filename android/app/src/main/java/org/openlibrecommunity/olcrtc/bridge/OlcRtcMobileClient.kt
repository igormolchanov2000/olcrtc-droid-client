package org.openlibrecommunity.olcrtc.bridge

import org.openlibrecommunity.olcrtc.mobile.LogWriter
import org.openlibrecommunity.olcrtc.mobile.Runtime
import org.openlibrecommunity.olcrtc.mobile.SocketProtector
import org.openlibrecommunity.olcrtc.service.TunnelConfig

class OlcRtcMobileClient {
    private val runtime = Runtime()

    fun setDebug(enabled: Boolean) {
        runtime.setDebug(enabled)
    }

    fun setLogWriter(writer: (String) -> Unit) {
        runtime.setLogWriter(object : LogWriter {
            override fun writeLog(msg: String) {
                writer(msg)
            }
        })
    }

    fun setSocketProtector(protector: (Long) -> Boolean) {
        runtime.setProtector(object : SocketProtector {
            override fun protect(fd: Long): Boolean = protector(fd)
        })
    }

    @Throws(Exception::class)
    fun start(config: TunnelConfig) {
        runtime.start(
            config.provider.runtimeId,
            config.sessionId,
            config.secretKey,
            config.socksPort.toLong(),
            "",
            "",
        )
    }

    @Throws(Exception::class)
    fun waitReady(timeoutMillis: Int) {
        runtime.waitReady(timeoutMillis.toLong())
    }

    fun stop() {
        runtime.stop()
    }

    fun isRunning(): Boolean = runtime.isRunning()
}
