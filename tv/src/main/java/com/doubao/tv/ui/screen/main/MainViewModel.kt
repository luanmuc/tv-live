package com.doubao.tv.ui.screen.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.doubao.tv.core.data.entities.channel.ChannelFavoriteList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelList
import com.doubao.tv.core.data.entities.channel.ChannelLineList
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.AppContainer
import com.doubao.tv.core.data.entities.epg.EpgList
import com.doubao.tv.core.data.entities.epg.EpgList.Companion.match
import com.doubao.tv.core.data.entities.epgsource.EpgSource
import com.doubao.tv.core.data.entities.iptvsource.IptvSource
import com.doubao.tv.core.data.network.HttpException
import com.doubao.tv.core.data.repositories.epg.EpgRepository
import com.doubao.tv.core.data.repositories.iptv.IptvRepository
import com.doubao.tv.core.data.utils.ChannelAlias
import com.doubao.tv.core.data.utils.ChannelUtil
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.data.utils.Logger
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.material.SnackbarType
import com.doubao.tv.ui.utils.Configs
import java.util.Calendar

class MainViewModel : ViewModel() {
    private val log = Logger.create("MainViewModel")

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var _lastJob: Job? = null

    private val _refreshRequests = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val refreshRequests: SharedFlow<Unit> = _refreshRequests.asSharedFlow()

    private fun requestRefresh() {
        _refreshRequests.tryEmit(Unit)
    }

    init {
        viewModelScope.launch {
            init()
            _lastJob?.join()
            refreshOtherIptvSource()
        }
    }

    fun init() {
        _lastJob?.cancel()
        _lastJob = viewModelScope.launch {
            ChannelAlias.refresh()
            refreshChannel()
            refreshEpg()
            mergeEpgMetadata()
        }
    }

    private suspend fun refreshChannel() {
        _uiState.value = MainUiState.Loading("加载直播源")

        flow {
            emit(
                IptvRepository(AppContainer.settings.current.iptvSourceCurrent)
                    .getChannelGroupList(AppContainer.settings.current.iptvSourceCacheTime)
            )
        }
            .retryWhen { e, attempt ->
                if (attempt >= Constants.NETWORK_RETRY_COUNT) return@retryWhen false
                if (e !is HttpException) return@retryWhen false

                _uiState.value =
                    MainUiState.Loading("加载直播源(${attempt + 1}/${Constants.NETWORK_RETRY_COUNT})...")
                delay(Constants.NETWORK_RETRY_INTERVAL)
                true
            }
            .catch {
                _uiState.value = MainUiState.Error(it.message)
            }
            .map { mergeSimilarChannel(it) }
            .map { hybridChannel(it) }
            .map { groupList ->
                _uiState.value = MainUiState.Ready(
                    channelGroupList = groupList,
                    filteredChannelGroupList = withContext(Dispatchers.Default) {
                        ChannelGroupList(groupList.filter { it.name !in AppContainer.settings.current.iptvChannelGroupHiddenList })
                            .withMetadata()
                    }
                )
                groupList
            }
            .map { refreshChannelFavoriteList(AppContainer.settings.current.iptvSourceCurrent, it) }
            .collect()
    }

    private suspend fun mergeSimilarChannel(channelGroupList: ChannelGroupList): ChannelGroupList =
        withContext(Dispatchers.Default) {
            if (!AppContainer.settings.current.iptvSimilarChannelMerge) return@withContext channelGroupList

            _uiState.value = MainUiState.Loading("合并相似频道")

            return@withContext ChannelGroupList(channelGroupList.map { group ->
                group.copy(
                    channelList = ChannelList(group.channelList
                        .groupBy { channel -> channel.standardName }
                        .map { (standardName, similarChannels) ->
                            if (similarChannels.size == 1) {
                                similarChannels.first()
                            } else {
                                val firstChannel = similarChannels.first()
                                val mergedLineList = similarChannels
                                    .asSequence()
                                    .flatMap { channel ->
                                        channel.lineList.asSequence().map { line ->
                                            line.copy(name = channel.name)
                                        }
                                    }
                                    .distinctBy { it.url }
                                    .toList()

                                firstChannel.copy(
                                    name = standardName,
                                    lineList = ChannelLineList(mergedLineList)
                                )
                            }
                        }
                    )
                )
            })
        }

    private suspend fun hybridChannel(channelGroupList: ChannelGroupList) =
        withContext(Dispatchers.Default) {
            if (AppContainer.settings.current.iptvHybridMode != Configs.IptvHybridMode.DISABLE) {
                _uiState.value = MainUiState.Loading("混合直播源")
            }

            return@withContext when (AppContainer.settings.current.iptvHybridMode) {
                Configs.IptvHybridMode.DISABLE -> channelGroupList
                Configs.IptvHybridMode.IPTV_FIRST -> {
                    ChannelGroupList(channelGroupList.map { group ->
                        group.copy(channelList = ChannelList(group.channelList.map { channel ->
                            channel.copy(
                                lineList = ChannelLineList(
                                    channel.lineList + ChannelUtil.getHybridWebViewLines(channel.epgName)
                                )
                            )
                        }))
                    })
                }

                Configs.IptvHybridMode.HYBRID_FIRST -> {
                    ChannelGroupList(channelGroupList.map { group ->
                        group.copy(channelList = ChannelList(group.channelList.map { channel ->
                            channel.copy(
                                lineList = ChannelLineList(
                                    ChannelUtil.getHybridWebViewLines(channel.epgName) + channel.lineList
                                )
                            )
                        }))
                    })
                }
            }
        }

