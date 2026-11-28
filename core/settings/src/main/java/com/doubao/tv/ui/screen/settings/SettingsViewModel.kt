package com.doubao.tv.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserveList
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList
import com.doubao.tv.AppContainer
import com.doubao.tv.settings.SettingsRepository
import com.doubao.tv.sync.CloudSyncProvider
import com.doubao.tv.ui.screen.components.AppThemeDef
import com.doubao.tv.settings.VideoPlayerDisplayMode
import com.doubao.tv.ui.utils.Configs

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = AppContainer.settings,
) : ViewModel() {

    var appBootLaunch: Boolean
        get() = settingsRepository.current.appBootLaunch
        set(value) {
            settingsRepository.update { it.copy(appBootLaunch = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var appPipEnable: Boolean
        get() = settingsRepository.current.appPipEnable
        set(value) {
            settingsRepository.update { it.copy(appPipEnable = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var appLastLatestVersion: String
        get() = settingsRepository.current.appLastLatestVersion
        set(value) {
            settingsRepository.update { it.copy(appLastLatestVersion = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var appAgreementAgreed: Boolean
        get() = settingsRepository.current.appAgreementAgreed
        set(value) {
            settingsRepository.update { it.copy(appAgreementAgreed = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var appStartupScreen: String
        get() = settingsRepository.current.appStartupScreen
        set(value) {
            settingsRepository.update { it.copy(appStartupScreen = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var debugDeveloperMode: Boolean
        get() = settingsRepository.current.debugDeveloperMode
        set(value) {
            settingsRepository.update { it.copy(debugDeveloperMode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var debugShowFps: Boolean
        get() = settingsRepository.current.debugShowFps
        set(value) {
            settingsRepository.update { it.copy(debugShowFps = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var debugShowVideoPlayerMetadata: Boolean
        get() = settingsRepository.current.debugShowVideoPlayerMetadata
        set(value) {
            settingsRepository.update { it.copy(debugShowVideoPlayerMetadata = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var debugShowLayoutGrids: Boolean
        get() = settingsRepository.current.debugShowLayoutGrids
        set(value) {
            settingsRepository.update { it.copy(debugShowLayoutGrids = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvSourceCacheTime: Long
        get() = settingsRepository.current.iptvSourceCacheTime
        set(value) {
            settingsRepository.update { it.copy(iptvSourceCacheTime = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvSourceCurrent: IptvSource
        get() = settingsRepository.current.iptvSourceCurrent
        set(value) {
            settingsRepository.update { it.copy(iptvSourceCurrent = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvSourceList: IptvSourceList
        get() = settingsRepository.current.iptvSourceList
        set(value) {
            settingsRepository.update { it.copy(iptvSourceList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelGroupHiddenList: Set<String>
        get() = settingsRepository.current.iptvChannelGroupHiddenList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelGroupHiddenList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvHybridMode: Configs.IptvHybridMode
        get() = settingsRepository.current.iptvHybridMode
        set(value) {
            settingsRepository.update { it.copy(iptvHybridMode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvSimilarChannelMerge: Boolean
        get() = settingsRepository.current.iptvSimilarChannelMerge
        set(value) {
            settingsRepository.update { it.copy(iptvSimilarChannelMerge = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelLogoProvider: String
        get() = settingsRepository.current.iptvChannelLogoProvider
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLogoProvider = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelLogoOverride: Boolean
        get() = settingsRepository.current.iptvChannelLogoOverride
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLogoOverride = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelFavoriteEnable: Boolean
        get() = settingsRepository.current.iptvChannelFavoriteEnable
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteEnable = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelFavoriteListVisible: Boolean
        get() = settingsRepository.current.iptvChannelFavoriteListVisible
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteListVisible = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelFavoriteList: ChannelFavoriteList
        get() = settingsRepository.current.iptvChannelFavoriteList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelLastPlay: Channel
        get() = settingsRepository.current.iptvChannelLastPlay
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLastPlay = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelLinePlayableHostList: Set<String>
        get() = settingsRepository.current.iptvChannelLinePlayableHostList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLinePlayableHostList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelLinePlayableUrlList: Set<String>
        get() = settingsRepository.current.iptvChannelLinePlayableUrlList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLinePlayableUrlList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelChangeFlip: Boolean
        get() = settingsRepository.current.iptvChannelChangeFlip
        set(value) {
            settingsRepository.update { it.copy(iptvChannelChangeFlip = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelNoSelectEnable: Boolean
        get() = settingsRepository.current.iptvChannelNoSelectEnable
        set(value) {
            settingsRepository.update { it.copy(iptvChannelNoSelectEnable = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var iptvChannelChangeListLoop: Boolean
        get() = settingsRepository.current.iptvChannelChangeListLoop
        set(value) {
            settingsRepository.update { it.copy(iptvChannelChangeListLoop = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgEnable: Boolean
        get() = settingsRepository.current.epgEnable
        set(value) {
            settingsRepository.update { it.copy(epgEnable = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgSourceCurrent: EpgSource
        get() = settingsRepository.current.epgSourceCurrent
        set(value) {
            settingsRepository.update { it.copy(epgSourceCurrent = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgSourceList: EpgSourceList
        get() = settingsRepository.current.epgSourceList
        set(value) {
            settingsRepository.update { it.copy(epgSourceList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgRefreshTimeThreshold: Int
        get() = settingsRepository.current.epgRefreshTimeThreshold
        set(value) {
            settingsRepository.update { it.copy(epgRefreshTimeThreshold = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgSourceFollowIptv: Boolean
        get() = settingsRepository.current.epgSourceFollowIptv
        set(value) {
            settingsRepository.update { it.copy(epgSourceFollowIptv = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var epgChannelReserveList: EpgProgrammeReserveList
        get() = settingsRepository.current.epgChannelReserveList
        set(value) {
            settingsRepository.update { it.copy(epgChannelReserveList = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiShowEpgProgrammeProgress: Boolean
        get() = settingsRepository.current.uiShowEpgProgrammeProgress
        set(value) {
            settingsRepository.update { it.copy(uiShowEpgProgrammeProgress = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiShowEpgProgrammePermanentProgress: Boolean
        get() = settingsRepository.current.uiShowEpgProgrammePermanentProgress
        set(value) {
            settingsRepository.update { it.copy(uiShowEpgProgrammePermanentProgress = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiShowChannelLogo: Boolean
        get() = settingsRepository.current.uiShowChannelLogo
        set(value) {
            settingsRepository.update { it.copy(uiShowChannelLogo = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiShowChannelPreview: Boolean
        get() = settingsRepository.current.uiShowChannelPreview
        set(value) {
            settingsRepository.update { it.copy(uiShowChannelPreview = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiUseClassicPanelScreen: Boolean
        get() = settingsRepository.current.uiUseClassicPanelScreen
        set(value) {
            settingsRepository.update { it.copy(uiUseClassicPanelScreen = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiDensityScaleRatio: Float
        get() = settingsRepository.current.uiDensityScaleRatio
        set(value) {
            settingsRepository.update { it.copy(uiDensityScaleRatio = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiFontScaleRatio: Float
        get() = settingsRepository.current.uiFontScaleRatio
        set(value) {
            settingsRepository.update { it.copy(uiFontScaleRatio = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiTimeShowMode: Configs.UiTimeShowMode
        get() = settingsRepository.current.uiTimeShowMode
        set(value) {
            settingsRepository.update { it.copy(uiTimeShowMode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiFocusOptimize: Boolean
        get() = settingsRepository.current.uiFocusOptimize
        set(value) {
            settingsRepository.update { it.copy(uiFocusOptimize = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var uiScreenAutoCloseDelay: Long
        get() = settingsRepository.current.uiScreenAutoCloseDelay
        set(value) {
            settingsRepository.update { it.copy(uiScreenAutoCloseDelay = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var updateForceRemind: Boolean
        get() = settingsRepository.current.updateForceRemind
        set(value) {
            settingsRepository.update { it.copy(updateForceRemind = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var updateChannel: String
        get() = settingsRepository.current.updateChannel
        set(value) {
            settingsRepository.update { it.copy(updateChannel = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerCore: Configs.VideoPlayerCore
        get() = settingsRepository.current.videoPlayerCore
        set(value) {
            settingsRepository.update { it.copy(videoPlayerCore = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerRenderMode: Configs.VideoPlayerRenderMode
        get() = settingsRepository.current.videoPlayerRenderMode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerRenderMode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerUserAgent: String
        get() = settingsRepository.current.videoPlayerUserAgent
        set(value) {
            settingsRepository.update { it.copy(videoPlayerUserAgent = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerHeaders: String
        get() = settingsRepository.current.videoPlayerHeaders
        set(value) {
            settingsRepository.update { it.copy(videoPlayerHeaders = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerLoadTimeout: Long
        get() = settingsRepository.current.videoPlayerLoadTimeout
        set(value) {
            settingsRepository.update { it.copy(videoPlayerLoadTimeout = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerDisplayMode: VideoPlayerDisplayMode
        get() = settingsRepository.current.videoPlayerDisplayMode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerDisplayMode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerForceAudioSoftDecode: Boolean
        get() = settingsRepository.current.videoPlayerForceAudioSoftDecode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerForceAudioSoftDecode = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerStopPreviousMediaItem: Boolean
        get() = settingsRepository.current.videoPlayerStopPreviousMediaItem
        set(value) {
            settingsRepository.update { it.copy(videoPlayerStopPreviousMediaItem = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var videoPlayerSkipMultipleFramesOnSameVSync: Boolean
        get() = settingsRepository.current.videoPlayerSkipMultipleFramesOnSameVSync
        set(value) {
            settingsRepository.update { it.copy(videoPlayerSkipMultipleFramesOnSameVSync = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var themeAppCurrent: AppThemeDef?
        get() = settingsRepository.current.themeAppCurrent
        set(value) {
            settingsRepository.update { it.copy(themeAppCurrent = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    var cloudSyncAutoPull: Boolean
        get() = settingsRepository.current.cloudSyncAutoPull
        set(value) {
            settingsRepository.update { it.copy(cloudSyncAutoPull = value) }
        }

    var cloudSyncProvider: CloudSyncProvider
        get() = settingsRepository.current.cloudSyncProvider
        set(value) {
            settingsRepository.update { it.copy(cloudSyncProvider = value) }
        }

    var cloudSyncGithubGistId: String
        get() = settingsRepository.current.cloudSyncGithubGistId
        set(value) {
            settingsRepository.update { it.copy(cloudSyncGithubGistId = value) }
        }

    var cloudSyncGithubGistToken: String
        get() = settingsRepository.current.cloudSyncGithubGistToken
        set(value) {
            settingsRepository.update { it.copy(cloudSyncGithubGistToken = value) }
        }

    var cloudSyncGiteeGistId: String
        get() = settingsRepository.current.cloudSyncGiteeGistId
        set(value) {
            settingsRepository.update { it.copy(cloudSyncGiteeGistId = value) }
        }

    var cloudSyncGiteeGistToken: String
        get() = settingsRepository.current.cloudSyncGiteeGistToken
        set(value) {
            settingsRepository.update { it.copy(cloudSyncGiteeGistToken = value) }
        }

    var cloudSyncNetworkUrl: String
        get() = settingsRepository.current.cloudSyncNetworkUrl
        set(value) {
            settingsRepository.update { it.copy(cloudSyncNetworkUrl = value) }
        }

    var cloudSyncLocalFilePath: String
        get() = settingsRepository.current.cloudSyncLocalFilePath
        set(value) {
            settingsRepository.update { it.copy(cloudSyncLocalFilePath = value) }
        }

    var cloudSyncWebDavUrl: String
        get() = settingsRepository.current.cloudSyncWebDavUrl
        set(value) {
            settingsRepository.update { it.copy(cloudSyncWebDavUrl = value) }
        }

    var cloudSyncWebDavUsername: String
        get() = settingsRepository.current.cloudSyncWebDavUsername
        set(value) {
            settingsRepository.update { it.copy(cloudSyncWebDavUsername = value) }
        }

    var cloudSyncWebDavPassword: String
        get() = settingsRepository.current.cloudSyncWebDavPassword
        set(value) {
            settingsRepository.update { it.copy(cloudSyncWebDavPassword = value) }
        }

    var feiyangAllInOneFilePath: String
        get() = settingsRepository.current.feiyangAllInOneFilePath
        set(value) {
            settingsRepository.update { it.copy(feiyangAllInOneFilePath = value) }
            afterSetWhenCloudSyncAutoPull()
        }

    private fun afterSetWhenCloudSyncAutoPull() {
        // if (_cloudSyncAutoPull) Snackbar.show("云同步：自动拉取已启用")
    }

    init {
        runCatching { refresh() }

        // 删除过期的预约
        epgChannelReserveList = EpgProgrammeReserveList(
            epgChannelReserveList.filter {
                System.currentTimeMillis() < it.startAt + 60 * 1000
            }
        )
    }

    fun refresh() {
        settingsRepository.refresh()
    }

}

val settingsVM: SettingsViewModel
    @Composable get() = viewModel<SettingsViewModel>()
