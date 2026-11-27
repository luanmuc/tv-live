package com.doubao.tv.ui.screensold.videotracks

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Text
import com.doubao.tv.ui.material.Drawer
import com.doubao.tv.ui.material.DrawerPosition
import com.doubao.tv.ui.screensold.components.rememberScreenAutoCloseState
import com.doubao.tv.ui.screensold.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.screensold.videotracks.components.VideoTrackItemList
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler
import com.doubao.tv.ui.utils.gridColumns

@Composable
fun VideoTracksScreen(
    modifier: Modifier = Modifier,
    trackListProvider: () -> List<VideoPlayer.Metadata.Video> = { emptyList() },
    onTrackChanged: (VideoPlayer.Metadata.Video?) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.backHandler { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("视轨") },
    ) {
        VideoTrackItemList(
            modifier = Modifier.width(4.5f.gridColumns()),
            trackListProvider = trackListProvider,
            onSelected = onTrackChanged,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun VideoTracksScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            VideoTracksScreen(
                trackListProvider = {
                    listOf(
                        VideoPlayer.Metadata.Video(
                            width = 1920,
                            height = 1080,
                            bitrate = 5_000_000,
                            frameRate = 50f,
                        ),
                        VideoPlayer.Metadata.Video(
                            width = 1280,
                            height = 720,
                            bitrate = 1_000_000,
                            isSelected = true,
                        )
                    )
                },
            )
        }
    }
}