package com.doubao.tv.core.data.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * 内置直播源释放 + 在线更新
 *
 * APK 里带着 assets/sources 下的 m3u，首次启动复制到应用私有目录，装完就能离线看。
 * 之后每 6 小时（或手动点「刷新全部」）会从仓库拉一次最新源覆盖本地文件：
 * 校验通过才替换，网络失败/文件异常就继续用旧文件，不影响离线可用。
 */
object BuiltinSources {
    private const val TAG = "BuiltinSources"
    private const val ASSET_DIR = "sources"
    private const val PREF_NAME = "builtin_sources"
    private const val KEY_ASSET_HASH = "asset_hash"
    private const val KEY_LAST_CHECK = "last_update_check"

    /** 在线检查间隔：6 小时 */
    private const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private data class RemoteFile(val name: String, val minBlocks: Int)

    private val REMOTE_FILES = listOf(
        RemoteFile("pack-cctv-satellite.m3u", 120),
        RemoteFile("intl-hkmo-tw.m3u", 60),
    )

    /**
     * 依次尝试：
     * 1. 自建国内 OSS/CDN（配置了才启用）
     * 2. gh-proxy.org（国内可访问的 GitHub 加速）
     * 3. gh-proxy.com
     * 4. jsDelivr
     * 5. GitHub raw
     */
    private val MIRRORS: List<String> = buildList {
        val domestic = Constants.SOURCES_DOMESTIC_BASE_URL
        if (domestic.isNotBlank()) add(domestic)
        add(Constants.SOURCES_PROXY_BASE_URL)
        add("https://gh-proxy.com/" + Constants.SOURCES_BASE_URL)
        add("https://cdn.jsdelivr.net/gh/luanmuc/tv-live@main/sources/dist/")
        add(Constants.SOURCES_BASE_URL)
    }

    fun install(context: Context) {
        val targetDir = File(Globals.fileDir, ASSET_DIR)
        if (!targetDir.exists()) targetDir.mkdirs()

        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val assetNames = context.assets.list(ASSET_DIR)
            ?.filter { it.endsWith(".m3u", ignoreCase = true) || it == "line-stats.json" }
            ?.sorted()
            ?: return
        if (assetNames.isEmpty()) return

        // 以 APK 内 assets 的内容哈希判断是否需要重新释放，避免手工维护 SEEDED_VERSION。
        val bundledHash = hashAssets(context, assetNames)
        val alreadySeeded = bundledHash != null && pref.getString(KEY_ASSET_HASH, null) == bundledHash

        var allCopied = true
        assetNames.forEach { name ->
            val target = File(targetDir, name)
            if (alreadySeeded && target.exists() && target.length() > 0L) return@forEach
            val copied = runCatching {
                context.assets.open("$ASSET_DIR/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.isSuccess
            if (!copied) allCopied = false
        }

        if (allCopied && bundledHash != null) {
            pref.edit().putString(KEY_ASSET_HASH, bundledHash).apply()
        }
    }

    private fun hashAssets(context: Context, names: List<String>): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        names.forEach { name ->
            digest.update(name.toByteArray(Charsets.UTF_8))
            digest.update('/'.code.toByte())
            context.assets.open("$ASSET_DIR/$name").use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
        }
        digest.digest().joinToString("") { String.format(Locale.US, "%02x", it.toInt() and 0xff) }
    }.onFailure {
        Log.w(TAG, "计算内置源哈希失败：${it.message}")
    }.getOrNull()

    /**
     * 在线更新内置源。
     *
     * @param force 忽略 6 小时节流，手动刷新时传 true
     * @return 是否有文件被更新
     */
    @Synchronized
    fun updateFromNetwork(context: Context, force: Boolean = false): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = pref.getLong(KEY_LAST_CHECK, 0L)
        if (!force && now - lastCheck in 0 until UPDATE_INTERVAL_MS) return false

        val dir = File(Globals.fileDir, ASSET_DIR)
        if (!dir.exists()) dir.mkdirs()

        var changed = false
        var attempted = false
        REMOTE_FILES.forEach { file ->
            attempted = true
            if (downloadOne(dir, file)) changed = true
        }

        // 所有镜像都没有拿到可用文件时不能写 lastCheck，否则一次网络抖动会锁死 6 小时。
        if (changed) {
            pref.edit().putLong(KEY_LAST_CHECK, now).apply()
            Log.i(TAG, "内置直播源已在线更新")
        } else if (!attempted) {
            pref.edit().putLong(KEY_LAST_CHECK, now).apply()
        } else {
            Log.w(TAG, "本轮在线更新没有可用源，保留 6 小时重试窗口")
        }
        return changed
    }

    private fun downloadOne(dir: File, file: RemoteFile): Boolean {
        for (mirror in MIRRORS) {
            val url = mirror + file.name
            try {
                val text = fetch(url) ?: continue
                if (!looksValid(text, file.minBlocks)) {
                    Log.w(TAG, "跳过无效源：$url")
                    continue
                }

                val target = File(dir, file.name)
                val tmp = File(dir, "${file.name}.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(target)) {
                    target.delete()
                    if (!tmp.renameTo(target)) {
                        tmp.delete()
                        continue
                    }
                }
                Log.i(TAG, "已更新 ${file.name}（${text.length / 1024}KB，来源 $mirror）")
                return true
            } catch (ex: Exception) {
                Log.w(TAG, "更新失败 $url: ${ex.message}")
            }
        }
        return false
    }

    private fun fetch(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "okhttp/4.12.0")
            setRequestProperty("Accept", "*/*")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (body.length > 4 * 1024 * 1024) return null
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun looksValid(text: String, minBlocks: Int): Boolean {
        if (!text.startsWith("#EXTM3U")) return false
        var blocks = 0
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF")) {
                blocks++
            } else if (line.isNotEmpty() && !line.startsWith("#") && !StreamUrl.isSupported(line)) {
                return false
            }
        }
        return blocks >= minBlocks
    }
}
