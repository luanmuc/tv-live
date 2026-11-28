package com.doubao.tv.ui.screen.dashboard

import com.doubao.tv.AppContainer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.repositories.iptv.IptvRepository
import com.doubao.tv.ui.rememberChildPadding
import com.doubao.tv.ui.screen.components.AppScreen
import com.doubao.tv.ui.screen.dashboard.components.DashboardFavoriteList
import com.doubao.tv.ui.screen.dashboard.components.DashboardModuleList
import com.doubao.tv.ui.screen.dashboard.components.DashboardTime
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.utils.Configs
import com.doubao.tv.ui.utils.focusOnLaunched
import com.doubao.tv.ui.utils.handleKeyEvents

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    currentIptvSourceProvider: () -> IptvSource = { IptvSource() },
    channelFavoriteListProvider: () -> ChannelFavoriteList = { ChannelFavoriteList() },
    onChannelSelected: (Channel) -> Unit = {},
    onChannelFavoriteToggle: (Channel) -> Unit = {},
    epgListProvider: () -> EpgList = { EpgList() },
    toLiveScreen: () -> Unit = {},
    toChannelsScreen: () -> Unit = {},
    toFavoritesScreen: () -> Unit = {},
    toSearchScreen: () -> Unit = {},
    toMultiViewScreen: () -> Unit = {},
    toSettingsScreen: () -> Unit = {},
    toAboutScreen: () -> Unit = {},
    toSettingsIptvSourceScreen: () -> Unit = {},
    onReload: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()
    val coroutineScope = rememberCoroutineScope()

    AppScreen(
        modifier = modifier,
        header = {
            DashboardScreeIptvSource(
                currentIptvSourceProvider = currentIptvSourceProvider,
                toSettingsIptvSourceScreen = toSettingsIptvSourceScreen,
                clearCurrentIptvSourceCache = {
                    coroutineScope.launch {
                        IptvRepository(AppContainer.settings.current.iptvSourceCurrent).clearCache()
                        onReload()
                    }
                },
            )
        },
        headerExtra = { DashboardTime() },
        onBackPressed = onBackPressed,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 20.dp, bottom = childPadding.bottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                DashboardModuleList(
                    modifier = Modifier.focusOnLaunched(),
                    toLiveScreen = toLiveScreen,
                    toChannelsScreen = toChannelsScreen,
                    toFavoritesScreen = toFavoritesScreen,
                    toSearchScreen = toSearchScreen,
                    toMultiViewScreen = toMultiViewScreen,
                    toSettingsScreen = toSettingsScreen,
                    toAboutScreen = toAboutScreen,
                )
            }

            item {
                DashboardFavoriteList(
                    channelFavoriteListProvider = channelFavoriteListProvider,
                    onChannelSelected = onChannelSelected,
                    onChannelUnFavorite = onChannelFavoriteToggle,
                    epgListProvider = epgListProvider,
                )
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun DashboardScreenScreen() {
    MyTvTheme {
        DashboardScreen(
            currentIptvSourceProvider = { IptvSource(name = "默认直播源1") },
            channelFavoriteListProvider = { ChannelFavoriteList.EXAMPLE },
            epgListProvider = { EpgList.example(ChannelList.EXAMPLE) },
        )
        // PreviewWithLayoutGrids { }
    }
}