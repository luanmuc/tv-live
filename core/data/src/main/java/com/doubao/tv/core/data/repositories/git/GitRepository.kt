package com.doubao.tv.core.data.repositories.git

import com.doubao.tv.core.data.entities.git.GitRelease
import com.doubao.tv.core.data.network.request
import com.doubao.tv.core.data.repositories.git.parser.GitReleaseParser
import com.doubao.tv.core.data.utils.Loggable

/**
 * git数据获取
 */
class GitRepository : Loggable("GitRepository") {

    /**
     * 获取最新发行版
     */
    suspend fun latestRelease(url: String): GitRelease {
        log.d("获取最新发行版: $url")

        try {
            val parser = GitReleaseParser.instances.first { it.isSupport(url) }
            return url.request { body -> parser.parse(body.string()) }!!
        } catch (ex: Exception) {
            log.e("获取最新发行版失败", ex)
            throw Exception("获取最新发行版失败，请检查网络连接", ex)
        }
    }
}