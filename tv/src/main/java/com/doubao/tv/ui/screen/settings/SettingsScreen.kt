package com.doubao.tv.ui.screen.settings

import com.doubao.tv.settings.VideoPlayerDisplayMode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.core.data.entities.epgsource.EpgSourceList
import com.doubao.tv.core.data.entities.iptvsource.IptvSourceList
import com.doubao.tv.core.data.repositories.epg.EpgRepository
import com.doubao.tv.core.data.repositories.iptv.IptvRepository
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.screen.components.AppScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsAppScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsControlScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsEpgScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsIptvScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsLogScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsNetworkScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsPermissionsScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsThemeScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsUiScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsUpdateScreen
import com.doubao.tv.ui.screen.settings.categories.SettingsVideoPlayerScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsChannelGroupVisibilityScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsEpgRefreshTimeThresholdScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsEpgSourceScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsIptvHybridModeScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsIptvSourceCacheTimeScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsIptvSourceScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsUiDensityScaleRatioScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsUiFontScaleRatioScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsUiScreenAutoCloseScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsUiTimeShowModeScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsUpdateChannelScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsVideoPlayerCoreScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsVideoPlayerDisplayModeScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsVideoPlayerLoadTimeoutScreen
import com.doubao.tv.ui.screen.settings.subcategories.SettingsVideoPlayerRenderModeScreen
import com.doubao.tv.ui.utils.navigateSingleTop

