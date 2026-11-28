package com.doubao.tv.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.doubao.tv.ui.utils.Configs

/**
 * 全部设置的单读写入口。
 *
 * SharedPreferences 仍由 Configs 持有，避免一次性迁移全部历史配置；
 * UI 不再维护第二份 mutableStateOf 字段，Compose 直接观察 current。
 */
class SettingsRepository {
    private var _current by mutableStateOf(load())

    val current: AppSettings
        get() = _current

    fun update(transform: (AppSettings) -> AppSettings) {
        val old = _current
        val next = transform(old)
        if (next.appBootLaunch != old.appBootLaunch) Configs.appBootLaunch = next.appBootLaunch
        if (next.appPipEnable != old.appPipEnable) Configs.appPipEnable = next.appPipEnable
        if (next.appLastLatestVersion != old.appLastLatestVersion) Configs.appLastLatestVersion = next.appLastLatestVersion
        if (next.appAgreementAgreed != old.appAgreementAgreed) Configs.appAgreementAgreed = next.appAgreementAgreed
        if (next.appStartupScreen != old.appStartupScreen) Configs.appStartupScreen = next.appStartupScreen
        if (next.debugDeveloperMode != old.debugDeveloperMode) Configs.debugDeveloperMode = next.debugDeveloperMode
        if (next.debugShowFps != old.debugShowFps) Configs.debugShowFps = next.debugShowFps
        if (next.debugShowVideoPlayerMetadata != old.debugShowVideoPlayerMetadata) Configs.debugShowVideoPlayerMetadata = next.debugShowVideoPlayerMetadata
        if (next.debugShowLayoutGrids != old.debugShowLayoutGrids) Configs.debugShowLayoutGrids = next.debugShowLayoutGrids
        if (next.cloudSyncAutoPull != old.cloudSyncAutoPull) Configs.cloudSyncAutoPull = next.cloudSyncAutoPull
        if (next.cloudSyncProvider != old.cloudSyncProvider) Configs.cloudSyncProvider = next.cloudSyncProvider
        if (next.cloudSyncGithubGistId != old.cloudSyncGithubGistId) Configs.cloudSyncGithubGistId = next.cloudSyncGithubGistId
        if (next.cloudSyncGithubGistToken != old.cloudSyncGithubGistToken) Configs.cloudSyncGithubGistToken = next.cloudSyncGithubGistToken
        if (next.cloudSyncGiteeGistId != old.cloudSyncGiteeGistId) Configs.cloudSyncGiteeGistId = next.cloudSyncGiteeGistId
        if (next.cloudSyncGiteeGistToken != old.cloudSyncGiteeGistToken) Configs.cloudSyncGiteeGistToken = next.cloudSyncGiteeGistToken
        if (next.cloudSyncNetworkUrl != old.cloudSyncNetworkUrl) Configs.cloudSyncNetworkUrl = next.cloudSyncNetworkUrl
        if (next.cloudSyncLocalFilePath != old.cloudSyncLocalFilePath) Configs.cloudSyncLocalFilePath = next.cloudSyncLocalFilePath
        if (next.cloudSyncWebDavUrl != old.cloudSyncWebDavUrl) Configs.cloudSyncWebDavUrl = next.cloudSyncWebDavUrl
        if (next.cloudSyncWebDavUsername != old.cloudSyncWebDavUsername) Configs.cloudSyncWebDavUsername = next.cloudSyncWebDavUsername
        if (next.cloudSyncWebDavPassword != old.cloudSyncWebDavPassword) Configs.cloudSyncWebDavPassword = next.cloudSyncWebDavPassword
        if (next.feiyangAllInOneFilePath != old.feiyangAllInOneFilePath) Configs.feiyangAllInOneFilePath = next.feiyangAllInOneFilePath
        if (next.iptvSourceCacheTime != old.iptvSourceCacheTime) Configs.iptvSourceCacheTime = next.iptvSourceCacheTime
        if (next.iptvSourceCurrent != old.iptvSourceCurrent) Configs.iptvSourceCurrent = next.iptvSourceCurrent
        if (next.iptvSourceList != old.iptvSourceList) Configs.iptvSourceList = next.iptvSourceList
        if (next.iptvChannelGroupHiddenList != old.iptvChannelGroupHiddenList) Configs.iptvChannelGroupHiddenList = next.iptvChannelGroupHiddenList
        if (next.iptvHybridMode != old.iptvHybridMode) Configs.iptvHybridMode = next.iptvHybridMode
        if (next.iptvSimilarChannelMerge != old.iptvSimilarChannelMerge) Configs.iptvSimilarChannelMerge = next.iptvSimilarChannelMerge
        if (next.iptvChannelLogoProvider != old.iptvChannelLogoProvider) Configs.iptvChannelLogoProvider = next.iptvChannelLogoProvider
        if (next.iptvChannelLogoOverride != old.iptvChannelLogoOverride) Configs.iptvChannelLogoOverride = next.iptvChannelLogoOverride
        if (next.iptvChannelFavoriteEnable != old.iptvChannelFavoriteEnable) Configs.iptvChannelFavoriteEnable = next.iptvChannelFavoriteEnable
        if (next.iptvChannelFavoriteListVisible != old.iptvChannelFavoriteListVisible) Configs.iptvChannelFavoriteListVisible = next.iptvChannelFavoriteListVisible
        if (next.iptvChannelFavoriteList != old.iptvChannelFavoriteList) Configs.iptvChannelFavoriteList = next.iptvChannelFavoriteList
        if (next.iptvChannelLastPlay != old.iptvChannelLastPlay) Configs.iptvChannelLastPlay = next.iptvChannelLastPlay
        if (next.iptvChannelLinePlayableHostList != old.iptvChannelLinePlayableHostList) Configs.iptvChannelLinePlayableHostList = next.iptvChannelLinePlayableHostList
        if (next.iptvChannelLinePlayableUrlList != old.iptvChannelLinePlayableUrlList) Configs.iptvChannelLinePlayableUrlList = next.iptvChannelLinePlayableUrlList
        if (next.iptvChannelChangeFlip != old.iptvChannelChangeFlip) Configs.iptvChannelChangeFlip = next.iptvChannelChangeFlip
        if (next.iptvChannelNoSelectEnable != old.iptvChannelNoSelectEnable) Configs.iptvChannelNoSelectEnable = next.iptvChannelNoSelectEnable
        if (next.iptvChannelChangeListLoop != old.iptvChannelChangeListLoop) Configs.iptvChannelChangeListLoop = next.iptvChannelChangeListLoop
        if (next.epgEnable != old.epgEnable) Configs.epgEnable = next.epgEnable
        if (next.epgSourceCurrent != old.epgSourceCurrent) Configs.epgSourceCurrent = next.epgSourceCurrent
        if (next.epgSourceList != old.epgSourceList) Configs.epgSourceList = next.epgSourceList
        if (next.epgRefreshTimeThreshold != old.epgRefreshTimeThreshold) Configs.epgRefreshTimeThreshold = next.epgRefreshTimeThreshold
        if (next.epgSourceFollowIptv != old.epgSourceFollowIptv) Configs.epgSourceFollowIptv = next.epgSourceFollowIptv
        if (next.epgChannelReserveList != old.epgChannelReserveList) Configs.epgChannelReserveList = next.epgChannelReserveList
        if (next.uiShowEpgProgrammeProgress != old.uiShowEpgProgrammeProgress) Configs.uiShowEpgProgrammeProgress = next.uiShowEpgProgrammeProgress
        if (next.uiShowEpgProgrammePermanentProgress != old.uiShowEpgProgrammePermanentProgress) Configs.uiShowEpgProgrammePermanentProgress = next.uiShowEpgProgrammePermanentProgress
        if (next.uiShowChannelLogo != old.uiShowChannelLogo) Configs.uiShowChannelLogo = next.uiShowChannelLogo
        if (next.uiShowChannelPreview != old.uiShowChannelPreview) Configs.uiShowChannelPreview = next.uiShowChannelPreview
        if (next.uiUseClassicPanelScreen != old.uiUseClassicPanelScreen) Configs.uiUseClassicPanelScreen = next.uiUseClassicPanelScreen
        if (next.uiDensityScaleRatio != old.uiDensityScaleRatio) Configs.uiDensityScaleRatio = next.uiDensityScaleRatio
        if (next.uiFontScaleRatio != old.uiFontScaleRatio) Configs.uiFontScaleRatio = next.uiFontScaleRatio
        if (next.uiTimeShowMode != old.uiTimeShowMode) Configs.uiTimeShowMode = next.uiTimeShowMode
        if (next.uiFocusOptimize != old.uiFocusOptimize) Configs.uiFocusOptimize = next.uiFocusOptimize
        if (next.uiScreenAutoCloseDelay != old.uiScreenAutoCloseDelay) Configs.uiScreenAutoCloseDelay = next.uiScreenAutoCloseDelay
        if (next.updateForceRemind != old.updateForceRemind) Configs.updateForceRemind = next.updateForceRemind
        if (next.updateChannel != old.updateChannel) Configs.updateChannel = next.updateChannel
        if (next.videoPlayerCore != old.videoPlayerCore) Configs.videoPlayerCore = next.videoPlayerCore
        if (next.videoPlayerRenderMode != old.videoPlayerRenderMode) Configs.videoPlayerRenderMode = next.videoPlayerRenderMode
        if (next.videoPlayerUserAgent != old.videoPlayerUserAgent) Configs.videoPlayerUserAgent = next.videoPlayerUserAgent
        if (next.videoPlayerHeaders != old.videoPlayerHeaders) Configs.videoPlayerHeaders = next.videoPlayerHeaders
        if (next.videoPlayerLoadTimeout != old.videoPlayerLoadTimeout) Configs.videoPlayerLoadTimeout = next.videoPlayerLoadTimeout
        if (next.videoPlayerDisplayMode != old.videoPlayerDisplayMode) Configs.videoPlayerDisplayMode = next.videoPlayerDisplayMode
        if (next.videoPlayerForceAudioSoftDecode != old.videoPlayerForceAudioSoftDecode) Configs.videoPlayerForceAudioSoftDecode = next.videoPlayerForceAudioSoftDecode
        if (next.videoPlayerStopPreviousMediaItem != old.videoPlayerStopPreviousMediaItem) Configs.videoPlayerStopPreviousMediaItem = next.videoPlayerStopPreviousMediaItem
        if (next.videoPlayerSkipMultipleFramesOnSameVSync != old.videoPlayerSkipMultipleFramesOnSameVSync) Configs.videoPlayerSkipMultipleFramesOnSameVSync = next.videoPlayerSkipMultipleFramesOnSameVSync
        if (next.themeAppCurrent != old.themeAppCurrent) Configs.themeAppCurrent = next.themeAppCurrent
        _current = next
    }

