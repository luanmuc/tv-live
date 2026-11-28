package com.doubao.tv.settings

import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserveList
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList
import com.doubao.tv.sync.CloudSyncProvider
import com.doubao.tv.ui.screen.Screens
import com.doubao.tv.ui.screen.components.AppThemeDef
import com.doubao.tv.ui.screensold.videoplayer.VideoPlayerDisplayMode
import com.doubao.tv.ui.utils.Configs

/**
 * 全部可持久化设置的不可变快照。
 *
 * SharedPreferences 仍由 Configs 持有；SettingsViewModel 不再为每个字段
 * 维护一份 mutableStateOf，而是统一读这里、写 SettingsRepository。
 */
data class AppSettings(
    val appBootLaunch: Boolean = false,
    val appPipEnable: Boolean = false,
    val appLastLatestVersion: String = "",
    val appAgreementAgreed: Boolean = false,
    val appStartupScreen: String = Screens.Live.name,
    val debugDeveloperMode: Boolean = false,
    val debugShowFps: Boolean = false,
    val debugShowVideoPlayerMetadata: Boolean = false,
    val debugShowLayoutGrids: Boolean = false,
    val cloudSyncAutoPull: Boolean = false,
    val cloudSyncProvider: CloudSyncProvider = CloudSyncProvider.GITHUB_GIST,
    val cloudSyncGithubGistId: String = "",
    val cloudSyncGithubGistToken: String = "",
    val cloudSyncGiteeGistId: String = "",
    val cloudSyncGiteeGistToken: String = "",
    val cloudSyncNetworkUrl: String = "",
    val cloudSyncLocalFilePath: String = "",
    val cloudSyncWebDavUrl: String = "",
    val cloudSyncWebDavUsername: String = "",
    val cloudSyncWebDavPassword: String = "",
    val feiyangAllInOneFilePath: String = "",
    val iptvSourceCacheTime: Long = 0,
    val iptvSourceCurrent: IptvSource = IptvSource(),
    val iptvSourceList: IptvSourceList = IptvSourceList(),
    val iptvChannelGroupHiddenList: Set<String> = emptySet<String>(),
    val iptvHybridMode: Configs.IptvHybridMode = Configs.IptvHybridMode.DISABLE,
    val iptvSimilarChannelMerge: Boolean = false,
    val iptvChannelLogoProvider: String = "",
    val iptvChannelLogoOverride: Boolean = false,
    val iptvChannelFavoriteEnable: Boolean = false,
    val iptvChannelFavoriteListVisible: Boolean = false,
    val iptvChannelFavoriteList: ChannelFavoriteList = ChannelFavoriteList(),
    val iptvChannelLastPlay: Channel = Channel.EMPTY,
    val iptvChannelLinePlayableHostList: Set<String> = emptySet<String>(),
    val iptvChannelLinePlayableUrlList: Set<String> = emptySet<String>(),
    val iptvChannelChangeFlip: Boolean = false,
    val iptvChannelNoSelectEnable: Boolean = false,
    val iptvChannelChangeListLoop: Boolean = false,
    val epgEnable: Boolean = false,
    val epgSourceCurrent: EpgSource = EpgSource(),
    val epgSourceList: EpgSourceList = EpgSourceList(),
    val epgRefreshTimeThreshold: Int = 0,
    val epgSourceFollowIptv: Boolean = false,
    val epgChannelReserveList: EpgProgrammeReserveList = EpgProgrammeReserveList(),
    val uiShowEpgProgrammeProgress: Boolean = false,
    val uiShowEpgProgrammePermanentProgress: Boolean = false,
    val uiShowChannelLogo: Boolean = true,
    val uiShowChannelPreview: Boolean = false,
    val uiUseClassicPanelScreen: Boolean = false,
    val uiDensityScaleRatio: Float = 0f,
    val uiFontScaleRatio: Float = 1f,
    val uiTimeShowMode: Configs.UiTimeShowMode = Configs.UiTimeShowMode.HIDDEN,
    val uiFocusOptimize: Boolean = false,
    val uiScreenAutoCloseDelay: Long = 0,
    val updateForceRemind: Boolean = false,
    val updateChannel: String = "",
    val videoPlayerCore: Configs.VideoPlayerCore = Configs.VideoPlayerCore.MEDIA3,
    val videoPlayerRenderMode: Configs.VideoPlayerRenderMode = Configs.VideoPlayerRenderMode.SURFACE_VIEW,
    val videoPlayerUserAgent: String = "",
    val videoPlayerHeaders: String = "",
    val videoPlayerLoadTimeout: Long = 0,
    val videoPlayerDisplayMode: VideoPlayerDisplayMode = VideoPlayerDisplayMode.ORIGINAL,
    val videoPlayerForceAudioSoftDecode: Boolean = false,
    val videoPlayerStopPreviousMediaItem: Boolean = false,
    val videoPlayerSkipMultipleFramesOnSameVSync: Boolean = false,
    val themeAppCurrent: AppThemeDef? = null,
)
