package com.doubao.tv.core.data.utils

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel as CoroutineChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * 线路健康检测
 *
 * 播放时在后台用极小的 Range 请求逐条探测线路，只读响应头/几十字节就断开，
 * 不碰播放器、不切台，探测结果供「自动换线」和线路列表使用。
 *
 * 结果缓存 30 分钟；同一 URL 不会重复并发探测；IPv6 与 webview 线路跳过。
 */
object LineHealth {
    private const val TAG = "LineHealth"
    private const val WORKERS = 3
    private const val CONNECT_TIMEOUT_MS = 3_000
    private const val READ_TIMEOUT_MS = 3_000
    private const val FRESH_MS = 30 * 60 * 1000L
    private const val PAUSE_BETWEEN_MS = 120L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val statusMap = mutableStateMapOf<String, Boolean>()
    private val checkedAt = mutableStateMapOf<String, Long>()
    private val checking = mutableStateMapOf<String, Boolean>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private var job: Job? = null
    private var currentKey: String = ""

    fun status(url: String): Boolean? = statusMap[key(url)]

    fun isChecking(url: String): Boolean = checking[key(url)] == true

    /** 播放器给出的确定结果，优先级高于探测 */
    fun mark(url: String, alive: Boolean) {
        val k = key(url)
        if (k.isEmpty()) return
        statusMap[k] = alive
        checkedAt[k] = System.currentTimeMillis()
    }

    /**
     * 开始整轮后台检测。频道列表变化（换源）时会重启。
     * @param current 当前正在播放的频道，它下面的线路排在最前
     */
    fun start(channels: List<Channel>, current: Channel?, playingUrl: String?) {
        val allLines = LinkedHashMap<String, ChannelLine>()
        current?.lineList?.forEach { allLines.putIfAbsent(key(it.url), it) }
        channels.forEach { channel ->
            channel.lineList.forEach { allLines.putIfAbsent(key(it.url), it) }
        }
        val queue = allLines.values.filter {
            it.probeable() && key(it.url) != key(playingUrl)
        }
        if (queue.isEmpty()) return

        val newKey = "${queue.size}:${queue.first().url}:${queue.last().url}"
        if (newKey == currentKey && job?.isActive == true) return

        currentKey = newKey
        job?.cancel()
        job = scope.launch {
            // 先让当前播放稳定几秒，再开始整轮扫描
            delay(4_000)
            Log.i(TAG, "开始后台检测 ${queue.size} 条线路")
            runQueue(queue)
            Log.i(TAG, "后台检测结束：可用 ${statusMap.count { it.value }} / ${statusMap.size}")
        }
    }

    /** 用户换台时，优先补测这个频道的线路 */
    fun probeChannel(channel: Channel, playingUrl: String?) {
        val queue = channel.lineList.filter { it.probeable() && key(it.url) != key(playingUrl ?: "") }
        if (queue.isEmpty()) return
        scope.launch {
            delay(2_000)
            runQueue(queue)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        currentKey = ""
    }

    private suspend fun runQueue(queue: List<ChannelLine>) = coroutineScope {
        val channel = CoroutineChannel<ChannelLine>(CoroutineChannel.UNLIMITED)
        queue.forEach { channel.trySend(it) }
        channel.close()

        repeat(WORKERS) {
            launch {
                for (line in channel) {
                    if (!isActive) break
                    probe(line)
                    delay(PAUSE_BETWEEN_MS)
                }
            }
        }
    }

    private suspend fun probe(line: ChannelLine) {
        val k = key(line.url)
        if (k.isEmpty()) return

        val now = System.currentTimeMillis()
        if (now - (checkedAt[k] ?: 0L) < FRESH_MS) return
        if (!inFlight.add(k)) return

        checking[k] = true
        try {
            val alive = request(line)
            statusMap[k] = alive
            checkedAt[k] = System.currentTimeMillis()
        } catch (ex: Exception) {
            statusMap[k] = false
            checkedAt[k] = System.currentTimeMillis()
        } finally {
            checking.remove(k)
            inFlight.remove(k)
        }
    }

    private fun request(line: ChannelLine): Boolean {
        val url = line.playableUrl
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Range", "bytes=0-2047")
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", line.httpUserAgent ?: "okhttp/4.12.0")
        }
        return try {
            val code = conn.responseCode
            val alive = code in 200..399 || code == 416
            if (alive) {
                // 只读一点点就断开，避免把直播流整段拖下来
                runCatching { conn.inputStream.use { it.read(ByteArray(64)) } }
            }
            alive
        } finally {
            conn.disconnect()
        }
    }

    private fun ChannelLine.probeable(): Boolean {
        val u = playableUrl
        return u.startsWith("http") && !u.contains("://[") && !u.startsWith("webview://")
    }

    private fun key(url: String?) = url?.substringBefore("$")?.trim().orEmpty()
}