    fun refresh() {
        _current = load()
    }

    private fun load() = AppSettings(
        appBootLaunch = Configs.appBootLaunch,
        appPipEnable = Configs.appPipEnable,
        appLastLatestVersion = Configs.appLastLatestVersion,
        appAgreementAgreed = Configs.appAgreementAgreed,
        appStartupScreen = Configs.appStartupScreen,
        debugDeveloperMode = Configs.debugDeveloperMode,
        debugShowFps = Configs.debugShowFps,
        debugShowVideoPlayerMetadata = Configs.debugShowVideoPlayerMetadata,
        debugShowLayoutGrids = Configs.debugShowLayoutGrids,
        cloudSyncAutoPull = Configs.cloudSyncAutoPull,
        cloudSyncProvider = Configs.cloudSyncProvider,
        cloudSyncGithubGistId = Configs.cloudSyncGithubGistId,
        cloudSyncGithubGistToken = Configs.cloudSyncGithubGistToken,
        cloudSyncGiteeGistId = Configs.cloudSyncGiteeGistId,
        cloudSyncGiteeGistToken = Configs.cloudSyncGiteeGistToken,
        cloudSyncNetworkUrl = Configs.cloudSyncNetworkUrl,
        cloudSyncLocalFilePath = Configs.cloudSyncLocalFilePath,
        cloudSyncWebDavUrl = Configs.cloudSyncWebDavUrl,
        cloudSyncWebDavUsername = Configs.cloudSyncWebDavUsername,
        cloudSyncWebDavPassword = Configs.cloudSyncWebDavPassword,
        feiyangAllInOneFilePath = Configs.feiyangAllInOneFilePath,
        iptvSourceCacheTime = Configs.iptvSourceCacheTime,
        iptvSourceCurrent = Configs.iptvSourceCurrent,
        iptvSourceList = Configs.iptvSourceList,
        iptvChannelGroupHiddenList = Configs.iptvChannelGroupHiddenList,
        iptvHybridMode = Configs.iptvHybridMode,
        iptvSimilarChannelMerge = Configs.iptvSimilarChannelMerge,
        iptvChannelLogoProvider = Configs.iptvChannelLogoProvider,
        iptvChannelLogoOverride = Configs.iptvChannelLogoOverride,
        iptvChannelFavoriteEnable = Configs.iptvChannelFavoriteEnable,
        iptvChannelFavoriteListVisible = Configs.iptvChannelFavoriteListVisible,
        iptvChannelFavoriteList = Configs.iptvChannelFavoriteList,
        iptvChannelLastPlay = Configs.iptvChannelLastPlay,
        iptvChannelLinePlayableHostList = Configs.iptvChannelLinePlayableHostList,
        iptvChannelLinePlayableUrlList = Configs.iptvChannelLinePlayableUrlList,
        iptvChannelChangeFlip = Configs.iptvChannelChangeFlip,
        iptvChannelNoSelectEnable = Configs.iptvChannelNoSelectEnable,
        iptvChannelChangeListLoop = Configs.iptvChannelChangeListLoop,
        epgEnable = Configs.epgEnable,
        epgSourceCurrent = Configs.epgSourceCurrent,
        epgSourceList = Configs.epgSourceList,
        epgRefreshTimeThreshold = Configs.epgRefreshTimeThreshold,
        epgSourceFollowIptv = Configs.epgSourceFollowIptv,
        epgChannelReserveList = Configs.epgChannelReserveList,
        uiShowEpgProgrammeProgress = Configs.uiShowEpgProgrammeProgress,
        uiShowEpgProgrammePermanentProgress = Configs.uiShowEpgProgrammePermanentProgress,
        uiShowChannelLogo = Configs.uiShowChannelLogo,
        uiShowChannelPreview = Configs.uiShowChannelPreview,
        uiUseClassicPanelScreen = Configs.uiUseClassicPanelScreen,
        uiDensityScaleRatio = Configs.uiDensityScaleRatio,
        uiFontScaleRatio = Configs.uiFontScaleRatio,
        uiTimeShowMode = Configs.uiTimeShowMode,
        uiFocusOptimize = Configs.uiFocusOptimize,
        uiScreenAutoCloseDelay = Configs.uiScreenAutoCloseDelay,
        updateForceRemind = Configs.updateForceRemind,
        updateChannel = Configs.updateChannel,
        videoPlayerCore = Configs.videoPlayerCore,
        videoPlayerRenderMode = Configs.videoPlayerRenderMode,
        videoPlayerUserAgent = Configs.videoPlayerUserAgent,
        videoPlayerHeaders = Configs.videoPlayerHeaders,
        videoPlayerLoadTimeout = Configs.videoPlayerLoadTimeout,
        videoPlayerDisplayMode = Configs.videoPlayerDisplayMode,
        videoPlayerForceAudioSoftDecode = Configs.videoPlayerForceAudioSoftDecode,
        videoPlayerStopPreviousMediaItem = Configs.videoPlayerStopPreviousMediaItem,
        videoPlayerSkipMultipleFramesOnSameVSync = Configs.videoPlayerSkipMultipleFramesOnSameVSync,
        themeAppCurrent = Configs.themeAppCurrent,
    )
}
