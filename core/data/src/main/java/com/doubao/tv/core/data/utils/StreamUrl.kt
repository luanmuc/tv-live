package com.doubao.tv.core.data.utils

/**
 * 直播源地址的协议白名单。
 *
 * 远端 m3u/txt 不应把 file://、javascript:、content:// 等非媒体协议带进播放器，
 * 也不允许外部源直接构造 webview://；webview 只由 App 内置的混合源生成。
 */
object StreamUrl {
    private val supportedSchemes = setOf(
        "http",
        "https",
        "rtsp",
        "rtmp",
        "rtp",
        "udp",
    )

    fun isSupported(url: String): Boolean {
        val value = url.trim()
        val separator = value.indexOf("://")
        if (separator <= 0 || separator + 3 >= value.length) return false
        return value.substring(0, separator).lowercase() in supportedSchemes
    }
}
