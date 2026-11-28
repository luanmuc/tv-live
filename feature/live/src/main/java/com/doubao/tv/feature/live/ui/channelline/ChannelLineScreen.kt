package com.doubao.tv.feature.live.ui.channelline

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelLine
import com.doubao.tv.ui.material.Drawer
import com.doubao.tv.ui.material.DrawerPosition
import com.doubao.tv.feature.live.ui.channelline.components.ChannelLineItemList
import com.doubao.tv.feature.live.ui.components.rememberScreenAutoCloseState
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler

@Composable
fun ChannelLineScreen(
    modifier: Modifier = Modifier,
    channelProvider: () -> Channel = { Channel() },
    currentLineProvider: () -> ChannelLine = { ChannelLine() },
    onLineSelected: (ChannelLine) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.backHandler { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("多线路") },
    ) {
        ChannelLineItemList(
            modifier = Modifier.width(268.dp),
            lineListProvider = { channelProvider().lineList },
            currentLineProvider = currentLineProvider,
            onSelected = onLineSelected,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun ChannelLineScreenPreview() {
    MyTvTheme {
        PreviewWithLayoutGrids {
            ChannelLineScreen(
                channelProvider = { Channel.EXAMPLE },
            )
        }
    }
}