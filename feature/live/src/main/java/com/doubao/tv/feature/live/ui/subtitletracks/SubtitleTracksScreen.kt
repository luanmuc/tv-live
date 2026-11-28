package com.doubao.tv.feature.live.ui.subtitletracks

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Text
import com.doubao.tv.ui.material.Drawer
import com.doubao.tv.ui.material.DrawerPosition
import com.doubao.tv.feature.live.ui.components.rememberScreenAutoCloseState
import com.doubao.tv.feature.live.ui.subtitletracks.components.SubtitleTrackItemList
import com.doubao.tv.feature.live.ui.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler
import com.doubao.tv.ui.utils.gridColumns

@Composable
fun SubtitleTracksScreen(
    modifier: Modifier = Modifier,
    trackListProvider: () -> List<VideoPlayer.Metadata.Subtitle> = { emptyList() },
    onTrackChanged: (VideoPlayer.Metadata.Subtitle?) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.backHandler { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("字幕") },
    ) {
        SubtitleTrackItemList(
            modifier = Modifier.width(4.5f.gridColumns()),
            trackListProvider = trackListProvider,
            onSelected = onTrackChanged,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun SubtitleTracksScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            SubtitleTracksScreen(
                trackListProvider = {
                    listOf(
                        VideoPlayer.Metadata.Subtitle(
                            bitrate = 10000,
                            language = "zh",
                        ),
                        VideoPlayer.Metadata.Subtitle(
                            bitrate = 10000,
                            language = "en",
                            isSelected = true,
                        )
                    )
                },
            )
        }
    }
}