object SettingsScreen {
    const val START_DESTINATION = "startDestination"
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    startDestinationProvider: () -> String? = { null },
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    settingsViewModel: SettingsViewModel = settingsVM,
    onCheckUpdate: () -> Unit = {},
    onReload: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            settingsViewModel.refresh()
            delay(1000)
        }
    }

    val navController = rememberNavController()

    AppScreen(modifier = modifier, onBackPressed = onBackPressed) {
        NavHost(
            navController = navController,
            startDestination = SettingsDestination.fromStoredName(startDestinationProvider()),
            builder = {
                composable<SettingsDestination.Categories> {
                    SettingsCategoriesScreen(
                        toSettingsCategoryScreen = { navController.navigateSingleTop(SettingsDestination.fromCategory(it)) },
                        onBackPressed = onBackPressed,
                    )
                }

                composable<SettingsDestination.App> {
                    SettingsAppScreen(
                        onReload = onReload,
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Iptv> {
                    SettingsIptvScreen(
                        channelGroupListProvider = channelGroupListProvider,
                        toIptvSourceScreen = { navController.navigateSingleTop(SettingsDestination.IptvSource) },
                        toIptvSourceCacheTimeScreen = {
                            navController.navigateSingleTop(SettingsDestination.IptvSourceCacheTime)
                        },
                        toChannelGroupVisibilityScreen = {
                            navController.navigateSingleTop(SettingsDestination.ChannelGroupVisibility)
                        },
                        toIptvHybridModeScreen = {
                            navController.navigateSingleTop(SettingsDestination.IptvHybridMode)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Epg> {
                    SettingsEpgScreen(
                        toEpgSourceScreen = { navController.navigateSingleTop(SettingsDestination.EpgSource) },
                        toEpgRefreshTimeThresholdScreen = {
                            navController.navigateSingleTop(SettingsDestination.EpgRefreshTimeThreshold)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Ui> {
                    SettingsUiScreen(
                        toUiTimeShowModeScreen = {
                            navController.navigateSingleTop(SettingsDestination.UiTimeShowMode)
                        },
                        toUiScreenAutoCloseDelayScreen = {
                            navController.navigateSingleTop(SettingsDestination.UiScreenAutoCloseDelay)
                        },
                        toUiDensityScaleRatioScreen = {
                            navController.navigateSingleTop(SettingsDestination.UiDensityScaleRatio)
                        },
                        toUiFontScaleRatioScreen = {
                            navController.navigateSingleTop(SettingsDestination.UiFontScaleRatio)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Control> {
                    SettingsControlScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.VideoPlayer> {
                    SettingsVideoPlayerScreen(
                        toVideoPlayerCoreScreen = {
                            navController.navigateSingleTop(SettingsDestination.VideoPlayerCore)
                        },
                        toVideoPlayerRenderModeScreen = {
                            navController.navigateSingleTop(SettingsDestination.VideoPlayerRenderMode)
                        },
                        toVideoPlayerDisplayModeScreen = {
                            navController.navigateSingleTop(SettingsDestination.VideoPlayerDisplayMode)
                        },
                        toVideoPlayerLoadTimeoutScreen = {
                            navController.navigateSingleTop(SettingsDestination.VideoPlayerLoadTimeout)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Update> {
                    SettingsUpdateScreen(
                        toUpdateChannelScreen = {
                            navController.navigateSingleTop(SettingsDestination.UpdateChannel)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Network> {
                    SettingsNetworkScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Theme> {
                    SettingsThemeScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Log> {
                    SettingsLogScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.Permissions> {
                    SettingsPermissionsScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.IptvSource> {
                    SettingsIptvSourceScreen(
                        currentIptvSourceProvider = { settingsViewModel.iptvSourceCurrent },
                        iptvSourceListProvider = { settingsViewModel.iptvSourceList },
                        onSetCurrent = {
                            settingsViewModel.iptvSourceCurrent = it
                            settingsViewModel.iptvChannelGroupHiddenList = emptySet()
                            settingsViewModel.iptvChannelLastPlay = Channel.EMPTY
                            onReload()
                        },
                        onDelete = {
                            settingsViewModel.iptvSourceList =
                                IptvSourceList(settingsViewModel.iptvSourceList - it)
                        },
                        onClearCache = {
                            coroutineScope.launch {
                                IptvRepository(it).clearCache()
                                Snackbar.show("缓存已清除")
                            }
                        },
                        onAddSource = {
                            settingsViewModel.iptvSourceList =
                                IptvSourceList(settingsViewModel.iptvSourceList + it)
                        },
                        onBackPressed = {
                            if (!navController.navigateUp()) onBackPressed()
                        },
                    )
                }

                composable<SettingsDestination.IptvSourceCacheTime> {
                    SettingsIptvSourceCacheTimeScreen(
                        cacheTimeProvider = { settingsViewModel.iptvSourceCacheTime },
                        onCacheTimeChanged = {
                            settingsViewModel.iptvSourceCacheTime = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.ChannelGroupVisibility> {
                    var hasChanged by remember { mutableStateOf(false) }
                    SettingsChannelGroupVisibilityScreen(
                        channelGroupListProvider = channelGroupListProvider,
                        channelGroupNameHiddenListProvider = { settingsViewModel.iptvChannelGroupHiddenList.toList() },
                        onChannelGroupNameHiddenListChange = {
                            settingsViewModel.iptvChannelGroupHiddenList = it.toSet()
                            hasChanged = true
                        },
                        onBackPressed = {
                            if (hasChanged) onReload()
                            else navController.navigateUp()
                        },
                    )
                }

                composable<SettingsDestination.IptvHybridMode> {
                    SettingsIptvHybridModeScreen(
                        hybridModeProvider = { settingsViewModel.iptvHybridMode },
                        onHybridModeChanged = {
                            settingsViewModel.iptvHybridMode = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.EpgSource> {
                    SettingsEpgSourceScreen(
                        currentEpgSourceProvider = { settingsViewModel.epgSourceCurrent },
                        epgSourceListProvider = { settingsViewModel.epgSourceList },
                        onSetCurrent = {
                            settingsViewModel.epgSourceCurrent = it
                            onReload()
                        },
                        onDelete = {
                            settingsViewModel.epgSourceList =
                                EpgSourceList(settingsViewModel.epgSourceList - it)
                        },
                        onClearCache = {
                            coroutineScope.launch {
                                EpgRepository(it).clearCache()
                                Snackbar.show("缓存已清除")
                            }
                        },
                        onAddSource = {
                            settingsViewModel.epgSourceList =
                                EpgSourceList(settingsViewModel.epgSourceList + it)
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.EpgRefreshTimeThreshold> {
                    SettingsEpgRefreshTimeThresholdScreen(
                        timeThresholdProvider = { settingsViewModel.epgRefreshTimeThreshold },
                        onTimeThresholdChanged = { settingsViewModel.epgRefreshTimeThreshold = it },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.UiTimeShowMode> {
                    SettingsUiTimeShowModeScreen(
                        timeShowModeProvider = { settingsViewModel.uiTimeShowMode },
                        onTimeShowModeChanged = {
                            settingsViewModel.uiTimeShowMode = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.UiScreenAutoCloseDelay> {
                    SettingsUiScreenAutoCloseScreen(
                        delayProvider = { settingsViewModel.uiScreenAutoCloseDelay },
                        onDelayChanged = {
                            settingsViewModel.uiScreenAutoCloseDelay = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.UiDensityScaleRatio> {
                    SettingsUiDensityScaleRatioScreen(
                        scaleRatioProvider = { settingsViewModel.uiDensityScaleRatio },
                        onScaleRatioChanged = { settingsViewModel.uiDensityScaleRatio = it },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.UiFontScaleRatio> {
                    SettingsUiFontScaleRatioScreen(
                        scaleRatioProvider = { settingsViewModel.uiFontScaleRatio },
                        onScaleRatioChanged = { settingsViewModel.uiFontScaleRatio = it },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.VideoPlayerCore> {
                    SettingsVideoPlayerCoreScreen(
                        coreProvider = { settingsViewModel.videoPlayerCore },
                        onCoreChanged = {
                            settingsViewModel.videoPlayerCore = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.VideoPlayerRenderMode> {
                    SettingsVideoPlayerRenderModeScreen(
                        renderModeProvider = { settingsViewModel.videoPlayerRenderMode },
                        onRenderModeChanged = {
                            settingsViewModel.videoPlayerRenderMode = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.VideoPlayerDisplayMode> {
                    SettingsVideoPlayerDisplayModeScreen(
                        displayModeProvider = { settingsViewModel.videoPlayerDisplayMode },
                        onDisplayModeChanged = {
                            settingsViewModel.videoPlayerDisplayMode = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.VideoPlayerLoadTimeout> {
                    SettingsVideoPlayerLoadTimeoutScreen(
                        timeoutProvider = { settingsViewModel.videoPlayerLoadTimeout },
                        onTimeoutChanged = {
                            settingsViewModel.videoPlayerLoadTimeout = it
                            navController.navigateUp()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<SettingsDestination.UpdateChannel> {
                    SettingsUpdateChannelScreen(
                        updateChannelProvider = { settingsViewModel.updateChannel },
                        onUpdateChannelChanged = {
                            settingsViewModel.updateChannel = it
                            navController.navigateUp()
                            onCheckUpdate()
                        },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

            }
        )
    }
}
