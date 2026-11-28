package com.doubao.tv.ui.screen.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 设置页内部路由。
 *
 * 每个分类/子分类都是独立目的地，@SerialName 与旧字符串路由保持一致，
 * 便于旧返回栈恢复；新增页面时只需加一个 object 和映射。
 */
@Serializable
sealed interface SettingsDestination {
    @Serializable
    @SerialName("categories")
    data object Categories : SettingsDestination

    @Serializable
    @SerialName("APP")
    data object App : SettingsDestination

    @Serializable
    @SerialName("IPTV")
    data object Iptv : SettingsDestination

    @Serializable
    @SerialName("EPG")
    data object Epg : SettingsDestination

    @Serializable
    @SerialName("UI")
    data object Ui : SettingsDestination

    @Serializable
    @SerialName("CONTROL")
    data object Control : SettingsDestination

    @Serializable
    @SerialName("VIDEO_PLAYER")
    data object VideoPlayer : SettingsDestination

    @Serializable
    @SerialName("UPDATE")
    data object Update : SettingsDestination

    @Serializable
    @SerialName("NETWORK")
    data object Network : SettingsDestination

    @Serializable
    @SerialName("THEME")
    data object Theme : SettingsDestination

    @Serializable
    @SerialName("LOG")
    data object Log : SettingsDestination

    @Serializable
    @SerialName("PERMISSIONS")
    data object Permissions : SettingsDestination

    @Serializable
    @SerialName("IPTV_SOURCE")
    data object IptvSource : SettingsDestination

    @Serializable
    @SerialName("IPTV_SOURCE_CACHE_TIME")
    data object IptvSourceCacheTime : SettingsDestination

    @Serializable
    @SerialName("CHANNEL_GROUP_VISIBILITY")
    data object ChannelGroupVisibility : SettingsDestination

    @Serializable
    @SerialName("IPTV_HYBRID_MODE")
    data object IptvHybridMode : SettingsDestination

    @Serializable
    @SerialName("EPG_SOURCE")
    data object EpgSource : SettingsDestination

    @Serializable
    @SerialName("EPG_REFRESH_TIME_THRESHOLD")
    data object EpgRefreshTimeThreshold : SettingsDestination

    @Serializable
    @SerialName("UI_TIME_SHOW_MODE")
    data object UiTimeShowMode : SettingsDestination

    @Serializable
    @SerialName("UI_SCREEN_AUTO_CLOSE_DELAY")
    data object UiScreenAutoCloseDelay : SettingsDestination

    @Serializable
    @SerialName("UI_DENSITY_SCALE_RATIO")
    data object UiDensityScaleRatio : SettingsDestination

    @Serializable
    @SerialName("UI_FONT_SCALE_RATIO")
    data object UiFontScaleRatio : SettingsDestination

    @Serializable
    @SerialName("VIDEO_PLAYER_CORE")
    data object VideoPlayerCore : SettingsDestination

    @Serializable
    @SerialName("VIDEO_PLAYER_RENDER_MODE")
    data object VideoPlayerRenderMode : SettingsDestination

    @Serializable
    @SerialName("VIDEO_PLAYER_DISPLAY_MODE")
    data object VideoPlayerDisplayMode : SettingsDestination

    @Serializable
    @SerialName("VIDEO_PLAYER_LOAD_TIMEOUT")
    data object VideoPlayerLoadTimeout : SettingsDestination

    @Serializable
    @SerialName("UPDATE_CHANNEL")
    data object UpdateChannel : SettingsDestination

    companion object {
        fun fromStoredName(name: String?): SettingsDestination =
            when (name?.substringBefore('/')) {
                null, "", "categories" -> Categories
                "APP" -> App
                "IPTV" -> Iptv
                "EPG" -> Epg
                "UI" -> Ui
                "CONTROL" -> Control
                "VIDEO_PLAYER" -> VideoPlayer
                "UPDATE" -> Update
                "NETWORK" -> Network
                "THEME" -> Theme
                "LOG" -> Log
                "PERMISSIONS" -> Permissions
                "IPTV_SOURCE" -> IptvSource
                "IPTV_SOURCE_CACHE_TIME" -> IptvSourceCacheTime
                "CHANNEL_GROUP_VISIBILITY" -> ChannelGroupVisibility
                "IPTV_HYBRID_MODE" -> IptvHybridMode
                "EPG_SOURCE" -> EpgSource
                "EPG_REFRESH_TIME_THRESHOLD" -> EpgRefreshTimeThreshold
                "UI_TIME_SHOW_MODE" -> UiTimeShowMode
                "UI_SCREEN_AUTO_CLOSE_DELAY" -> UiScreenAutoCloseDelay
                "UI_DENSITY_SCALE_RATIO" -> UiDensityScaleRatio
                "UI_FONT_SCALE_RATIO" -> UiFontScaleRatio
                "VIDEO_PLAYER_CORE" -> VideoPlayerCore
                "VIDEO_PLAYER_RENDER_MODE" -> VideoPlayerRenderMode
                "VIDEO_PLAYER_DISPLAY_MODE" -> VideoPlayerDisplayMode
                "VIDEO_PLAYER_LOAD_TIMEOUT" -> VideoPlayerLoadTimeout
                "UPDATE_CHANNEL" -> UpdateChannel
                else -> Categories
            }

        fun fromCategory(category: SettingsCategories): SettingsDestination =
            when (category) {
                SettingsCategories.APP -> App
                SettingsCategories.IPTV -> Iptv
                SettingsCategories.EPG -> Epg
                SettingsCategories.UI -> Ui
                SettingsCategories.CONTROL -> Control
                SettingsCategories.VIDEO_PLAYER -> VideoPlayer
                SettingsCategories.UPDATE -> Update
                SettingsCategories.NETWORK -> Network
                SettingsCategories.THEME -> Theme
                SettingsCategories.LOG -> Log
                SettingsCategories.PERMISSIONS -> Permissions
            }

        fun fromSubCategory(category: SettingsSubCategories): SettingsDestination =
            when (category) {
                SettingsSubCategories.IPTV_SOURCE -> IptvSource
                SettingsSubCategories.IPTV_SOURCE_CACHE_TIME -> IptvSourceCacheTime
                SettingsSubCategories.CHANNEL_GROUP_VISIBILITY -> ChannelGroupVisibility
                SettingsSubCategories.IPTV_HYBRID_MODE -> IptvHybridMode
                SettingsSubCategories.EPG_SOURCE -> EpgSource
                SettingsSubCategories.EPG_REFRESH_TIME_THRESHOLD -> EpgRefreshTimeThreshold
                SettingsSubCategories.UI_TIME_SHOW_MODE -> UiTimeShowMode
                SettingsSubCategories.UI_SCREEN_AUTO_CLOSE_DELAY -> UiScreenAutoCloseDelay
                SettingsSubCategories.UI_DENSITY_SCALE_RATIO -> UiDensityScaleRatio
                SettingsSubCategories.UI_FONT_SCALE_RATIO -> UiFontScaleRatio
                SettingsSubCategories.VIDEO_PLAYER_CORE -> VideoPlayerCore
                SettingsSubCategories.VIDEO_PLAYER_RENDER_MODE -> VideoPlayerRenderMode
                SettingsSubCategories.VIDEO_PLAYER_DISPLAY_MODE -> VideoPlayerDisplayMode
                SettingsSubCategories.VIDEO_PLAYER_LOAD_TIMEOUT -> VideoPlayerLoadTimeout
                SettingsSubCategories.UPDATE_CHANNEL -> UpdateChannel
            }
    }
}
