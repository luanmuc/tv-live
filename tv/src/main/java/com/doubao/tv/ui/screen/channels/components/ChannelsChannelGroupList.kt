package com.doubao.tv.ui.screen.channels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doubao.tv.core.data.entities.channel.ChannelGroup
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.ui.material.LazyRow
import com.doubao.tv.ui.material.items
import com.doubao.tv.ui.rememberChildPadding
import com.doubao.tv.ui.theme.MyTvTheme

@Composable
fun ChannelsChannelGroupList(
    modifier: Modifier = Modifier,
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    currentChannelGroupProvider: () -> ChannelGroup = { ChannelGroup() },
    onChannelGroupSelected: (ChannelGroup) -> Unit = {},
) {
    val channelGroupList = channelGroupListProvider()
    val currentChannelGroup = currentChannelGroupProvider()
    val childPadding = rememberChildPadding()

    if (channelGroupList.size <= 1) return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) { runtime ->
            items(channelGroupList, runtime) { itemModifier, channelGroup ->
                ChannelsChannelGroupItem(
                    modifier = itemModifier,
                    channelGroupProvider = { channelGroup },
                    isSelectedProvider = { channelGroup == currentChannelGroup },
                    onChannelGroupSelected = { onChannelGroupSelected(channelGroup) },
                )
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun ChannelsChannelGroupListPreview() {
    MyTvTheme {
        ChannelsChannelGroupList(channelGroupListProvider = { ChannelGroupList.EXAMPLE },
            currentChannelGroupProvider = { ChannelGroupList.EXAMPLE.first() })
    }
}