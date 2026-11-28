package com.doubao.tv.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelFavorite
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.chanelGroup
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.BuildConfig
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.screen.Destination
import com.doubao.tv.ui.screen.about.AboutScreen
import com.doubao.tv.ui.screen.agreement.AgreementScreen
import com.doubao.tv.ui.screen.channels.ChannelsScreen
import com.doubao.tv.ui.screen.dashboard.DashboardScreen
import com.doubao.tv.ui.screen.favorites.FavoritesScreen
import com.doubao.tv.ui.screen.loading.LoadingScreen
import com.doubao.tv.ui.screen.multiview.MultiViewScreen
import com.doubao.tv.ui.screen.search.SearchScreen
import com.doubao.tv.ui.screen.settings.SettingsScreen
import com.doubao.tv.ui.screen.settings.SettingsSubCategories
import com.doubao.tv.ui.screen.settings.SettingsViewModel
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.screen.update.UpdateScreen
import com.doubao.tv.ui.screen.update.UpdateViewModel
import com.doubao.tv.ui.screen.update.updateVM
import com.doubao.tv.ui.theme.BrandBackground
import com.doubao.tv.ui.utils.navigateSingleTop

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = settingsVM,
    updateViewModel: UpdateViewModel = updateVM,
    mainViewModel: MainViewModel = mainVM,
    onBackPressed: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(mainViewModel, settingsViewModel) {
        mainViewModel.refreshRequests.collect { settingsViewModel.refresh() }
    }

    val channelGroupListProvider = {
        (uiState as? MainUiState.Ready)?.channelGroupList ?: ChannelGroupList()
    }
    val filteredChannelGroupListProvider = {
        (uiState as? MainUiState.Ready)?.filteredChannelGroupList ?: ChannelGroupList()
    }
    val epgListProvider = { (uiState as? MainUiState.Ready)?.epgList ?: EpgList() }
    val favoriteChannelListProvider = {
        ChannelList(settingsViewModel.iptvChannelFavoriteList.map { it.channel.copy(index = -1) })
    }

    val navController = rememberNavController()

    fun onChannelSelected(channel: Channel) {
        settingsViewModel.iptvChannelLastPlay = channel
        navController.navigateSingleTop(Destination.Live)
    }

    fun onChannelFavoriteToggle(channel: Channel) {
        if (!settingsViewModel.iptvChannelFavoriteEnable) return


        if (settingsViewModel.iptvChannelFavoriteList.any { it.channel == channel }) {
            settingsViewModel.iptvChannelFavoriteList =
                ChannelFavoriteList(settingsViewModel.iptvChannelFavoriteList.filter {
                    it.channel != channel
                })

            Snackbar.show("取消收藏：${channel.name}")
        } else {
            val favoriteChannel = ChannelFavorite(
                channel = channel,
                iptvSourceName = settingsViewModel.iptvSourceCurrent.name,
                groupName = filteredChannelGroupListProvider().chanelGroup(channel).name,
            )

            settingsViewModel.iptvChannelFavoriteList =
                ChannelFavoriteList(settingsViewModel.iptvChannelFavoriteList + favoriteChannel)

            Snackbar.show("已收藏：${channel.name}")
        }
    }

    fun onChannelFavoriteClear() {
        if (!settingsViewModel.iptvChannelFavoriteEnable) return

        settingsViewModel.iptvChannelFavoriteList = ChannelFavoriteList()
        Snackbar.show("已清空所有收藏")
    }

    fun checkUpdate(quiet: Boolean = true) {
        coroutineScope.launch {
            if (!quiet) Snackbar.show("正在检查更新...", leadingLoading = true, duration = 5000)

            delay(3000)
            updateViewModel.checkUpdate(BuildConfig.VERSION_NAME, settingsViewModel.updateChannel)

            if (!quiet) {
                if (updateViewModel.isUpdateAvailable) Snackbar.show("发现新版本: v${updateViewModel.latestRelease.version}")
                else Snackbar.show("当前已是最新版本")
            }

            if (!updateViewModel.isUpdateAvailable) return@launch
            if (settingsViewModel.appLastLatestVersion == updateViewModel.latestRelease.version) return@launch

            settingsViewModel.appLastLatestVersion = updateViewModel.latestRelease.version
            if (settingsViewModel.updateForceRemind) {
                navController.navigateSingleTop(Destination.Update)
            } else {
                if (quiet) Snackbar.show("发现新版本: v${updateViewModel.latestRelease.version}")
            }
        }
    }

    fun reload() {
        settingsViewModel.refresh()
        mainViewModel.init()
        navController.navigateUp()
        navController.navigateSingleTop(Destination.Loading)
    }

    /**
     * 直播页返回：优先回到进入直播前的页面；如果是“启动直接进直播”，
     * 栈里只有 Loading，就回主页 Dashboard，而不是直接退出应用。
     */
    fun backFromLive() {
        val previous = navController.previousBackStackEntry?.destination
        val previousRoute = previous?.route?.substringBefore('?')
        if (previous == null || previousRoute == "Loading" || previousRoute == "Agreement") {
            navController.navigateSingleTop(Destination.Dashboard)
        } else {
            navController.navigateUp()
        }
    }

    BrandBackground(modifier = modifier) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = if (settingsViewModel.appAgreementAgreed) Destination.Loading else Destination.Agreement,
            builder = {
                composable<Destination.Agreement> {
                    AgreementScreen(
                        onAgree = {
                            settingsViewModel.appAgreementAgreed = true
                            navController.navigateSingleTop(Destination.Loading)
                        },
                        onDisagree = onBackPressed,
                        onTouchTested = { settingsViewModel.uiFocusOptimize = false },
                    )
                }

                composable<Destination.Loading> {
                    LoadingScreen(
                        mainUiState = uiState,
                        toDashboardScreen = {
                            navController.navigateUp()
                            navController.navigateSingleTop(Destination.fromStoredName(settingsViewModel.appStartupScreen))
                            checkUpdate()
                        },
                        toSettingsScreen = { navController.navigateSingleTop(Destination.Settings()) },
                        onBackPressed = onBackPressed,
                    )
                }

                composable<Destination.Dashboard> {
                    DashboardScreen(
                        currentIptvSourceProvider = { settingsViewModel.iptvSourceCurrent },
                        channelFavoriteListProvider = { settingsViewModel.iptvChannelFavoriteList },
                        onChannelSelected = { onChannelSelected(it) },
                        epgListProvider = epgListProvider,
                        toLiveScreen = { navController.navigateSingleTop(Destination.Live) },
                        toChannelsScreen = { navController.navigateSingleTop(Destination.Channels) },
                        toFavoritesScreen = { navController.navigateSingleTop(Destination.Favorites) },
                        toSearchScreen = { navController.navigateSingleTop(Destination.Search) },
                        toMultiViewScreen = { navController.navigateSingleTop(Destination.MultiView) },
                        toSettingsScreen = { navController.navigateSingleTop(Destination.Settings()) },
                        toAboutScreen = { navController.navigateSingleTop(Destination.About) },
                        toSettingsIptvSourceScreen = {
                            navController.navigateSingleTop(
                                Destination.Settings(SettingsSubCategories.IPTV_SOURCE.name)
                            )
                        },
                        onReload = { reload() },
                        onBackPressed = onBackPressed,
                    )
                }

                composable<Destination.Live> {
                    BackHandler { backFromLive() }

                    key(settingsViewModel.videoPlayerCore) {
                        com.doubao.tv.feature.live.ui.main.components.MainContent(
                            filteredChannelGroupListProvider = filteredChannelGroupListProvider,
                            favoriteChannelListProvider = favoriteChannelListProvider,
                            epgListProvider = epgListProvider,
                            onChannelFavoriteToggle = { onChannelFavoriteToggle(it) },
                            toSettingsScreen = {
                                if (it != null) {
                                    navController.navigateSingleTop(Destination.Settings(it.name))
                                } else {
                                    navController.navigateSingleTop(Destination.Settings())
                                }
                            },
                            toDashboardScreen  ={
                                navController.navigateSingleTop(Destination.Dashboard)
                            },
                            onBackPressed = { backFromLive() },
                        )
                    }
                }

                composable<Destination.Channels> {
                    ChannelsScreen(
                        channelGroupListProvider = filteredChannelGroupListProvider,
                        onChannelSelected = { onChannelSelected(it) },
                        onChannelFavoriteToggle = { onChannelFavoriteToggle(it) },
                        epgListProvider = epgListProvider,
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.Favorites> {
                    FavoritesScreen(
                        channelFavoriteListProvider = { settingsViewModel.iptvChannelFavoriteList },
                        onChannelSelected = { onChannelSelected(it) },
                        onChannelFavoriteToggle = { onChannelFavoriteToggle(it) },
                        onChannelFavoriteClear = { onChannelFavoriteClear() },
                        epgListProvider = epgListProvider,
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.Search> {
                    SearchScreen(
                        channelGroupListProvider = filteredChannelGroupListProvider,
                        onChannelSelected = { onChannelSelected(it) },
                        onChannelFavoriteToggle = { onChannelFavoriteToggle(it) },
                        epgListProvider = epgListProvider,
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.Settings> { backStackEntry ->
                    val destination = backStackEntry.toRoute<Destination.Settings>()
                    SettingsScreen(
                        startDestinationProvider = { destination.startDestination },
                        channelGroupListProvider = channelGroupListProvider,
                        onCheckUpdate = { checkUpdate(false) },
                        onReload = { reload() },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.About> {
                    AboutScreen(
                        latestVersionProvider = { updateViewModel.latestRelease.version },
                        toUpdateScreen = { navController.navigateSingleTop(Destination.Update) },
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.Update> {
                    UpdateScreen(
                        onBackPressed = { navController.navigateUp() },
                    )
                }

                composable<Destination.MultiView> {
                    MultiViewScreen(
                        channelGroupListProvider = filteredChannelGroupListProvider,
                        epgListProvider = epgListProvider,
                        onBackPressed = { navController.navigateUp() },
                    )
                }
            },
        )
    }
}