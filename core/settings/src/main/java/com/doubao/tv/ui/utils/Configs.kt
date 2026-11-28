package com.doubao.tv.ui.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserveList
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.SP
import com.doubao.tv.ui.screen.components.AppThemeDef
import com.doubao.tv.settings.VideoPlayerDisplayMode

/**
 * 应用配置
 */
object Configs {
    enum class KEY {
        /** ==================== 应用 ==================== */
        /** 开机自启 */
        APP_BOOT_LAUNCH,

        /** 画中画启用 */
        APP_PIP_ENABLE,

        /** 上一次最新版本 */
        APP_LAST_LATEST_VERSION,

        /** 协议已同意 */
        APP_AGREEMENT_AGREED,

        /** 打开直接进入直播 */
        APP_STARTUP_SCREEN,

        /** ==================== 直播源 ==================== */
        /** 当前直播源 */
        IPTV_SOURCE_CURRENT,

        /** 直播源列表 */
        IPTV_SOURCE_LIST,

        /** 直播源缓存时间（毫秒） */
        IPTV_SOURCE_CACHE_TIME,

        /** 直播源分组隐藏列表 */
        IPTV_CHANNEL_GROUP_HIDDEN_LIST,

        /** 混合模式 */
        IPTV_HYBRID_MODE,

        /** 相似频道合并 */
        IPTV_SIMILAR_CHANNEL_MERGE,

        /** 频道图标提供 */
        IPTV_CHANNEL_LOGO_PROVIDER,

        /** 频道图标覆盖 */
        IPTV_CHANNEL_LOGO_OVERRIDE,

        /** 是否启用直播源频道收藏 */
        IPTV_CHANNEL_FAVORITE_ENABLE,

        /** 显示直播源频道收藏列表 */
        IPTV_CHANNEL_FAVORITE_LIST_VISIBLE,

        /** 直播源频道收藏列表 */
        IPTV_CHANNEL_FAVORITE_LIST,

        /** 上一次播放频道 */
        IPTV_CHANNEL_LAST_PLAY,

        /** 直播源线路可播放host列表 */
        IPTV_CHANNEL_LINE_PLAYABLE_HOST_LIST,

        /** 直播源线路可播放地址列表 */
        IPTV_CHANNEL_LINE_PLAYABLE_URL_LIST,

        /** 换台反转 */
        IPTV_CHANNEL_CHANGE_FLIP,

        /** 是否启用数字选台 */
        IPTV_CHANNEL_NO_SELECT_ENABLE,

        /** 换台列表首尾循环 **/
        IPTV_CHANNEL_CHANGE_LIST_LOOP,

        /** ==================== 节目单 ==================== */
        /** 启用节目单 */
        EPG_ENABLE,

        /** 当前节目单来源 */
        EPG_SOURCE_CURRENT,

        /** 节目单来源列表 */
        EPG_SOURCE_LIST,

        /** 节目单刷新时间阈值（小时） */
        EPG_REFRESH_TIME_THRESHOLD,

        /** 节目单跟随直播源 */
        EPG_SOURCE_FOLLOW_IPTV,

        /** 节目预约列表 */
        EPG_CHANNEL_RESERVE_LIST,

        /** ==================== 界面 ==================== */
        /** 显示节目进度 */
        UI_SHOW_EPG_PROGRAMME_PROGRESS,

        /** 显示常驻节目进度 */
        UI_SHOW_EPG_PROGRAMME_PERMANENT_PROGRESS,

        /** 显示台标 */
        UI_SHOW_CHANNEL_LOGO,

        /** 显示频道预览 */
        UI_SHOW_CHANNEL_PREVIEW,

        /** 使用经典选台界面 */
        UI_USE_CLASSIC_PANEL_SCREEN,

        /** 界面密度缩放比例 */
        UI_DENSITY_SCALE_RATIO,

        /** 界面字体缩放比例 */
        UI_FONT_SCALE_RATIO,

        /** 时间显示模式 */
        UI_TIME_SHOW_MODE,

        /** 焦点优化 */
        UI_FOCUS_OPTIMIZE,

        /** 自动关闭界面延时 */
        UI_SCREEN_AUTO_CLOSE_DELAY,

        /** ==================== 更新 ==================== */
        /** 更新强提醒 */
        UPDATE_FORCE_REMIND,

        /** 更新通道 */
        UPDATE_CHANNEL,

