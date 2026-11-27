package com.doubao.tv.sync.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.Loggable
import com.doubao.tv.core.util.utils.ensureSuffix
import com.doubao.tv.sync.CloudSyncData
import java.io.File

class LocalFileSyncRepository(
    private val path: String
) : CloudSyncRepository, Loggable("LocalFileSyncRepository") {

    val file by lazy {
        val hasFileName = path.split("/").last().contains(".")
        if (hasFileName) File(path.substringAfter("file://"))
        else File(path.substringAfter("file://").ensureSuffix("/") + "all_configs.json")
    }

    override suspend fun push(data: CloudSyncData): Boolean = withContext(Dispatchers.IO) {
        try {
            file.writeText(Globals.json.encodeToString(data))
            true
        } catch (ex: Exception) {
            log.e("推送云端失败", ex)
            throw Exception("推送云端失败", ex)
        }
    }

    override suspend fun pull() = withContext(Dispatchers.IO) {
        try {
            val text = file.readText()
            Globals.json.decodeFromString<CloudSyncData>(text)
        } catch (ex: Exception) {
            log.e("拉取云端失败", ex)
            throw Exception("拉取云端失败", ex)
        }
    }
}