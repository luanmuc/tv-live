package com.doubao.tv.sync

enum class CloudSyncProvider(
    val value: Int,
    val label: String,
    val supportPull: Boolean,
    val supportPush: Boolean,
) {
    GITHUB_GIST(0, "GitHub Gist", true, true),
    GITEE_GIST(1, "Gitee 代码片段", true, true),
    NETWORK_URL(2, "网络链接", true, false),
    LOCAL_FILE(3, "本地文件", true, true),
    WEBDAV(4, "WebDAV", true, true);

    companion object {
        fun fromValue(value: Int): CloudSyncProvider {
            return CloudSyncProvider.entries.firstOrNull { it.value == value } ?: GITHUB_GIST
        }
    }
}