        /** ==================== 播放器 ==================== */
        /** 播放器 内核 */
        VIDEO_PLAYER_CORE,

        /** 播放器 渲染方式 */
        VIDEO_PLAYER_RENDER_MODE,

        /** 播放器 自定义ua */
        VIDEO_PLAYER_USER_AGENT,

        /** 播放器 自定义headers */
        VIDEO_PLAYER_HEADERS,

        /** 播放器 加载超时 */
        VIDEO_PLAYER_LOAD_TIMEOUT,

        /** 播放器 显示模式 */
        VIDEO_PLAYER_DISPLAY_MODE,

        /** 播放器 强制音频软解 */
        VIDEO_PLAYER_FORCE_AUDIO_SOFT_DECODE,

        /** 播放器 停止上一媒体项 */
        VIDEO_PLAYER_STOP_PREVIOUS_MEDIA_ITEM,

        /** 播放器 跳过同一VSync渲染多帧 */
        VIDEO_PLAYER_SKIP_MULTIPLE_FRAMES_ON_SAME_VSYNC,

        /** ==================== 主题 ==================== */
        /** 当前应用主题 */
        THEME_APP_CURRENT,

        /** 肥羊 AllInOne 文件路径 */
        FEIYANG_ALLINONE_FILE_PATH,
    }

    /** ==================== 应用 ==================== */
    /** 开机自启 */
    var appBootLaunch: Boolean
        get() = SP.getBoolean(KEY.APP_BOOT_LAUNCH.name, false)
        set(value) = SP.putBoolean(KEY.APP_BOOT_LAUNCH.name, value)

    /** 画中画启用 */
    var appPipEnable: Boolean
        get() = SP.getBoolean(KEY.APP_PIP_ENABLE.name, false)
        set(value) = SP.putBoolean(KEY.APP_PIP_ENABLE.name, value)

    /** 上一次最新版本 */
    var appLastLatestVersion: String
        get() = SP.getString(KEY.APP_LAST_LATEST_VERSION.name, "")
        set(value) = SP.putString(KEY.APP_LAST_LATEST_VERSION.name, value)

    /** 协议已同意 */
    var appAgreementAgreed: Boolean
        get() = SP.getBoolean(KEY.APP_AGREEMENT_AGREED.name, false)
        set(value) = SP.putBoolean(KEY.APP_AGREEMENT_AGREED.name, value)

    /** 起始界面 */
    var appStartupScreen: String
        get() = SP.getString(KEY.APP_STARTUP_SCREEN.name, "Live")
        set(value) = SP.putString(KEY.APP_STARTUP_SCREEN.name, value)

    /** ==================== 直播源 ==================== */
    /** 当前直播源 */
    var iptvSourceCurrent: IptvSource
        get() {
            val source = Globals.json.decodeFromString<IptvSource>(
                SP.getString(KEY.IPTV_SOURCE_CURRENT.name, "")
                    .ifBlank { Globals.json.encodeToString(Constants.IPTV_SOURCE_LIST.first()) }
            )
            // 1.4.3 及以前有独立的「在线·自动更新」源，现已并入内置源
            return if (source.url.startsWith(Constants.SOURCES_BASE_URL)) {
                Constants.IPTV_SOURCE_LIST.first()
            } else {
                source
            }
        }
        set(value) = SP.putString(KEY.IPTV_SOURCE_CURRENT.name, Globals.json.encodeToString(value))

    /** 直播源列表 */
    var iptvSourceList: IptvSourceList
        get() = Globals.json.decodeFromString(
            SP.getString(KEY.IPTV_SOURCE_LIST.name, Globals.json.encodeToString(IptvSourceList()))
        )
        set(value) = SP.putString(KEY.IPTV_SOURCE_LIST.name, Globals.json.encodeToString(value))

    /** 直播源缓存时间（毫秒） */
    var iptvSourceCacheTime: Long
        get() = SP.getLong(KEY.IPTV_SOURCE_CACHE_TIME.name, Constants.IPTV_SOURCE_CACHE_TIME)
        set(value) = SP.putLong(KEY.IPTV_SOURCE_CACHE_TIME.name, value)

    /** 直播源分组隐藏列表 */
    var iptvChannelGroupHiddenList: Set<String>
        get() = SP.getStringSet(KEY.IPTV_CHANNEL_GROUP_HIDDEN_LIST.name, emptySet())
        set(value) = SP.putStringSet(KEY.IPTV_CHANNEL_GROUP_HIDDEN_LIST.name, value)

