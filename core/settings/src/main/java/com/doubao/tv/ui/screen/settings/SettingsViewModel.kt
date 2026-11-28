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
        }

    var appPipEnable: Boolean
        get() = settingsRepository.current.appPipEnable
        set(value) {
            settingsRepository.update { it.copy(appPipEnable = value) }
        }

    var appLastLatestVersion: String
        get() = settingsRepository.current.appLastLatestVersion
        set(value) {
            settingsRepository.update { it.copy(appLastLatestVersion = value) }
        }

    var appAgreementAgreed: Boolean
        get() = settingsRepository.current.appAgreementAgreed
        set(value) {
            settingsRepository.update { it.copy(appAgreementAgreed = value) }
        }

    var appStartupScreen: String
        get() = settingsRepository.current.appStartupScreen
        set(value) {
            settingsRepository.update { it.copy(appStartupScreen = value) }
        }

    var iptvSourceCacheTime: Long
        get() = settingsRepository.current.iptvSourceCacheTime
        set(value) {
            settingsRepository.update { it.copy(iptvSourceCacheTime = value) }
        }

    var iptvSourceCurrent: IptvSource
        get() = settingsRepository.current.iptvSourceCurrent
        set(value) {
            settingsRepository.update { it.copy(iptvSourceCurrent = value) }
        }

    var iptvSourceList: IptvSourceList
        get() = settingsRepository.current.iptvSourceList
        set(value) {
            settingsRepository.update { it.copy(iptvSourceList = value) }
        }

    var iptvChannelGroupHiddenList: Set<String>
        get() = settingsRepository.current.iptvChannelGroupHiddenList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelGroupHiddenList = value) }
        }

    var iptvHybridMode: Configs.IptvHybridMode
        get() = settingsRepository.current.iptvHybridMode
        set(value) {
            settingsRepository.update { it.copy(iptvHybridMode = value) }
        }

    var iptvSimilarChannelMerge: Boolean
        get() = settingsRepository.current.iptvSimilarChannelMerge
        set(value) {
            settingsRepository.update { it.copy(iptvSimilarChannelMerge = value) }
        }

    var iptvChannelLogoProvider: String
        get() = settingsRepository.current.iptvChannelLogoProvider
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLogoProvider = value) }
        }

    var iptvChannelLogoOverride: Boolean
        get() = settingsRepository.current.iptvChannelLogoOverride
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLogoOverride = value) }
        }

    var iptvChannelFavoriteEnable: Boolean
        get() = settingsRepository.current.iptvChannelFavoriteEnable
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteEnable = value) }
        }

    var iptvChannelFavoriteListVisible: Boolean
        get() = settingsRepository.current.iptvChannelFavoriteListVisible
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteListVisible = value) }
        }

    var iptvChannelFavoriteList: ChannelFavoriteList
        get() = settingsRepository.current.iptvChannelFavoriteList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelFavoriteList = value) }
        }

    var iptvChannelLastPlay: Channel
        get() = settingsRepository.current.iptvChannelLastPlay
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLastPlay = value) }
        }

    var iptvChannelLinePlayableHostList: Set<String>
        get() = settingsRepository.current.iptvChannelLinePlayableHostList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLinePlayableHostList = value) }
        }

    var iptvChannelLinePlayableUrlList: Set<String>
        get() = settingsRepository.current.iptvChannelLinePlayableUrlList
        set(value) {
            settingsRepository.update { it.copy(iptvChannelLinePlayableUrlList = value) }
        }

    var iptvChannelChangeFlip: Boolean
        get() = settingsRepository.current.iptvChannelChangeFlip
        set(value) {
            settingsRepository.update { it.copy(iptvChannelChangeFlip = value) }
        }

    var iptvChannelNoSelectEnable: Boolean
        get() = settingsRepository.current.iptvChannelNoSelectEnable
        set(value) {
            settingsRepository.update { it.copy(iptvChannelNoSelectEnable = value) }
        }

    var iptvChannelChangeListLoop: Boolean
        get() = settingsRepository.current.iptvChannelChangeListLoop
        set(value) {
            settingsRepository.update { it.copy(iptvChannelChangeListLoop = value) }
        }

    var epgEnable: Boolean
        get() = settingsRepository.current.epgEnable
        set(value) {
            settingsRepository.update { it.copy(epgEnable = value) }
        }

    var epgSourceCurrent: EpgSource
        get() = settingsRepository.current.epgSourceCurrent
        set(value) {
            settingsRepository.update { it.copy(epgSourceCurrent = value) }
        }

    var epgSourceList: EpgSourceList
        get() = settingsRepository.current.epgSourceList
        set(value) {
            settingsRepository.update { it.copy(epgSourceList = value) }
        }

    var epgRefreshTimeThreshold: Int
        get() = settingsRepository.current.epgRefreshTimeThreshold
        set(value) {
            settingsRepository.update { it.copy(epgRefreshTimeThreshold = value) }
        }

    var epgSourceFollowIptv: Boolean
        get() = settingsRepository.current.epgSourceFollowIptv
        set(value) {
            settingsRepository.update { it.copy(epgSourceFollowIptv = value) }
        }

    var epgChannelReserveList: EpgProgrammeReserveList
        get() = settingsRepository.current.epgChannelReserveList
        set(value) {
            settingsRepository.update { it.copy(epgChannelReserveList = value) }
        }

    var uiShowEpgProgrammeProgress: Boolean
        get() = settingsRepository.current.uiShowEpgProgrammeProgress
        set(value) {
            settingsRepository.update { it.copy(uiShowEpgProgrammeProgress = value) }
        }

    var uiShowEpgProgrammePermanentProgress: Boolean
        get() = settingsRepository.current.uiShowEpgProgrammePermanentProgress
        set(value) {
            settingsRepository.update { it.copy(uiShowEpgProgrammePermanentProgress = value) }
        }

    var uiShowChannelLogo: Boolean
        get() = settingsRepository.current.uiShowChannelLogo
        set(value) {
            settingsRepository.update { it.copy(uiShowChannelLogo = value) }
        }

    var uiShowChannelPreview: Boolean
        get() = settingsRepository.current.uiShowChannelPreview
        set(value) {
            settingsRepository.update { it.copy(uiShowChannelPreview = value) }
        }

    var uiUseClassicPanelScreen: Boolean
        get() = settingsRepository.current.uiUseClassicPanelScreen
        set(value) {
            settingsRepository.update { it.copy(uiUseClassicPanelScreen = value) }
        }

    var uiDensityScaleRatio: Float
        get() = settingsRepository.current.uiDensityScaleRatio
        set(value) {
            settingsRepository.update { it.copy(uiDensityScaleRatio = value) }
        }

    var uiFontScaleRatio: Float
        get() = settingsRepository.current.uiFontScaleRatio
        set(value) {
            settingsRepository.update { it.copy(uiFontScaleRatio = value) }
        }

    var uiTimeShowMode: Configs.UiTimeShowMode
        get() = settingsRepository.current.uiTimeShowMode
        set(value) {
            settingsRepository.update { it.copy(uiTimeShowMode = value) }
        }

    var uiFocusOptimize: Boolean
        get() = settingsRepository.current.uiFocusOptimize
        set(value) {
            settingsRepository.update { it.copy(uiFocusOptimize = value) }
        }

    var uiScreenAutoCloseDelay: Long
        get() = settingsRepository.current.uiScreenAutoCloseDelay
        set(value) {
            settingsRepository.update { it.copy(uiScreenAutoCloseDelay = value) }
        }

    var updateForceRemind: Boolean
        get() = settingsRepository.current.updateForceRemind
        set(value) {
            settingsRepository.update { it.copy(updateForceRemind = value) }
        }

    var updateChannel: String
        get() = settingsRepository.current.updateChannel
        set(value) {
            settingsRepository.update { it.copy(updateChannel = value) }
        }

    var videoPlayerCore: Configs.VideoPlayerCore
        get() = settingsRepository.current.videoPlayerCore
        set(value) {
            settingsRepository.update { it.copy(videoPlayerCore = value) }
        }

    var videoPlayerRenderMode: Configs.VideoPlayerRenderMode
        get() = settingsRepository.current.videoPlayerRenderMode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerRenderMode = value) }
        }

    var videoPlayerUserAgent: String
        get() = settingsRepository.current.videoPlayerUserAgent
        set(value) {
            settingsRepository.update { it.copy(videoPlayerUserAgent = value) }
        }

    var videoPlayerHeaders: String
        get() = settingsRepository.current.videoPlayerHeaders
        set(value) {
            settingsRepository.update { it.copy(videoPlayerHeaders = value) }
        }

    var videoPlayerLoadTimeout: Long
        get() = settingsRepository.current.videoPlayerLoadTimeout
        set(value) {
            settingsRepository.update { it.copy(videoPlayerLoadTimeout = value) }
        }

    var videoPlayerDisplayMode: VideoPlayerDisplayMode
        get() = settingsRepository.current.videoPlayerDisplayMode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerDisplayMode = value) }
        }

    var videoPlayerForceAudioSoftDecode: Boolean
        get() = settingsRepository.current.videoPlayerForceAudioSoftDecode
        set(value) {
            settingsRepository.update { it.copy(videoPlayerForceAudioSoftDecode = value) }
        }

    var videoPlayerStopPreviousMediaItem: Boolean
        get() = settingsRepository.current.videoPlayerStopPreviousMediaItem
        set(value) {
            settingsRepository.update { it.copy(videoPlayerStopPreviousMediaItem = value) }
        }

    var videoPlayerSkipMultipleFramesOnSameVSync: Boolean
        get() = settingsRepository.current.videoPlayerSkipMultipleFramesOnSameVSync
        set(value) {
            settingsRepository.update { it.copy(videoPlayerSkipMultipleFramesOnSameVSync = value) }
        }

    var themeAppCurrent: AppThemeDef?
        get() = settingsRepository.current.themeAppCurrent
        set(value) {
            settingsRepository.update { it.copy(themeAppCurrent = value) }
        }

    var feiyangAllInOneFilePath: String
        get() = settingsRepository.current.feiyangAllInOneFilePath
        set(value) {
            settingsRepository.update { it.copy(feiyangAllInOneFilePath = value) }
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
