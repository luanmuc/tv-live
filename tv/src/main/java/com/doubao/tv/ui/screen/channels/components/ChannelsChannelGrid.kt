package com.doubao.tv.ui.screen.channels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.core.data.entities.epg.EpgList.Companion.recentProgramme
import com.doubao.tv.ui.rememberChildPadding
import com.doubao.tv.ui.screen.components.AppScreen
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.backHandler
import com.doubao.tv.ui.utils.ifElse
import com.doubao.tv.ui.utils.saveFocusRestorer
import com.doubao.tv.ui.utils.saveRequestFocus

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChannelsChannelGrid(
    modifier: Modifier = Modifier,
    channelListProvider: () -> ChannelList = { ChannelList() },
    onChannelSelected: (Channel) -> Unit = {},
    onChannelFavoriteToggle: (Channel) -> Unit = {},
    epgListProvider: () -> EpgList = { EpgList() },
    inFavoriteMode: Boolean = false,
    updateTopBarVisibility: (Boolean) -> Unit = {},
) {
    val channelList = channelListProvider()

    val coroutineScope = rememberCoroutineScope()
    val childPadding = rememberChildPadding()
    val focusManager = LocalFocusManager.current

    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var isFirstItemFocused by remember { mutableStateOf(false) }

    LaunchedEffect(channelList) {
        gridState.scrollToItem(0)
    }

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 10
        }
    }
    LaunchedEffect(shouldShowTopBar) {
        updateTopBarVisibility(shouldShowTopBar)
    }

    LazyVerticalGrid(
        modifier = modifier
            .backHandler {
                if (!isFirstItemFocused) {
                    coroutineScope.launch {
                        gridState.scrollToItem(0)
                        firstItemFocusRequester.saveRequestFocus()
                    }
                } else {
                    focusManager.moveFocus(FocusDirection.Up)
                }
            }
            .ifElse(
                settingsVM.uiFocusOptimize && channelList.isNotEmpty(),
                Modifier.saveFocusRestorer { firstItemFocusRequester }
            ),
        state = gridState,
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = childPadding.copy(top = 10.dp).paddingValues,
    ) {
        itemsIndexed(channelList) { index, channel ->
            ChannelsChannelItem(
                modifier = Modifier
                    .ifElse(
                        index == 0,
                        Modifier
                            .focusRequester(firstItemFocusRequester)
                            .onFocusChanged { isFirstItemFocused = it.isFocused },
                    ),
                channelProvider = { channel },
                onChannelSelected = { onChannelSelected(channel) },
                onChannelFavoriteToggle = {
                    if (inFavoriteMode) {
                        if (index % 5 == 0 && index == channelList.size - 1)
                            focusManager.moveFocus(FocusDirection.Up)
                    }

                    onChannelFavoriteToggle(channel)
                },
                recentEpgProgrammeProvider = { epgListProvider().recentProgramme(channel) },
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun ChannelsChannelGridPreview() {
    MyTvTheme {
        AppScreen {
            ChannelsChannelGrid(
                modifier = Modifier.padding(vertical = 20.dp),
                channelListProvider = { ChannelList.EXAMPLE },
                epgListProvider = { EpgList.example(ChannelList.EXAMPLE) },
            )
        }
        PreviewWithLayoutGrids { }
    }
}