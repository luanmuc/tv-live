package com.doubao.tv.feature.live.ui.quickop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.core.data.entities.epg.EpgList.Companion.recentProgramme
import com.doubao.tv.core.data.entities.epg.EpgProgramme
import com.doubao.tv.ui.rememberChildPadding
import com.doubao.tv.ui.screen.dashboard.DashboardScreeIptvSource
import com.doubao.tv.feature.live.ui.live.channels.components.LiveChannelsChannelInfo
import com.doubao.tv.ui.screen.settings.SettingsSubCategories
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.feature.live.ui.channel.components.ChannelNumber
import com.doubao.tv.feature.live.ui.components.rememberScreenAutoCloseState
import com.doubao.tv.feature.live.ui.datetime.components.DateTimeDetail
import com.doubao.tv.feature.live.ui.quickop.components.QuickOpBtnList
import com.doubao.tv.settings.VideoPlayerDisplayMode
import com.doubao.tv.feature.live.ui.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler

@Composable
fun QuickOpScreen(
    modifier: Modifier = Modifier,
    currentChannelProvider: () -> Channel = { Channel() },
    currentChannelLineIdxProvider: () -> Int = { 0 },
    currentChannelNumberProvider: () -> String = { "" },
    epgListProvider: () -> EpgList = { EpgList() },
    isInTimeShiftProvider: () -> Boolean = { false },
    currentPlaybackEpgProgrammeProvider: () -> EpgProgramme? = { null },
    playerDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    videoPlayerMetadataProvider: () -> VideoPlayer.Metadata = { VideoPlayer.Metadata() },
    onShowEpg: () -> Unit = {},
    onShowChannelLine: () -> Unit = {},
    onShowVideoPlayerController: () -> Unit = {},
    onShowVideoPlayerDisplayMode: () -> Unit = {},
    onShowVideoTracks: () -> Unit = {},
    onShowAudioTracks: () -> Unit = {},
    onShowSubtitleTracks: () -> Unit = {},
    onClearCache: () -> Unit = {},
    toSettingsScreen: (SettingsSubCategories?) -> Unit = {},
    toDashboardScreen: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Box(
        modifier = modifier
            .backHandler { onClose() }
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
    ) {
        QuickOpScreenTop(
            channelNumberProvider = {
                currentChannelNumberProvider().padStart(2, '0')
            },
            toSettingsIptvSourceScreen = { toSettingsScreen(SettingsSubCategories.IPTV_SOURCE) },
        )

        QuickOpScreenBottom(
            currentChannelProvider = currentChannelProvider,
            currentChannelLineIdxProvider = currentChannelLineIdxProvider,
            epgListProvider = epgListProvider,
            isInTimeShiftProvider = isInTimeShiftProvider,
            currentPlaybackEpgProgrammeProvider = currentPlaybackEpgProgrammeProvider,
            playerDisplayModeProvider = playerDisplayModeProvider,
            videoPlayerMetadataProvider = videoPlayerMetadataProvider,
            onShowEpg = onShowEpg,
            onShowChannelLine = onShowChannelLine,
            onShowVideoPlayerController = onShowVideoPlayerController,
            onShowVideoPlayerDisplayMode = onShowVideoPlayerDisplayMode,
            onShowVideoTracks = onShowVideoTracks,
            onShowAudioTracks = onShowAudioTracks,
            onShowSubtitleTracks = onShowSubtitleTracks,
            onShowMoreSettings = { toSettingsScreen(null) },
            onClearCache = onClearCache,
            toDashboardScreen = toDashboardScreen,
            onUserAction = { screenAutoCloseState.active() },
            onDismissRequest = onClose,
        )
    }
}

@Composable
private fun QuickOpScreenTop(
    modifier: Modifier = Modifier,
    channelNumberProvider: () -> String = { "" },
    toSettingsIptvSourceScreen: () -> Unit = {},
) {
    val iptvSourceCurrent = settingsVM.iptvSourceCurrent
    val childPadding = rememberChildPadding()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(childPadding.paddingValues),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            DashboardScreeIptvSource(
                currentIptvSourceProvider = { iptvSourceCurrent },
                toSettingsIptvSourceScreen = toSettingsIptvSourceScreen,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelNumber(channelNumberProvider = channelNumberProvider)

            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                Spacer(
                    modifier = Modifier
                        .background(Color.White)
                        .width(2.dp)
                        .height(30.dp),
                )
            }

            DateTimeDetail()
        }
    }
}

@Composable
private fun QuickOpScreenBottom(
    modifier: Modifier = Modifier,
    currentChannelProvider: () -> Channel = { Channel() },
    currentChannelLineIdxProvider: () -> Int = { 0 },
    epgListProvider: () -> EpgList = { EpgList() },
    isInTimeShiftProvider: () -> Boolean = { false },
    currentPlaybackEpgProgrammeProvider: () -> EpgProgramme? = { null },
    playerDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    videoPlayerMetadataProvider: () -> VideoPlayer.Metadata = { VideoPlayer.Metadata() },
    onShowEpg: () -> Unit = {},
    onShowChannelLine: () -> Unit = {},
    onShowVideoPlayerController: () -> Unit = {},
    onShowVideoPlayerDisplayMode: () -> Unit = {},
    onShowVideoTracks: () -> Unit = {},
    onShowAudioTracks: () -> Unit = {},
    onShowSubtitleTracks: () -> Unit = {},
    onShowMoreSettings: () -> Unit = {},
    onClearCache: () -> Unit = {},
    toDashboardScreen: () -> Unit = {},
    onUserAction: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0xB4000000),
                            Color(0xF3000000),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY),
                    )
                )
                .padding(bottom = childPadding.bottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LiveChannelsChannelInfo(
                modifier = Modifier.padding(start = childPadding.start, end = childPadding.end),
                channelProvider = currentChannelProvider,
                channelLineIdxProvider = currentChannelLineIdxProvider,
                recentEpgProgrammeProvider = {
                    epgListProvider().recentProgramme(currentChannelProvider())
                },
                isInTimeShiftProvider = isInTimeShiftProvider,
                currentPlaybackEpgProgrammeProvider = currentPlaybackEpgProgrammeProvider,
                playerMetadataProvider = videoPlayerMetadataProvider,
            )

            QuickOpBtnList(
                channelProvider = currentChannelProvider,
                channelLineIdxProvider = currentChannelLineIdxProvider,
                playerDisplayModeProvider = playerDisplayModeProvider,
                playerMetadataProvider = videoPlayerMetadataProvider,
                onShowEpg = onShowEpg,
                onShowChannelLine = onShowChannelLine,
                onShowVideoPlayerController = onShowVideoPlayerController,
                onShowVideoPlayerDisplayMode = onShowVideoPlayerDisplayMode,
                onShowMoreSettings = onShowMoreSettings,
                onShowVideoTracks = onShowVideoTracks,
                onShowAudioTracks = onShowAudioTracks,
                onShowSubtitleTracks = onShowSubtitleTracks,
                onClearCache = onClearCache,
                toDashboardScreen = toDashboardScreen,
                onUserAction = onUserAction,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun QuickOpScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            QuickOpScreen(
                currentChannelProvider = { Channel.EXAMPLE },
                currentChannelNumberProvider = { "1" },
                epgListProvider = {
                    EpgList.example(ChannelList(listOf(Channel.EXAMPLE)))
                },
            )
        }
    }
}