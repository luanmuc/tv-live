package com.doubao.tv.utlis

import android.content.Context
import android.content.Intent
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.body.JSONObjectBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList
import com.doubao.tv.core.data.repositories.epg.EpgRepository
import com.doubao.tv.core.data.repositories.iptv.IptvRepository
import com.doubao.tv.core.data.utils.ChannelAlias
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.Loggable
import com.doubao.tv.core.data.utils.Logger
import com.doubao.tv.BuildConfig
import com.doubao.tv.HttpServerService
import com.doubao.tv.R
import com.doubao.tv.sync.CloudSync
import com.doubao.tv.sync.CloudSyncData
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.material.SnackbarType
import com.doubao.tv.ui.utils.Configs
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.UUID


object HttpServer : Loggable("HttpServer") {
    private const val SERVER_PORT = 10481
    private const val AUTH_PREF_NAME = "http_server_auth"
    private const val AUTH_TOKEN_KEY = "token"

    @Volatile
    private var server: AsyncHttpServer? = null

    @Volatile
    private var activeToken: String? = null

    fun serverUrl(context: Context): String {
        return "http://${getLocalIpAddress()}:${SERVER_PORT}/?token=${authToken(context)}"
    }

    fun start(context: Context) {
        if (!Configs.debugDeveloperMode) return
        activeToken = authToken(context)
        synchronized(this) {
            if (server != null) return
            server = AsyncHttpServer()
        }

        val current = server ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = current

                server.addAction("OPTIONS", ".+") { _, response ->
                    wrapResponse(response)
                    response.send("ok")
                }

                server.get("/") { request, response ->
                    withAuth(request, response, setCookie = true) {
                        handleRawResource(response, context, "text/html", R.raw.web_push)
                    }
                }
                server.get("/web_push_css.css") { request, response ->
                    withAuth(request, response) {
                        handleRawResource(response, context, "text/css", R.raw.web_push_css)
                    }
                }
                server.get("/web_push_js.js") { request, response ->
                    withAuth(request, response) {
                        handleRawResource(response, context, "text/javascript", R.raw.web_push_js)
                    }
                }

                server.get("/advance") { request, response ->
                    withAuth(request, response, setCookie = true) {
                        handleAssets(response, context, "text/html", "remote-configs/index.html")
                    }
                }

                server.get("/remote-configs/(.*)") { request, response ->
                    withAuth(request, response, setCookie = true) {
                        val contentType = when (request.path.split(".").last()) {
                            "css" -> "text/css"
                            "js" -> "text/javascript"
                            "html" -> "text/html"
                            "json" -> "application/json"
                            "svg" -> "image/svg+xml"
                            "png" -> "image/png"
                            else -> "text/plain"
                        }

                        handleAssets(response, context, contentType, request.path.removePrefix("/"))
                    }
                }

                server.get("/api/info") { request, response ->
                    withAuth(request, response) { handleGetInfo(response) }
                }

                server.post("/api/iptv-source/push") { request, response ->
                    withAuth(request, response) { handleIptvSourcePush(request, response) }
                }

                server.post("/api/epg-source/push") { request, response ->
                    withAuth(request, response) { handleEpgSourcePush(request, response) }
                }

                server.get("/api/channel-alias") { request, response ->
                    withAuth(request, response) { handleGetChannelAlias(response) }
                }

                server.post("/api/channel-alias") { request, response ->
                    withAuth(request, response) { handleUpdateChannelAlias(request, response) }
                }

                server.get("/api/configs") { request, response ->
                    withAuth(request, response) { handleConfigsGet(response) }
                }

                server.post("/api/configs") { request, response ->
                    withAuth(request, response) { handleConfigsPush(request, response) }
                }

                server.get("/api/cloud-sync/data") { request, response ->
                    withAuth(request, response) { handleCloudSyncDataGet(response) }
                }

                server.post("/api/cloud-sync/data") { request, response ->
                    withAuth(request, response) { handleCloudSyncDataPost(request, response) }
                }

                server.get("/api/about") { request, response ->
                    withAuth(request, response) { handleAboutGet(response) }
                }

                server.get("/api/logs") { request, response ->
                    withAuth(request, response) { handleLogsGet(response) }
                }

                if (server !== current) return@launch
                server.listen(AsyncServer.getDefault(), SERVER_PORT)

                log.i("设置服务已启动: http://${getLocalIpAddress()}:${SERVER_PORT}")
            } catch (ex: Exception) {
                synchronized(this@HttpServer) {
                    if (server === current) server = null
                }
                log.e("设置服务启动失败: ${ex.message}", ex)
                launch(Dispatchers.Main) {
                    Snackbar.show("设置服务启动失败", type = SnackbarType.ERROR)
                }
            }
        }
    }

    fun stop() {
        val current = synchronized(this) {
            server.also { server = null }
        }
        activeToken = null
        runCatching { current?.stop() }
    }

    private fun authToken(context: Context): String {
        val pref = context.getSharedPreferences(AUTH_PREF_NAME, Context.MODE_PRIVATE)
        val saved = pref.getString(AUTH_TOKEN_KEY, null)
        if (!saved.isNullOrBlank()) return saved

        return UUID.randomUUID().toString().replace("-", "").also {
            pref.edit().putString(AUTH_TOKEN_KEY, it).apply()
        }
    }

    private fun requestToken(request: AsyncHttpServerRequest): String? {
        request.query.getString("token")?.takeIf { it.isNotBlank() }?.let { return it }
        request.headers["X-Auth-Token"]?.takeIf { it.isNotBlank() }?.let { return it }

        val cookie = request.headers["Cookie"] ?: return null
        return cookie.split(";")
            .map { it.trim() }
            .firstOrNull { it.substringBefore("=") == "http_server_token" }
            ?.substringAfter("=", "")
            ?.takeIf { it.isNotBlank() }
    }

    private fun authorize(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
        setCookie: Boolean = false,
    ): Boolean {
        val expected = activeToken
        if (expected == null || requestToken(request) != expected) {
            response.code(401).send("Unauthorized")
            return false
        }
        if (setCookie) {
            response.headers.set(
                "Set-Cookie",
                "http_server_token=$expected; Path=/; HttpOnly; SameSite=Strict",
            )
        }
        return true
    }

    private inline fun withAuth(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
        setCookie: Boolean = false,
        block: () -> Unit,
    ) {
        if (authorize(request, response, setCookie)) block()
    }

    private fun wrapResponse(response: AsyncHttpServerResponse) = response.apply {
        headers.set("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS")
        headers.set("Access-Control-Allow-Origin", "*")
        headers.set("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token")
    }

    private fun responseSuccess(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send("{\"code\": 0}")
        }
    }

    private fun handleRawResource(
        response: AsyncHttpServerResponse,
        context: Context,
        contentType: String,
        id: Int,
    ) {
        wrapResponse(response).apply {
            setContentType(contentType)
            send(context.resources.openRawResource(id).readBytes().decodeToString())
        }
    }

    private fun handleAssets(
        response: AsyncHttpServerResponse,
        context: Context,
        contentType: String,
        filename: String,
    ) {
        wrapResponse(response).apply {
            setContentType(contentType)
            send(context.assets.open(filename).reader().readText())
        }
    }

    private fun handleGetInfo(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send(
                Globals.json.encodeToString(
                    AppInfo(
                        appTitle = Constants.APP_TITLE,
                        appRepo = Constants.APP_REPO,
                        logHistory = Logger.history,
                    )
                )
            )
        }
    }

    private fun handleIptvSourcePush(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
    ) {
        val body = request.getBody<JSONObjectBody>().get()
        val name = body.get("name").toString()
        val type = body.get("type").toString()
        val url = body.get("url").toString()
        val filePath = body.get("filePath").toString()
        val content = body.get("content").toString()

        var newIptvSource: IptvSource? = null

        when (type) {
            "url" -> {
                newIptvSource = IptvSource(name, url)
            }

            "file" -> {
                if (!filePath.startsWith(Globals.fileDir.path + File.separator)) {
                    response.code(400).send("Invalid file path")
                    return
                }
                newIptvSource = IptvSource(name, filePath, true)
            }

            "content" -> {
                val file =
                    File(Globals.fileDir, "iptv_source_local_${System.currentTimeMillis()}.txt")
                file.writeText(content)
                newIptvSource = IptvSource(name, file.path, true)
            }
        }

        newIptvSource?.let {
            Configs.iptvSourceList = IptvSourceList(Configs.iptvSourceList + it)
            Configs.iptvSourceCurrent = it
        }

        responseSuccess(response)
    }

    private fun handleEpgSourcePush(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
    ) {
        val body = request.getBody<JSONObjectBody>().get()
        val name = body.get("name").toString()
        val url = body.get("url").toString()

        EpgSource(name, url).let {
            Configs.epgSourceList = EpgSourceList(Configs.epgSourceList + it)
            Configs.epgSourceCurrent = it
        }

        responseSuccess(response)
    }

    private fun handleGetChannelAlias(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            send(runCatching { ChannelAlias.aliasFile.readText() }.getOrElse { "" })
        }
    }

    private fun handleUpdateChannelAlias(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
    ) {
        val alias = request.getBody<StringBody>().get()

        ChannelAlias.aliasFile.writeText(alias)
        runBlocking {
            IptvRepository.clearAllCache()
            EpgRepository.clearAllCache()
        }

        responseSuccess(response)
    }

    private fun handleConfigsGet(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send(Globals.json.encodeToString(Configs.toPartial().desensitized()))
        }
    }

    private fun handleConfigsPush(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
    ) {
        val body = request.getBody<JSONObjectBody>().get()
        val configs = Globals.json.decodeFromString<Configs.Partial>(body.toString())
        // 配置接口不允许写入云同步密钥、播放历史等敏感字段
        Configs.fromPartial(configs.desensitized())

        responseSuccess(response)
    }

    private fun handleCloudSyncDataGet(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send(Globals.json.encodeToString(runBlocking { CloudSync.getData() }))
        }
    }

    private fun handleCloudSyncDataPost(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse
    ) {
        val body = request.getBody<JSONObjectBody>().get()
        val data = Globals.json.decodeFromString<CloudSyncData>(body.toString())
        runBlocking { data.apply() }

        responseSuccess(response)
    }

    private fun handleAboutGet(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send(
                Globals.json.encodeToString(
                    AppAbout(
                        applicationId = BuildConfig.APPLICATION_ID,
                        flavor = BuildConfig.FLAVOR,
                        buildType = BuildConfig.BUILD_TYPE,
                        versionCode = BuildConfig.VERSION_CODE,
                        versionName = BuildConfig.VERSION_NAME,
                        deviceName = Globals.deviceName,
                        deviceId = Globals.deviceId,
                    )
                )
            )
        }
    }

    private fun handleLogsGet(response: AsyncHttpServerResponse) {
        wrapResponse(response).apply {
            setContentType("application/json")
            send(Globals.json.encodeToString(Logger.history))
        }
    }

    private fun getLocalIpAddress(): String {
        val defaultIp = "127.0.0.1"

        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val ip = inetAddress.hostAddress ?: defaultIp
                        val isValid = when {
                            ip.startsWith("192.168.") -> true
                            ip.startsWith("10.") -> true
                            ip.startsWith("172.") -> {
                                val parts = ip.split(".")
                                val secondOctet = parts.getOrNull(1)?.toIntOrNull()
                                secondOctet in 16..31
                            }

                            else -> false
                        }

                        if (isValid)
                            return ip
                    }
                }
            }
            return defaultIp
        } catch (ex: SocketException) {
            log.e("IP Address: ${ex.message}", ex)
            return defaultIp
        }
    }

    fun startService(context: Context) {
        // 远程配置服务只作为开发者工具启用，Release 默认不监听局域网
        if (!Configs.debugDeveloperMode) return
        runCatching {
            context.startService(Intent(context, HttpServerService::class.java))
        }.onFailure { it.printStackTrace() }
    }

    fun stopService(context: Context) {
        stop()
        runCatching {
            context.stopService(Intent(context, HttpServerService::class.java))
        }.onFailure { it.printStackTrace() }
    }
}

@Serializable
private data class AppInfo(
    val appTitle: String,
    val appRepo: String,
    val logHistory: List<Logger.HistoryItem>,
)

@Serializable
private data class AppAbout(
    val applicationId: String,
    val flavor: String,
    val buildType: String,
    val versionCode: Int,
    val versionName: String,
    val deviceName: String,
    val deviceId: String,
)