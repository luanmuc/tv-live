package com.doubao.tv.core.data.utils

import com.doubao.tv.core.data.BuildConfig
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList

/**
 * 常量
 */
object Constants {
    /**
     * 应用 标题
     */
    const val APP_TITLE = "电视台"

    /**
     * 应用 代码仓库（本仓库：软件 + 源一起维护）
     */
    const val APP_REPO = "https://github.com/luanmuc/tv-live"

    /**
     * 源仓库在线地址（在线订阅用）
     */
    const val SOURCES_BASE_URL = "https://raw.githubusercontent.com/luanmuc/tv-live/main/sources/dist/"

    /**
     * 国内对象存储/CDN 地址（可选，必须以 / 结尾）。
     * 例如阿里云 OSS：https://tv-live-sources.oss-cn-beijing.aliyuncs.com/dist/
     * 填了就会排在所有 GitHub 镜像之前。
     */
    val SOURCES_DOMESTIC_BASE_URL = BuildConfig.SOURCES_DOMESTIC_BASE_URL

    /**
     * 国内可访问的 GitHub 加速前缀（代理，不是自有源）。
     */
    const val SOURCES_PROXY_BASE_URL = "https://gh-proxy.org/" + SOURCES_BASE_URL

    /**
     * 直播源
     *
     * 内置源由 BuiltinSources 释放到私有目录，离线可用；
     * 联网时会自动从本仓库 dist 目录拉取最新版本覆盖（失败则继续用旧文件）。
     */
    val IPTV_SOURCE_LIST: IptvSourceList by lazy {
        IptvSourceList(
            listOf(
                IptvSource(
                    name = "内置·央视卫视（联网自动更新）",
                    url = java.io.File(Globals.fileDir, "sources/pack-cctv-satellite.m3u").path,
                    isLocal = true,
                ),
                IptvSource(
                    name = "内置·港澳台及国际（联网自动更新）",
                    url = java.io.File(Globals.fileDir, "sources/intl-hkmo-tw.m3u").path,
                    isLocal = true,
                ),
            )
        )
    }

    /**
     * 直播源缓存时间（毫秒）
     */
    const val IPTV_SOURCE_CACHE_TIME = 1000 * 60 * 60 * 24L // 24小时

    /**
     * 节目单来源
     *
     * fanmingming 的 live.fanmingming.com / .cn 已不可用（502/超时），
     * 改走 GitHub 直链，jsDelivr 作为备用，均按频道名（tvg-name）匹配。
     */
    val EPG_SOURCE_LIST = EpgSourceList(
        listOf(
            EpgSource(
                name = "国内节目单 fanmingming",
                url = "https://raw.githubusercontent.com/fanmingming/live/main/e.xml",
            ),
            EpgSource(
                name = "国内节目单 fanmingming（jsDelivr）",
                url = "https://cdn.jsdelivr.net/gh/fanmingming/live@main/e.xml",
            ),
        )
    )

    /**
     * 节目单刷新时间阈值（小时）
     */
    const val EPG_REFRESH_TIME_THRESHOLD = 2 // 不到2点不刷新

    /**
     * 频道图标提供
     *
     * {name} 频道名称
     *
     * {name|lowercase} 转成小写
     *
     * {name|uppercase} 转成大写
     *
     */
    const val CHANNEL_LOGO_PROVIDER = "https://raw.githubusercontent.com/fanmingming/live/main/tv/{name|uppercase}.png"

    /**
     * 下载直链前缀（留空表示直连 GitHub）
     */
    const val GITHUB_PROXY = ""

    /**
     * 最新版本信息：统一指向本仓库的 Release
     */
    val GIT_RELEASE_LATEST_URL = mapOf(
        "stable" to "https://api.github.com/repos/luanmuc/tv-live/releases/latest",
        "beta" to "https://api.github.com/repos/luanmuc/tv-live/releases/latest",
        "dev" to "https://api.github.com/repos/luanmuc/tv-live/releases/latest",
    )

    /**
     * 网络请求重试次数
     */
    const val NETWORK_RETRY_COUNT = 10L

    /**
     * 网络请求重试间隔时间（毫秒）
     */
    const val NETWORK_RETRY_INTERVAL = 3000L

    /**
     * 播放器 userAgent
     */
    const val VIDEO_PLAYER_USER_AGENT = "okhttp"

    /**
     * 播放器加载超时
     */
    const val VIDEO_PLAYER_LOAD_TIMEOUT = 1000L * 15 // 15秒

    /**
     * 日志历史最大保留条数
     */
    const val LOG_HISTORY_MAX_SIZE = 100

    /**
     * 界面 临时频道界面显示时间
     */
    const val UI_TEMP_CHANNEL_SCREEN_SHOW_DURATION = 2000L // 2秒

    /**
     * 界面 超时未操作自动关闭界面
     */
    const val UI_SCREEN_AUTO_CLOSE_DELAY = 1000L * 15 // 15秒

    /**
     * 界面 时间显示前后范围
     */
    const val UI_TIME_SCREEN_SHOW_DURATION = 1000L * 30 // 前后30秒
}