package com.doubao.tv.core.data.utils

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
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

    private val _statusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val statusMap: StateFlow<Map<String, Boolean>> = _statusMap.asStateFlow()

    /** 探测到的首字节延迟（毫秒），换线时优先选延迟低的线路。 */
    private val _latencyMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val latencyMap: StateFlow<Map<String, Long>> = _latencyMap.asStateFlow()

    private val _checking = MutableStateFlow<Set<String>>(emptySet())
    val checking: StateFlow<Set<String>> = _checking.asStateFlow()

    private val checkedAt = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private var job: Job? = null
    private var currentKey: String = ""
    private val channelJobs = Collections.synchronizedSet(mutableSetOf<Job>())

    data class Summary(val checked: Int, val alive: Int, val dead: Int, val checking: Int)

    /**
     * 用 APK 内置的 line-stats.json 先填充一轮健康状态，UI 首次进入就能显示已知结果；
     * checkedAt 置为旧时间，播放时仍会按 30 分钟规则重新实测。
     */
    fun init(context: Context) {
        val file = File(Globals.fileDir, "sources/line-stats.json")
        if (!file.exists()) return
        runCatching {
            val lines = JSONObject(file.readText()).optJSONArray("lines") ?: return@runCatching
            var seeded = 0
            val seededMap = mutableMapOf<String, Boolean>()
            for (i in 0 until lines.length()) {
                val row = lines.optJSONObject(i) ?: continue
                val url = row.optString("url")
                if (url.isBlank() || row.isNull("alive")) continue
                val k = key(url)
                if (k.isEmpty()) continue
                seededMap[k] = row.optBoolean("alive")
                if (row.optBoolean("alive") && row.has("latency_ms")) {
                    _latencyMap.update { it + (k to row.optLong("latency_ms")) }
                }
                checkedAt[k] = 1L
                seeded++
            }
            if (seeded > 0) {
                _statusMap.update { it + seededMap }
                Log.i(TAG, "载入内置线路健康快照 $seeded 条")
            }
        }.onFailure {
            Log.w(TAG, "载入线路健康快照失败：${it.message}")
        }
    }

    fun summary(): Summary {
        val statuses = _statusMap.value
        return Summary(
            checked = statuses.size,
            alive = statuses.count { it.value },
            dead = statuses.count { !it.value },
            checking = _checking.value.size,
        )
    }

    fun status(url: String): Boolean? = _statusMap.value[key(url)]

    fun latency(url: String): Long? = _latencyMap.value[key(url)]

    fun isChecking(url: String): Boolean = key(url) in _checking.value

    /** 供 UI 观察单条线路状态时复用；URL 可能带 `$` 后缀。 */
    fun key(url: String?): String = url?.substringBefore("$")?.trim().orEmpty()

    /** 播放器给出的确定结果，优先级高于探测 */
    fun mark(url: String, alive: Boolean) {
        val k = key(url)
        if (k.isEmpty()) return
        _statusMap.update { it + (k to alive) }
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
            val snapshot = _statusMap.value
            Log.i(TAG, "后台检测结束：可用 ${snapshot.count { it.value }} / ${snapshot.size}")
        }
    }

    /** 用户换台时，优先补测这个频道的线路 */
    fun probeChannel(channel: Channel, playingUrl: String?) {
        val queue = channel.lineList.filter { it.probeable() && key(it.url) != key(playingUrl ?: "") }
        if (queue.isEmpty()) return
        val probeJob = scope.launch {
            delay(2_000)
            runQueue(queue)
        }
        channelJobs.add(probeJob)
        probeJob.invokeOnCompletion { channelJobs.remove(probeJob) }
    }

    fun stop() {
        job?.cancel()
        job = null
        synchronized(channelJobs) {
            channelJobs.forEach { it.cancel() }
            channelJobs.clear()
        }
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

        _checking.update { it + k }
        try {
            val result = request(line)
            _statusMap.update { it + (k to result.alive) }
            if (result.alive && result.latencyMs >= 0) {
                _latencyMap.update { it + (k to result.latencyMs) }
            }
            checkedAt[k] = System.currentTimeMillis()
        } catch (ex: Exception) {
            _statusMap.update { it + (k to false) }
            checkedAt[k] = System.currentTimeMillis()
        } finally {
            _checking.update { it - k }
            inFlight.remove(k)
        }
    }

    private data class ProbeResult(val alive: Boolean, val latencyMs: Long)

    private fun request(line: ChannelLine): ProbeResult {
        val url = line.playableUrl
        val startedAt = System.nanoTime()
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
            val latency = (System.nanoTime() - startedAt) / 1_000_000
            ProbeResult(alive, latency)
        } finally {
            conn.disconnect()
        }
    }

    private fun ChannelLine.probeable(): Boolean {
        val u = playableUrl
        return u.startsWith("http") && !u.contains("://[") && !u.startsWith("webview://")
    }

}
