package com.doubao.tv.feature.live.ui.videoplayerdiaplaymode

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import kotlinx.collections.immutable.toPersistentList
import com.doubao.tv.ui.material.Drawer
import com.doubao.tv.ui.material.DrawerPosition
import com.doubao.tv.feature.live.ui.components.rememberScreenAutoCloseState
import com.doubao.tv.settings.VideoPlayerDisplayMode
import com.doubao.tv.feature.live.ui.videoplayerdiaplaymode.components.VideoPlayerDisplayModeItemList
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler

@Composable
fun VideoPlayerDisplayModeScreen(
    modifier: Modifier = Modifier,
    currentDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    onDisplayModeChanged: (VideoPlayerDisplayMode) -> Unit = {},
    onApplyToGlobal: (() -> Unit)? = null,
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.backHandler { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("显示模式") },
    ) {
        VideoPlayerDisplayModeItemList(
            modifier = Modifier.width(268.dp),
            displayModeListProvider = { VideoPlayerDisplayMode.entries.toPersistentList() },
            currentDisplayModeProvider = currentDisplayModeProvider,
            onSelected = onDisplayModeChanged,
            onApplyToGlobal = onApplyToGlobal,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun VideoPlayerDisplayModeScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            VideoPlayerDisplayModeScreen()
        }
    }
}