package com.doubao.tv.ui.screensold.main.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelList
import com.doubao.tv.core.data.entities.channel.ChannelLine
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.Epg
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.core.data.entities.epg.EpgList.Companion.match
import com.doubao.tv.core.data.entities.epg.EpgList.Companion.recentProgramme
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserveList
import com.doubao.tv.core.data.repositories.epg.EpgRepository
import com.doubao.tv.core.data.repositories.iptv.IptvRepository
import com.doubao.tv.core.data.utils.LineHealth
import com.doubao.tv.ui.material.PopupContent
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.material.Visibility
import com.doubao.tv.ui.material.popupable
import com.doubao.tv.ui.screen.settings.SettingsSubCategories
import com.doubao.tv.ui.screen.settings.SettingsViewModel
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.screensold.audiotracks.AudioTracksScreen
import com.doubao.tv.ui.screensold.channel.ChannelNumberSelectScreen
import com.doubao.tv.ui.screensold.channel.ChannelScreen
import com.doubao.tv.ui.screensold.channel.ChannelTempScreen
import com.doubao.tv.ui.screensold.channel.rememberChannelNumberSelectState
import com.doubao.tv.ui.screensold.channelline.ChannelLineScreen
import com.doubao.tv.ui.screensold.classicchannel.ClassicChannelScreen
import com.doubao.tv.ui.screensold.datetime.DatetimeScreen
import com.doubao.tv.ui.screensold.epg.EpgProgrammeProgressScreen
import com.doubao.tv.ui.screensold.epg.EpgScreen
import com.doubao.tv.ui.screensold.epgreverse.EpgReverseScreen
import com.doubao.tv.ui.screensold.quickop.QuickOpScreen
import com.doubao.tv.ui.screensold.subtitletracks.SubtitleTracksScreen
import com.doubao.tv.ui.screensold.videoplayer.VideoPlayerScreen
import com.doubao.tv.ui.screensold.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.screensold.videoplayer.rememberVideoPlayerState
import com.doubao.tv.ui.screensold.videoplayercontroller.VideoPlayerControllerScreen
import com.doubao.tv.ui.screensold.videoplayerdiaplaymode.VideoPlayerDisplayModeScreen
import com.doubao.tv.ui.screensold.videotracks.VideoTracksScreen
import com.doubao.tv.ui.screensold.webview.WebViewScreen
import com.doubao.tv.ui.utils.backHandler
import com.doubao.tv.ui.utils.handleDragGestures
import com.doubao.tv.ui.utils.handleKeyEvents

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    filteredChannelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    favoriteChannelListProvider: () -> ChannelList = { ChannelList() },
    epgListProvider: () -> EpgList = { EpgList() },
    settingsViewModel: SettingsViewModel = settingsVM,
    onChannelFavoriteToggle: (Channel) -> Unit = {},
    toSettingsScreen: (SettingsSubCategories?) -> Unit = {},
    toDashboardScreen: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    val videoPlayerState =
        rememberVideoPlayerState(defaultDisplayModeProvider = { settingsViewModel.videoPlayerDisplayMode })
    val mainContentState = rememberMainContentState(
        videoPlayerState = videoPlayerState,
        channelGroupListProvider = filteredChannelGroupListProvider,
        favoriteChannelListProvider = favoriteChannelListProvider,
    )

    // 换源后重启后台检测；离开直播页时停止，避免后台空跑流量
    LaunchedEffect(settingsViewModel.iptvSourceCurrent.url) {
        mainContentState.onChannelGroupListChanged(filteredChannelGroupListProvider())
    }
    DisposableEffect(Unit) {
        onDispose { LineHealth.stop() }
    }

    // 播放失败自动换线：同频道依次试下一条线路，全部失败则停在错误提示
    LaunchedEffect(videoPlayerState.errorVersion, videoPlayerState.error) {
        if (videoPlayerState.error != null) {
            mainContentState.handlePlayerError()
        }
    }
    val channelNumberSelectState = rememberChannelNumberSelectState {
        val idx = it.toInt() - 1
        filteredChannelGroupListProvider().channelList.getOrNull(idx)?.let { channel ->
            mainContentState.changeCurrentChannel(channel)
        }
    }

    Box(
        modifier = modifier
            .popupable()
            .backHandler { onBackPressed() }
            .handleKeyEvents(
                onUp = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToNext()
                    else mainContentState.changeCurrentChannelToPrev()
                },
                onDown = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToPrev()
                    else mainContentState.changeCurrentChannelToNext()
                },
                onLeft = {
                    if (mainContentState.currentChannel.lineList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelLineIdx - 1,
                        )
                    }
                },
                onRight = {
                    if (mainContentState.currentChannel.lineList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelLineIdx + 1,
                        )
                    }
                },
                onSelect = { mainContentState.isChannelScreenVisible = true },
                onLongSelect = { mainContentState.isQuickOpScreenVisible = true },
                onSettings = { mainContentState.isQuickOpScreenVisible = true },
                onLongLeft = { mainContentState.isEpgScreenVisible = true },
                onLongRight = { mainContentState.isChannelLineScreenVisible = true },
                onLongDown = { mainContentState.isVideoPlayerControllerScreenVisible = true },
                onNumber = { channelNumberSelectState.input(it) },
            )
            .handleDragGestures(
                onSwipeDown = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToNext()
                    else mainContentState.changeCurrentChannelToPrev()
                },
                onSwipeUp = {
                    if (settingsViewModel.iptvChannelChangeFlip) mainContentState.changeCurrentChannelToPrev()
                    else mainContentState.changeCurrentChannelToNext()
                },
                onSwipeRight = {
                    if (mainContentState.currentChannel.lineList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelLineIdx - 1,
                        )
                    }
                },
                onSwipeLeft = {
                    if (mainContentState.currentChannel.lineList.size > 1) {
                        mainContentState.changeCurrentChannel(
                            mainContentState.currentChannel,
                            mainContentState.currentChannelLineIdx + 1,
                        )
                    }
                },
            ),
    ) {
        VideoPlayerScreen(
            state = videoPlayerState,
            showMetadataProvider = { settingsViewModel.debugShowVideoPlayerMetadata },
        )

        Visibility({ mainContentState.currentChannelLine.url.startsWith("webview://") }) {
            WebViewScreen(
                urlProvider = { mainContentState.currentChannelLine.url },
                onVideoResolutionChanged = { width, height ->
                    videoPlayerState.metadata = videoPlayerState.metadata.copy(
                        video = (videoPlayerState.metadata.video
                            ?: VideoPlayer.Metadata.Video()).copy(
                            width = width,
                            height = height,
                        ),
                    )
                    mainContentState.isTempChannelScreenVisible = false
                },
            )
        }
    }

    Visibility({ settingsViewModel.uiShowEpgProgrammePermanentProgress }) {
        EpgProgrammeProgressScreen(
            currentEpgProgrammeProvider = {
                mainContentState.currentPlaybackEpgProgramme ?: epgListProvider().recentProgramme(
                    mainContentState.currentChannel
                )?.now
            },
            videoPlayerCurrentPositionProvider = { videoPlayerState.currentPosition },
        )
    }

    Visibility({
        !mainContentState.isTempChannelScreenVisible
                && !mainContentState.isChannelScreenVisible
                && !mainContentState.isQuickOpScreenVisible
                && !mainContentState.isEpgScreenVisible
                && !mainContentState.isChannelLineScreenVisible
                && channelNumberSelectState.channelNumber.isEmpty()
    }) {
        DatetimeScreen(showModeProvider = { settingsViewModel.uiTimeShowMode })
    }

    ChannelNumberSelectScreen(channelNumberProvider = { channelNumberSelectState.channelNumber })

    Visibility({
        mainContentState.isTempChannelScreenVisible
                && !mainContentState.isChannelScreenVisible
                && !mainContentState.isQuickOpScreenVisible
                && !mainContentState.isEpgScreenVisible
                && !mainContentState.isChannelLineScreenVisible
                && !mainContentState.isVideoPlayerControllerScreenVisible
                && channelNumberSelectState.channelNumber.isEmpty()
    }) {
        ChannelTempScreen(
            channelProvider = { mainContentState.currentChannel },
            channelLineIdxProvider = { mainContentState.currentChannelLineIdx },
            recentEpgProgrammeProvider = {
                epgListProvider().recentProgramme(mainContentState.currentChannel)
            },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            playerMetadataProvider = { videoPlayerState.metadata },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isEpgScreenVisible },
        onDismissRequest = { mainContentState.isEpgScreenVisible = false },
    ) {
        EpgScreen(
            epgProvider = {
                epgListProvider().match(mainContentState.currentChannel) ?: Epg.empty(
                    mainContentState.currentChannel
                )
            },
            epgProgrammeReserveListProvider = {
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList.filter {
                    it.channel == mainContentState.currentChannel.name
                })
            },
            supportPlaybackProvider = { mainContentState.supportPlayback() },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            onEpgProgrammePlayback = {
                mainContentState.isEpgScreenVisible = false
                mainContentState.changeCurrentChannel(
                    mainContentState.currentChannel,
                    mainContentState.currentChannelLineIdx,
                    it,
                )
            },
            onEpgProgrammeReserve = { programme ->
                mainContentState.reverseEpgProgrammeOrNot(
                    mainContentState.currentChannel, programme
                )
            },
            onClose = { mainContentState.isEpgScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isChannelLineScreenVisible },
        onDismissRequest = { mainContentState.isChannelLineScreenVisible = false },
    ) {
        ChannelLineScreen(
            channelProvider = { mainContentState.currentChannel },
            currentLineProvider = { mainContentState.currentChannelLine },
            onLineSelected = {
                mainContentState.isChannelLineScreenVisible = false
                mainContentState.changeCurrentChannel(
                    mainContentState.currentChannel,
                    mainContentState.currentChannel.lineList.indexOf(it),
                )
            },
            onClose = { mainContentState.isChannelLineScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isVideoPlayerControllerScreenVisible },
        onDismissRequest = { mainContentState.isVideoPlayerControllerScreenVisible = false },
    ) {
        val threshold = 1000L * 60 * 60 * 24 * 365
        val hour0 = -28800000L

        VideoPlayerControllerScreen(
            isVideoPlayerPlayingProvider = { videoPlayerState.isPlaying },
            isVideoPlayerBufferingProvider = { videoPlayerState.isBuffering },
            videoPlayerCurrentPositionProvider = {
                if (videoPlayerState.currentPosition >= threshold) videoPlayerState.currentPosition
                else hour0 + videoPlayerState.currentPosition
            },
            videoPlayerDurationProvider = {
                if (videoPlayerState.currentPosition >= threshold) {
                    val playback = mainContentState.currentPlaybackEpgProgramme

                    if (playback != null) {
                        playback.startAt to playback.endAt
                    } else {
                        val programme =
                            epgListProvider().recentProgramme(mainContentState.currentChannel)?.now
                        (programme?.startAt ?: hour0) to (programme?.endAt ?: hour0)
                    }
                } else {
                    hour0 to (hour0 + videoPlayerState.duration)
                }
            },
            onVideoPlayerPlay = { videoPlayerState.play() },
            onVideoPlayerPause = { videoPlayerState.pause() },
            onVideoPlayerSeekTo = { videoPlayerState.seekTo(it) },
            onClose = { mainContentState.isVideoPlayerControllerScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isVideoPlayerDisplayModeScreenVisible },
        onDismissRequest = { mainContentState.isVideoPlayerDisplayModeScreenVisible = false },
    ) {
        VideoPlayerDisplayModeScreen(
            currentDisplayModeProvider = { videoPlayerState.displayMode },
            onDisplayModeChanged = { videoPlayerState.displayMode = it },
            onApplyToGlobal = {
                mainContentState.isVideoPlayerDisplayModeScreenVisible = false
                settingsViewModel.videoPlayerDisplayMode = videoPlayerState.displayMode
                Snackbar.show("已应用到全局")
            },
            onClose = { mainContentState.isVideoPlayerDisplayModeScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isVideoTracksScreenVisible },
        onDismissRequest = { mainContentState.isVideoTracksScreenVisible = false },
    ) {
        VideoTracksScreen(
            trackListProvider = { videoPlayerState.metadata.videoTracks },
            onTrackChanged = {
                videoPlayerState.selectVideoTrack(it)
                mainContentState.isVideoTracksScreenVisible = false
            },
            onClose = { mainContentState.isVideoTracksScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isAudioTracksScreenVisible },
        onDismissRequest = { mainContentState.isAudioTracksScreenVisible = false },
    ) {
        AudioTracksScreen(
            trackListProvider = { videoPlayerState.metadata.audioTracks },
            onTrackChanged = {
                videoPlayerState.selectAudioTrack(it)
                mainContentState.isAudioTracksScreenVisible = false
            },
            onClose = { mainContentState.isAudioTracksScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isSubtitleTracksScreenVisible },
        onDismissRequest = { mainContentState.isSubtitleTracksScreenVisible = false },
    ) {
        SubtitleTracksScreen(
            trackListProvider = { videoPlayerState.metadata.subtitleTracks },
            onTrackChanged = {
                videoPlayerState.selectSubtitleTrack(it)
                mainContentState.isSubtitleTracksScreenVisible = false
            },
            onClose = { mainContentState.isSubtitleTracksScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isQuickOpScreenVisible },
        onDismissRequest = { mainContentState.isQuickOpScreenVisible = false },
    ) {
        QuickOpScreen(
            currentChannelProvider = { mainContentState.currentChannel },
            currentChannelLineIdxProvider = { mainContentState.currentChannelLineIdx },
            currentChannelNumberProvider = {
                (filteredChannelGroupListProvider().channelList.indexOf(mainContentState.currentChannel) + 1).toString()
            },
            epgListProvider = epgListProvider,
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            playerDisplayModeProvider = { videoPlayerState.displayMode },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
            onShowEpg = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isEpgScreenVisible = true
            },
            onShowChannelLine = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isChannelLineScreenVisible = true
            },
            onShowVideoPlayerController = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isVideoPlayerControllerScreenVisible = true
            },
            onShowVideoPlayerDisplayMode = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isVideoPlayerDisplayModeScreenVisible = true
            },
            onShowVideoTracks = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isVideoTracksScreenVisible = true
            },
            onShowAudioTracks = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isAudioTracksScreenVisible = true
            },
            onShowSubtitleTracks = {
                mainContentState.isQuickOpScreenVisible = false
                mainContentState.isSubtitleTracksScreenVisible = true
            },
            toSettingsScreen = {
                mainContentState.isQuickOpScreenVisible = false
                toSettingsScreen(it)
            },
            onClearCache = {
                settingsViewModel.iptvChannelLinePlayableHostList = emptySet()
                settingsViewModel.iptvChannelLinePlayableUrlList = emptySet()
                coroutineScope.launch {
                    IptvRepository(settingsViewModel.iptvSourceCurrent).clearCache()
                    EpgRepository(settingsViewModel.epgSourceCurrent).clearCache()
                    Snackbar.show("缓存已清除，请重启应用")
                }
            },
            toDashboardScreen = {
                mainContentState.isQuickOpScreenVisible = false
                toDashboardScreen()
            },
            onClose = { mainContentState.isQuickOpScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isChannelScreenVisible && !settingsViewModel.uiUseClassicPanelScreen },
        onDismissRequest = { mainContentState.isChannelScreenVisible = false },
    ) {
        ChannelScreen(
            channelGroupListProvider = filteredChannelGroupListProvider,
            favoriteChannelListProvider = favoriteChannelListProvider,
            currentChannelProvider = { mainContentState.currentChannel },
            currentChannelLineIdxProvider = { mainContentState.currentChannelLineIdx },
            showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
            onChannelSelected = {
                mainContentState.isChannelScreenVisible = false
                mainContentState.changeCurrentChannel(it)
            },
            onChannelFavoriteToggle = onChannelFavoriteToggle,
            epgListProvider = epgListProvider,
            showEpgProgrammeProgressProvider = { settingsViewModel.uiShowEpgProgrammeProgress },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
            channelFavoriteEnabledProvider = { settingsViewModel.iptvChannelFavoriteEnable },
            channelFavoriteListVisibleProvider = { settingsViewModel.iptvChannelFavoriteListVisible },
            onChannelFavoriteListVisibleChange = {
                settingsViewModel.iptvChannelFavoriteListVisible = it
            },
            onClose = { mainContentState.isChannelScreenVisible = false },
        )
    }

    PopupContent(
        visibleProvider = { mainContentState.isChannelScreenVisible && settingsViewModel.uiUseClassicPanelScreen },
        onDismissRequest = { mainContentState.isChannelScreenVisible = false },
    ) {
        ClassicChannelScreen(
            channelGroupListProvider = filteredChannelGroupListProvider,
            favoriteChannelListProvider = favoriteChannelListProvider,
            currentChannelProvider = { mainContentState.currentChannel },
            currentChannelLineIdxProvider = { mainContentState.currentChannelLineIdx },
            showChannelLogoProvider = { settingsViewModel.uiShowChannelLogo },
            onChannelSelected = {
                mainContentState.isChannelScreenVisible = false
                mainContentState.changeCurrentChannel(it)
            },
            onChannelFavoriteToggle = onChannelFavoriteToggle,
            epgListProvider = epgListProvider,
            epgProgrammeReserveListProvider = {
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList)
            },
            showEpgProgrammeProgressProvider = { settingsViewModel.uiShowEpgProgrammeProgress },
            supportPlaybackProvider = { mainContentState.supportPlayback(it, null) },
            currentPlaybackEpgProgrammeProvider = { mainContentState.currentPlaybackEpgProgramme },
            onEpgProgrammePlayback = { channel, programme ->
                mainContentState.isChannelScreenVisible = false
                mainContentState.changeCurrentChannel(channel, null, programme)
            },
            onEpgProgrammeReserve = { channel, programme ->
                mainContentState.reverseEpgProgrammeOrNot(channel, programme)
            },
            videoPlayerMetadataProvider = { videoPlayerState.metadata },
            channelFavoriteEnabledProvider = { settingsViewModel.iptvChannelFavoriteEnable },
            channelFavoriteListVisibleProvider = { settingsViewModel.iptvChannelFavoriteListVisible },
            onChannelFavoriteListVisibleChange = {
                settingsViewModel.iptvChannelFavoriteListVisible = it
            },
            onClose = { mainContentState.isChannelScreenVisible = false },
        )
    }

    EpgReverseScreen(
        epgProgrammeReserveListProvider = { settingsViewModel.epgChannelReserveList },
        onConfirmReserve = { reserve ->
            filteredChannelGroupListProvider().channelList.firstOrNull { it.name == reserve.channel }
                ?.let {
                    mainContentState.changeCurrentChannel(it)
                }
        },
        onDeleteReserve = { reserve ->
            settingsViewModel.epgChannelReserveList =
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList - reserve)
        },
    )
}