    private suspend fun refreshEpg() {
        if (!AppContainer.settings.current.epgEnable) return

        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < AppContainer.settings.current.epgRefreshTimeThreshold) {
            val threshold = AppContainer.settings.current.epgRefreshTimeThreshold.toString().padStart(2, '0') + ":00"
            log.i("当前时间未到${threshold}，不获取节目单")
            return
        }


        if (_uiState.value is MainUiState.Ready) {
            EpgList.clearCache()
            val channelGroupList = (_uiState.value as MainUiState.Ready).channelGroupList

            flow {
                val epgSource = AppContainer.settings.current.epgSourceFollowIptv
                    .takeIf { it }
                    ?.let {
                        val iptvRepository = IptvRepository(AppContainer.settings.current.iptvSourceCurrent)
                        iptvRepository.getEpgUrl()?.let { epgUrl ->
                            EpgSource(
                                name = AppContainer.settings.current.iptvSourceCurrent.name,
                                url = epgUrl,
                            )
                        }
                    } ?: AppContainer.settings.current.epgSourceCurrent

                emit(EpgRepository(epgSource).getEpgList())
            }
                .retryWhen { e, attempt ->
                    if (attempt >= Constants.NETWORK_RETRY_COUNT) return@retryWhen false
                    if (e !is HttpException) return@retryWhen false

                    delay(Constants.NETWORK_RETRY_INTERVAL)
                    true
                }
                .catch {
                    emit(EpgList())
                    Snackbar.show("节目单获取失败，请检查网络连接", type = SnackbarType.ERROR)
                }
                .map { epgList ->
                    withContext(Dispatchers.Default) {
                        val filteredChannels =
                            (channelGroupList.channelList + AppContainer.settings.current.iptvChannelFavoriteList.map { it.channel }).map { it.epgName }

                        EpgList(epgList.filter { epg -> epg.channelList.any { it in filteredChannels } })
                    }
                }
                .map { epgList ->
                    _uiState.value = (_uiState.value as MainUiState.Ready).copy(epgList = epgList)
                }
                .collect()
        }
    }

    private suspend fun mergeEpgMetadata() = withContext(Dispatchers.Default) {
        if (_uiState.value is MainUiState.Ready) {
            val channelGroupList = (_uiState.value as MainUiState.Ready).channelGroupList
            val epgList = (_uiState.value as MainUiState.Ready).epgList

            if (epgList.all { epg -> epg.logo == null }) return@withContext

            _uiState.value = (_uiState.value as MainUiState.Ready).copy(
                channelGroupList = ChannelGroupList(channelGroupList.map { group ->
                    group.copy(channelList = ChannelList(group.channelList.map { channel ->
                        channel.copy(
                            logo = epgList.match(channel)?.logo ?: channel.logo
                        )
                    }))
                }),
                epgList = epgList,
            )
        }
    }

    private suspend fun refreshOtherIptvSource() {
        val needRefreshNames = AppContainer.settings.current.iptvChannelFavoriteList.map { it.iptvSourceName }.distinct()
            .filter { it != AppContainer.settings.current.iptvSourceCurrent.name }

        (Constants.IPTV_SOURCE_LIST + AppContainer.settings.current.iptvSourceList)
            .filter { it.name in needRefreshNames }
            .forEach { iptvSource ->
                runCatching {
                    val channelGroupList =
                        IptvRepository(iptvSource).getChannelGroupList(AppContainer.settings.current.iptvSourceCacheTime)

                    refreshChannelFavoriteList(iptvSource, channelGroupList)
                }
            }
    }

    private suspend fun refreshChannelFavoriteList(
        iptvSource: IptvSource,
        channelGroupList: ChannelGroupList,
    ) = withContext(Dispatchers.Default) {
        AppContainer.settings.update { snapshot ->
            snapshot.copy(
                iptvChannelFavoriteList = ChannelFavoriteList(
                    snapshot.iptvChannelFavoriteList.map { channelFavorite ->
                        if (iptvSource.name != channelFavorite.iptvSourceName) return@map channelFavorite

                        val newChannel = channelGroupList
                            .firstOrNull { group -> group.name == channelFavorite.groupName }?.channelList
                            ?.firstOrNull { channel -> channel.name == channelFavorite.channel.name }

                        channelFavorite.copy(channel = newChannel ?: channelFavorite.channel)
                    },
                ),
            )
        }
        requestRefresh()
    }

}

sealed interface MainUiState {
    data class Loading(val message: String? = null) : MainUiState
    data class Error(val message: String? = null) : MainUiState
    data class Ready(
        val channelGroupList: ChannelGroupList = ChannelGroupList(),
        val filteredChannelGroupList: ChannelGroupList = ChannelGroupList(),
        val epgList: EpgList = EpgList(),
    ) : MainUiState
}

val mainVM: MainViewModel
    @Composable get() = viewModel<MainViewModel>()