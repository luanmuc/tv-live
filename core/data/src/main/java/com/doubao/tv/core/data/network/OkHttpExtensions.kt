package com.doubao.tv.core.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resumeWithException

private val defaultClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()
}

/**
 * 只对显式标记的源创建“信任自签名证书”的客户端；不修改全局 TLS 校验。
 * 客户端禁用重定向，避免从被信任 host 跳到任意第三方 host。
 */
object SelfSignedTls {
    private val clients = ConcurrentHashMap<String, OkHttpClient>()

    fun clientFor(host: String): OkHttpClient = clients.getOrPut(host.lowercase()) {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { candidate, _ -> candidate.equals(host, ignoreCase = true) }
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) {
                    if (response.body != null) {
                        response.closeQuietly()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        }
    )

    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (t: Throwable) {
            // Ignore cancel exception
        }
    }
}

suspend fun <T> String.request(
    builder: (Request.Builder) -> Request.Builder = { it -> it },
    action: suspend CoroutineScope.(ResponseBody) -> T,
) = request(false, builder, action)

suspend fun <T> String.request(
    builder: (Request.Builder) -> Request.Builder = { it -> it },
    action: suspend CoroutineScope.(Response, Request) -> T,
): T = request(false, builder, action)

suspend fun <T> String.request(
    allowSelfSignedTls: Boolean,
    builder: (Request.Builder) -> Request.Builder = { it -> it },
    action: suspend CoroutineScope.(ResponseBody) -> T,
) = request(allowSelfSignedTls, builder) { response, _ -> response.body?.let { action(it) } }

suspend fun <T> String.request(
    allowSelfSignedTls: Boolean,
    builder: (Request.Builder) -> Request.Builder = { it -> it },
    action: suspend CoroutineScope.(Response, Request) -> T,
): T {
    val url = this

    return withContext(Dispatchers.IO) {
        // 复用连接池/线程池；普通源使用系统默认 TLS 校验，只有显式标记的源才用单 host 例外。
        val request = Request.Builder()
            .url(url)
            .let(builder)
            .build()

        val client = if (allowSelfSignedTls) {
            toHttpUrlOrNull()?.host?.let { SelfSignedTls.clientFor(it) } ?: defaultClient
        } else {
            defaultClient
        }
        val response = client.newCall(request).await()

        try {
            if (!response.isSuccessful) throw Exception("${response.code}: ${response.message}")
            action(response, response.request)
        } finally {
            response.close()
        }
    }
}