    /** 混合模式 */
    var iptvHybridMode: IptvHybridMode
        get() = IptvHybridMode.fromValue(
            SP.getInt(KEY.IPTV_HYBRID_MODE.name, IptvHybridMode.DISABLE.value)
        )
        set(value) = SP.putInt(KEY.IPTV_HYBRID_MODE.name, value.value)

    /** 相似频道合并 */
    var iptvSimilarChannelMerge: Boolean
        get() = SP.getBoolean(KEY.IPTV_SIMILAR_CHANNEL_MERGE.name, false)
        set(value) = SP.putBoolean(KEY.IPTV_SIMILAR_CHANNEL_MERGE.name, value)

    /** 频道图标提供 */
    var iptvChannelLogoProvider: String
        get() = SP.getString(KEY.IPTV_CHANNEL_LOGO_PROVIDER.name, Constants.CHANNEL_LOGO_PROVIDER)
        set(value) = SP.putString(KEY.IPTV_CHANNEL_LOGO_PROVIDER.name, value)

    /** 频道图标覆盖 */
    var iptvChannelLogoOverride: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_LOGO_OVERRIDE.name, false)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_LOGO_OVERRIDE.name, value)

    /** 是否启用直播源频道收藏 */
    var iptvChannelFavoriteEnable: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_FAVORITE_ENABLE.name, true)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_FAVORITE_ENABLE.name, value)

    /** 显示直播源频道收藏列表 */
    var iptvChannelFavoriteListVisible: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_FAVORITE_LIST_VISIBLE.name, false)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_FAVORITE_LIST_VISIBLE.name, value)

    /** 直播源频道收藏列表 */
    var iptvChannelFavoriteList: ChannelFavoriteList
        get() = Globals.json.decodeFromString(
            SP.getString(
                KEY.IPTV_CHANNEL_FAVORITE_LIST.name,
                Globals.json.encodeToString(ChannelFavoriteList())
            )
        )
        set(value) = SP.putString(
            KEY.IPTV_CHANNEL_FAVORITE_LIST.name,
            Globals.json.encodeToString(value)
        )

    /** 上一次播放频道 */
    var iptvChannelLastPlay: Channel
        get() = Globals.json.decodeFromString(
            SP.getString(
                KEY.IPTV_CHANNEL_LAST_PLAY.name,
                Globals.json.encodeToString(Channel.EMPTY)
            )
        )
        set(value) = SP.putString(
            KEY.IPTV_CHANNEL_LAST_PLAY.name,
            Globals.json.encodeToString(value)
        )

    /** 直播源线路可播放host列表 */
    var iptvChannelLinePlayableHostList: Set<String>
        get() = SP.getStringSet(KEY.IPTV_CHANNEL_LINE_PLAYABLE_HOST_LIST.name, emptySet())
        set(value) = SP.putStringSet(KEY.IPTV_CHANNEL_LINE_PLAYABLE_HOST_LIST.name, value)

    /** 直播源线路可播放地址列表 */
    // IPTV_CHANNEL_LINE_PLAYABLE_URL_LIST,
    var iptvChannelLinePlayableUrlList: Set<String>
        get() = SP.getStringSet(KEY.IPTV_CHANNEL_LINE_PLAYABLE_URL_LIST.name, emptySet())
        set(value) = SP.putStringSet(KEY.IPTV_CHANNEL_LINE_PLAYABLE_URL_LIST.name, value)

    /** 换台反转 */
    var iptvChannelChangeFlip: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_CHANGE_FLIP.name, false)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_CHANGE_FLIP.name, value)

    /** 是否启用数字选台 */
    var iptvChannelNoSelectEnable: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_NO_SELECT_ENABLE.name, true)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_NO_SELECT_ENABLE.name, value)

    /** 换台列表首尾循环 **/
    var iptvChannelChangeListLoop: Boolean
        get() = SP.getBoolean(KEY.IPTV_CHANNEL_CHANGE_LIST_LOOP.name, false)
        set(value) = SP.putBoolean(KEY.IPTV_CHANNEL_CHANGE_LIST_LOOP.name, value)

    /** ==================== 节目单 ==================== */
    /** 启用节目单 */
    var epgEnable: Boolean
        get() = SP.getBoolean(KEY.EPG_ENABLE.name, true)
        set(value) = SP.putBoolean(KEY.EPG_ENABLE.name, value)

    /** 当前节目单来源 */
    var epgSourceCurrent: EpgSource
        get() {
            val stored = Globals.json.decodeFromString<EpgSource>(
                SP.getString(KEY.EPG_SOURCE_CURRENT.name, "")
                    .ifBlank { Globals.json.encodeToString(Constants.EPG_SOURCE_LIST.first()) }
            )
            // 老版本存的 live.fanmingming.com/.cn 已失效，自动切到新默认源
            return if (stored.url.isDeadFanmingmingHost()) Constants.EPG_SOURCE_LIST.first()
            else stored
        }
        set(value) = SP.putString(KEY.EPG_SOURCE_CURRENT.name, Globals.json.encodeToString(value))

    /** 节目单来源列表 */
    var epgSourceList: EpgSourceList
        get() {
            val stored = Globals.json.decodeFromString<EpgSourceList>(
                SP.getString(KEY.EPG_SOURCE_LIST.name, Globals.json.encodeToString(EpgSourceList()))
            )
            return EpgSourceList(
                stored.map { source ->
                    if (source.url.isDeadFanmingmingHost()) source.copy(url = source.url.fixDeadHost())
                    else source
                }
            )
        }
        set(value) = SP.putString(KEY.EPG_SOURCE_LIST.name, Globals.json.encodeToString(value))

    /** 节目单刷新时间阈值（小时） */
    var epgRefreshTimeThreshold: Int
        get() = SP.getInt(KEY.EPG_REFRESH_TIME_THRESHOLD.name, Constants.EPG_REFRESH_TIME_THRESHOLD)
        set(value) = SP.putInt(KEY.EPG_REFRESH_TIME_THRESHOLD.name, value)

    /** 节目单跟随直播源 */
    var epgSourceFollowIptv: Boolean
        get() = SP.getBoolean(KEY.EPG_SOURCE_FOLLOW_IPTV.name, false)
        set(value) = SP.putBoolean(KEY.EPG_SOURCE_FOLLOW_IPTV.name, value)

    /** 节目预约列表 */
    var epgChannelReserveList: EpgProgrammeReserveList
        get() = Globals.json.decodeFromString(
            SP.getString(
                KEY.EPG_CHANNEL_RESERVE_LIST.name,
                Globals.json.encodeToString(EpgProgrammeReserveList())
            )
        )
        set(value) = SP.putString(
            KEY.EPG_CHANNEL_RESERVE_LIST.name,
            Globals.json.encodeToString(value)
        )

    /** ==================== 界面 ==================== */
    /** 显示节目进度 */
    var uiShowEpgProgrammeProgress: Boolean
        get() = SP.getBoolean(KEY.UI_SHOW_EPG_PROGRAMME_PROGRESS.name, true)
        set(value) = SP.putBoolean(KEY.UI_SHOW_EPG_PROGRAMME_PROGRESS.name, value)

    /** 显示常驻节目进度 */
    var uiShowEpgProgrammePermanentProgress: Boolean
        get() = SP.getBoolean(KEY.UI_SHOW_EPG_PROGRAMME_PERMANENT_PROGRESS.name, false)
        set(value) = SP.putBoolean(KEY.UI_SHOW_EPG_PROGRAMME_PERMANENT_PROGRESS.name, value)

    /** 显示台标 */
    var uiShowChannelLogo: Boolean
        get() = SP.getBoolean(KEY.UI_SHOW_CHANNEL_LOGO.name, true)
        set(value) = SP.putBoolean(KEY.UI_SHOW_CHANNEL_LOGO.name, value)

    /** 显示频道预览 */
    var uiShowChannelPreview: Boolean
        get() = SP.getBoolean(KEY.UI_SHOW_CHANNEL_PREVIEW.name, false)
        set(value) = SP.putBoolean(KEY.UI_SHOW_CHANNEL_PREVIEW.name, value)

    /** 使用经典选台界面 */
    var uiUseClassicPanelScreen: Boolean
        get() = SP.getBoolean(KEY.UI_USE_CLASSIC_PANEL_SCREEN.name, true)
        set(value) = SP.putBoolean(KEY.UI_USE_CLASSIC_PANEL_SCREEN.name, value)

    /** 界面密度缩放比例 */
    var uiDensityScaleRatio: Float
        get() = SP.getFloat(KEY.UI_DENSITY_SCALE_RATIO.name, 0f)
        set(value) = SP.putFloat(KEY.UI_DENSITY_SCALE_RATIO.name, value)

    /** 界面字体缩放比例 */
    var uiFontScaleRatio: Float
        get() = SP.getFloat(KEY.UI_FONT_SCALE_RATIO.name, 1f)
        set(value) = SP.putFloat(KEY.UI_FONT_SCALE_RATIO.name, value)

    /** 时间显示模式 */
    var uiTimeShowMode: UiTimeShowMode
        get() = UiTimeShowMode.fromValue(
            SP.getInt(KEY.UI_TIME_SHOW_MODE.name, UiTimeShowMode.HIDDEN.value)
        )
        set(value) = SP.putInt(KEY.UI_TIME_SHOW_MODE.name, value.value)

    /** 焦点优化 */
    var uiFocusOptimize: Boolean
        get() = SP.getBoolean(KEY.UI_FOCUS_OPTIMIZE.name, true)
        set(value) = SP.putBoolean(KEY.UI_FOCUS_OPTIMIZE.name, value)

    /** 自动关闭界面延时 */
    var uiScreenAutoCloseDelay: Long
        get() =
            SP.getLong(KEY.UI_SCREEN_AUTO_CLOSE_DELAY.name, Constants.UI_SCREEN_AUTO_CLOSE_DELAY)
        set(value) = SP.putLong(KEY.UI_SCREEN_AUTO_CLOSE_DELAY.name, value)

    /** ==================== 更新 ==================== */
    /** 更新强提醒 */
    var updateForceRemind: Boolean
        get() = SP.getBoolean(KEY.UPDATE_FORCE_REMIND.name, false)
        set(value) = SP.putBoolean(KEY.UPDATE_FORCE_REMIND.name, value)

    /** 更新通道 */
    var updateChannel: String
        get() = SP.getString(KEY.UPDATE_CHANNEL.name, "stable")
        set(value) = SP.putString(KEY.UPDATE_CHANNEL.name, value)

    /** ==================== 播放器 ==================== */
    /** 播放器 内核 */
    var videoPlayerCore: VideoPlayerCore
        get() = VideoPlayerCore.fromValue(
            SP.getInt(KEY.VIDEO_PLAYER_CORE.name, VideoPlayerCore.MEDIA3.value)
        )
        set(value) = SP.putInt(KEY.VIDEO_PLAYER_CORE.name, value.value)

    /** 播放器 渲染方式 */
    var videoPlayerRenderMode: VideoPlayerRenderMode
        get() = VideoPlayerRenderMode.fromValue(
            SP.getInt(KEY.VIDEO_PLAYER_RENDER_MODE.name, VideoPlayerRenderMode.SURFACE_VIEW.value)
        )
        set(value) = SP.putInt(KEY.VIDEO_PLAYER_RENDER_MODE.name, value.value)

    /** 播放器 自定义ua */
    var videoPlayerUserAgent: String
        get() = SP.getString(KEY.VIDEO_PLAYER_USER_AGENT.name, "").ifBlank {
            Constants.VIDEO_PLAYER_USER_AGENT
        }
        set(value) = SP.putString(KEY.VIDEO_PLAYER_USER_AGENT.name, value)

    /** 播放器 自定义headers */
    var videoPlayerHeaders: String
        get() = SP.getString(KEY.VIDEO_PLAYER_HEADERS.name, "")
        set(value) = SP.putString(KEY.VIDEO_PLAYER_HEADERS.name, value)

    /** 播放器 加载超时 */
    var videoPlayerLoadTimeout: Long
        get() = SP.getLong(KEY.VIDEO_PLAYER_LOAD_TIMEOUT.name, Constants.VIDEO_PLAYER_LOAD_TIMEOUT)
        set(value) = SP.putLong(KEY.VIDEO_PLAYER_LOAD_TIMEOUT.name, value)

    /** 播放器 显示模式 */
    var videoPlayerDisplayMode: VideoPlayerDisplayMode
        get() = VideoPlayerDisplayMode.fromValue(
            SP.getInt(KEY.VIDEO_PLAYER_DISPLAY_MODE.name, VideoPlayerDisplayMode.ORIGINAL.value)
        )
        set(value) = SP.putInt(KEY.VIDEO_PLAYER_DISPLAY_MODE.name, value.value)

    /** 播放器 强制音频软解 */
    var videoPlayerForceAudioSoftDecode: Boolean
        get() = SP.getBoolean(KEY.VIDEO_PLAYER_FORCE_AUDIO_SOFT_DECODE.name, false)
        set(value) = SP.putBoolean(KEY.VIDEO_PLAYER_FORCE_AUDIO_SOFT_DECODE.name, value)

    /** 播放器 停止上一媒体项 */
    var videoPlayerStopPreviousMediaItem: Boolean
        get() = SP.getBoolean(KEY.VIDEO_PLAYER_STOP_PREVIOUS_MEDIA_ITEM.name, true)
        set(value) = SP.putBoolean(KEY.VIDEO_PLAYER_STOP_PREVIOUS_MEDIA_ITEM.name, value)

    /** 播放器 跳过同一VSync渲染多帧 */
    var videoPlayerSkipMultipleFramesOnSameVSync: Boolean
        get() = SP.getBoolean(KEY.VIDEO_PLAYER_SKIP_MULTIPLE_FRAMES_ON_SAME_VSYNC.name, true)
        set(value) = SP.putBoolean(KEY.VIDEO_PLAYER_SKIP_MULTIPLE_FRAMES_ON_SAME_VSYNC.name, value)

    /** ==================== 主题 ==================== */
    /** 当前应用主题 */
    var themeAppCurrent: AppThemeDef?
        get() = SP.getString(KEY.THEME_APP_CURRENT.name, "").let {
            if (it.isBlank()) null else Globals.json.decodeFromString(it)
        }
        set(value) = SP.putString(
            KEY.THEME_APP_CURRENT.name,
            value?.let { Globals.json.encodeToString(value) } ?: "")

    /** 肥羊 AllInOne 文件路径 */
    var feiyangAllInOneFilePath: String
        get() = SP.getString(KEY.FEIYANG_ALLINONE_FILE_PATH.name, "")
        set(value) = SP.putString(KEY.FEIYANG_ALLINONE_FILE_PATH.name, value)

    enum class UiTimeShowMode(val value: Int, val label: String) {
        /** 隐藏 */
        HIDDEN(0, "隐藏"),

        /** 常显 */
        ALWAYS(1, "常显"),

        /** 整点 */
        EVERY_HOUR(2, "整点"),

        /** 半点 */
        HALF_HOUR(3, "半点");

        companion object {
            fun fromValue(value: Int): UiTimeShowMode {
                return entries.firstOrNull { it.value == value } ?: ALWAYS
            }
        }
    }

    enum class IptvHybridMode(val value: Int, val label: String) {
        /** 禁用 */
        DISABLE(0, "禁用"),

        /** 直播源优先 */
        IPTV_FIRST(1, "直播源优先"),

        /** 混合优先 */
        HYBRID_FIRST(2, "混合优先");

        companion object {
            fun fromValue(value: Int): IptvHybridMode {
                return entries.firstOrNull { it.value == value } ?: DISABLE
            }
        }
    }

    enum class VideoPlayerCore(val value: Int, val label: String) {
        /** Media3 */
        MEDIA3(0, "Media3"),

        /** IJK */
        IJK(1, "IjkPlayer");

        companion object {
            fun fromValue(value: Int): VideoPlayerCore {
                return entries.firstOrNull { it.value == value } ?: MEDIA3
            }
        }
    }

    enum class VideoPlayerRenderMode(val value: Int, val label: String) {
        /** SurfaceView */
        SURFACE_VIEW(0, "SurfaceView"),

        /** TextureView */
        TEXTURE_VIEW(1, "TextureView");

        companion object {
            fun fromValue(value: Int): VideoPlayerRenderMode {
                return entries.firstOrNull { it.value == value } ?: SURFACE_VIEW
            }
        }
    }


}
/** fanmingming 旧域名已失效，统一映射到 GitHub 直链 */
private const val FANMINGMING_RAW = "https://raw.githubusercontent.com/fanmingming/live/main"

private fun String.isDeadFanmingmingHost(): Boolean =
    contains("live.fanmingming.com") || contains("live.fanmingming.cn")

private fun String.fixDeadHost(): String =
    replace("https://live.fanmingming.com", FANMINGMING_RAW)
        .replace("https://live.fanmingming.cn", FANMINGMING_RAW)
