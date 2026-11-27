package com.doubao.tv.sync.repositories

import kotlinx.serialization.encodeToString
import okhttp3.Credentials
import okhttp3.RequestBody.Companion.toRequestBody
import com.doubao.tv.core.data.network.request
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.Loggable
import com.doubao.tv.core.util.utils.ensureSuffix
import com.doubao.tv.sync.CloudSyncData

class WebDavSyncRepository(
    private val url: String,
    private val username: String,
    private val password: String
) : CloudSyncRepository, Loggable("WebDavSyncRepository") {

    private val fullUrl by lazy {
        val hasFileName = url.split("/").last().contains(".")
        if (hasFileName) url
        else url.ensureSuffix("/") + "all_configs.json"
    }

    override suspend fun push(data: CloudSyncData): Boolean {
        try {
            return fullUrl.request({ builder ->
                builder
                    .header("Authorization", Credentials.basic(username, password))
                    .put(Globals.json.encodeToString(data).toRequestBody())
            }) { _ -> true }!!
        } catch (ex: Exception) {
            log.e("推送云端失败", ex)
            throw Exception("推送云端失败", ex)
        }
    }

    override suspend fun pull(): CloudSyncData {
        try {
            return fullUrl.request({ builder ->
                builder.header("Authorization", Credentials.basic(username, password))
            }) { body ->
                Globals.json.decodeFromString<CloudSyncData>(body.string())
            } ?: CloudSyncData.EMPTY
        } catch (ex: Exception) {
            log.e("拉取云端失败", ex)
            throw Exception("拉取云端失败", ex)
        }
    }
}