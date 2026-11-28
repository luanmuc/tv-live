package com.doubao.tv.feature.live.ui.audiotracks

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Text
import com.doubao.tv.ui.material.Drawer
import com.doubao.tv.ui.material.DrawerPosition
import com.doubao.tv.feature.live.ui.audiotracks.components.AudioTrackItemList
import com.doubao.tv.feature.live.ui.components.rememberScreenAutoCloseState
import com.doubao.tv.feature.live.ui.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler
import com.doubao.tv.ui.utils.gridColumns

@Composable
fun AudioTracksScreen(
    modifier: Modifier = Modifier,
    trackListProvider: () -> List<VideoPlayer.Metadata.Audio> = { emptyList() },
    onTrackChanged: (VideoPlayer.Metadata.Audio?) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.backHandler { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("音轨") },
    ) {
        AudioTrackItemList(
            modifier = Modifier.width(4.5f.gridColumns()),
            trackListProvider = trackListProvider,
            onSelected = onTrackChanged,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun AudioTracksScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            AudioTracksScreen(
                trackListProvider = {
                    listOf(
                        VideoPlayer.Metadata.Audio(
                            channels = 2,
                            bitrate = 128000,
                        ),
                        VideoPlayer.Metadata.Audio(
                            channels = 10,
                            bitrate = 567000,
                            isSelected = true,
                        )
                    )
                },
            )
        }
    }
}