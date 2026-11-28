package com.doubao.tv.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import com.doubao.tv.core.data.utils.ChannelAlias
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.Loggable
import com.doubao.tv.BuildConfig
import com.doubao.tv.sync.repositories.CloudSyncRepository
import com.doubao.tv.sync.repositories.GiteeGistSyncRepository
import com.doubao.tv.sync.repositories.GithubGistSyncRepository
import com.doubao.tv.sync.repositories.LocalFileSyncRepository
import com.doubao.tv.sync.repositories.NetworkUrlSyncRepository
import com.doubao.tv.sync.repositories.WebDavSyncRepository
import com.doubao.tv.ui.utils.Configs
import java.io.File

object CloudSync : Loggable("CloudSync") {
    private fun getRepository(): CloudSyncRepository {
        return when (Configs.cloudSyncProvider) {
            CloudSyncProvider.GITHUB_GIST -> GithubGistSyncRepository(
                Configs.cloudSyncGithubGistId,
                Configs.cloudSyncGithubGistToken,
            )

            CloudSyncProvider.GITEE_GIST -> GiteeGistSyncRepository(
                Configs.cloudSyncGiteeGistId,
                Configs.cloudSyncGiteeGistToken,
            )

            CloudSyncProvider.NETWORK_URL -> NetworkUrlSyncRepository(Configs.cloudSyncNetworkUrl)
            CloudSyncProvider.LOCAL_FILE -> LocalFileSyncRepository(Configs.cloudSyncLocalFilePath)
            CloudSyncProvider.WEBDAV -> WebDavSyncRepository(
                Configs.cloudSyncWebDavUrl,
                Configs.cloudSyncWebDavUsername,
                Configs.cloudSyncWebDavPassword,
            )
        }
    }

    suspend fun getData(): CloudSyncData = withContext(Dispatchers.IO) {
        val configs = Configs.toPartial()
        CloudSyncData(
            version = BuildConfig.VERSION_NAME,
            syncAt = System.currentTimeMillis(),
            syncFrom = Globals.deviceName,
            configs = configs.desensitized(),
            extraLocalIptvSourceList = configs.iptvSourceList
                ?.filter { it.isLocal && it.url.startsWith(Globals.fileDir.path) }
                ?.associate { it.url to runCatching { File(it.url).readText() }.getOrDefault("") },
            extraChannelNameAlias = runCatching { ChannelAlias.aliasFile.readText() }.getOrDefault(""),
        )
    }

    suspend fun push(): Boolean {
        log.i("推送云端数据(${Configs.cloudSyncProvider.label})")
        return getRepository().push(getData())
    }

    suspend fun pull(): CloudSyncData {
        log.i("拉取云端数据(${Configs.cloudSyncProvider.label})")
        return getRepository().pull().let {
            it.copy(configs = it.configs.desensitized())
        }
    }
}

@Serializable
data class CloudSyncData(
    val version: String = "",
    val syncAt: Long = 0,
    val syncFrom: String = "",
    val description: String? = null,
    val configs: Configs.Partial = Configs.Partial(),
    val extraLocalIptvSourceList: Map<String, String>? = null,
    val extraChannelNameAlias: String? = null,
) {
    companion object {
        val EMPTY = CloudSyncData()
    }

    suspend fun apply() {
        val that = this
        withContext(Dispatchers.IO) {
            Configs.fromPartial(that.configs)
            that.extraLocalIptvSourceList?.entries
                ?.filter { it.key.startsWith(Globals.fileDir.path) }
                ?.forEach { entry ->
                    File(entry.key).writeText(entry.value)
                }
            that.extraChannelNameAlias?.let {
                ChannelAlias.aliasFile.writeText(it)
            }
        }
    }
}
