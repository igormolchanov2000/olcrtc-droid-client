package org.openlibrecommunity.olcrtc.tunnel

object Tun2SocksNative {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyStartService(configPath: String, fd: Int)

    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyStopService()

    @JvmStatic
    @Suppress("FunctionName")
    private external fun TProxyGetStats(): LongArray?

    fun start(configPath: String, tunFd: Int) {
        TProxyStartService(configPath, tunFd)
    }

    fun stop() {
        TProxyStopService()
    }

    fun stats(): LongArray? = TProxyGetStats()
}
