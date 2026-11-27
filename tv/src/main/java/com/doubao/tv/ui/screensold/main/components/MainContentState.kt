package com.doubao.tv.ui.screensold.main.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.doubao.tv.core.data.entities.channel.Channel
import com.doubao.tv.core.data.entities.channel.ChannelGroupList
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelFirstOrNull
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelGroupIdx
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelIdx
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelLastOrNull
import com.doubao.tv.core.data.entities.channel.ChannelGroupList.Companion.channelList
import com.doubao.tv.core.data.entities.channel.ChannelLine
import com.doubao.tv.core.data.entities.channel.ChannelLineList
import com.doubao.tv.core.data.entities.channel.ChannelList
import com.doubao.tv.core.data.entities.epg.EpgProgramme
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserve
import com.doubao.tv.core.data.entities.epg.EpgProgrammeReserveList
import com.doubao.tv.core.data.utils.ChannelUtil
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.data.utils.Loggable
import com.doubao.tv.core.util.utils.urlHost
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.screen.settings.SettingsViewModel
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.screensold.videoplayer.VideoPlayerState
import com.doubao.tv.ui.screensold.videoplayer.player.VideoPlayer
import com.doubao.tv.ui.screensold.videoplayer.rememberVideoPlayerState
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Stable
class MainContentState(
    private val coroutineScope: CoroutineScope,
    private val videoPlayerState: VideoPlayerState,
    private val channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    private val favoriteChannelListProvider: () -> ChannelList = { ChannelList() },
    private val settingsViewModel: SettingsViewModel,
) : Loggable("MainContentState") {
    private var _currentChannel by mutableStateOf(Channel())
    val currentChannel get() = _currentChannel

    private var _currentChannelLineIdx by mutableIntStateOf(0)
    val currentChannelLineIdx get() = _currentChannelLineIdx

    val currentChannelLine get() = _currentChannel.lineList[_currentChannelLineIdx]

    /** 本次播放已自动换线次数，切换频道或播放成功后清零 */
    private var _autoSwitchLineCount by mutableIntStateOf(0)
    private var _lastAutoSwitchLineAt by mutableStateOf(0L)

    private var _currentPlaybackEpgProgramme by mutableStateOf<EpgProgramme?>(null)
    val currentPlaybackEpgProgramme get() = _currentPlaybackEpgProgramme

    private var _tempChannelScreenHideJob: Job? = null

    private var _isTempChannelScreenVisible by mutableStateOf(false)
    var isTempChannelScreenVisible
        get() = _isTempChannelScreenVisible
        set(value) {
            _isTempChannelScreenVisible = value
        }

    private var _isChannelScreenVisible by mutableStateOf(false)
    var isChannelScreenVisible
        get() = _isChannelScreenVisible
        set(value) {
            _isChannelScreenVisible = value
        }

    private var _isVideoPlayerControllerScreenVisible by mutableStateOf(false)
    var isVideoPlayerControllerScreenVisible
        get() = _isVideoPlayerControllerScreenVisible
        set(value) {
            _isVideoPlayerControllerScreenVisible = value
        }

    private var _isQuickOpScreenVisible by mutableStateOf(false)
    var isQuickOpScreenVisible
        get() = _isQuickOpScreenVisible
        set(value) {
            _isQuickOpScreenVisible = value
        }

    private var _isEpgScreenVisible by mutableStateOf(false)
    var isEpgScreenVisible
        get() = _isEpgScreenVisible
        set(value) {
            _isEpgScreenVisible = value
        }

    private var _isChannelLineScreenVisible by mutableStateOf(false)
    var isChannelLineScreenVisible
        get() = _isChannelLineScreenVisible
        set(value) {
            _isChannelLineScreenVisible = value
        }

    private var _isVideoPlayerDisplayModeScreenVisible by mutableStateOf(false)
    var isVideoPlayerDisplayModeScreenVisible
        get() = _isVideoPlayerDisplayModeScreenVisible
        set(value) {
            _isVideoPlayerDisplayModeScreenVisible = value
        }

    private var _isVideoTracksScreenVisible by mutableStateOf(false)
    var isVideoTracksScreenVisible
        get() = _isVideoTracksScreenVisible
        set(value) {
            _isVideoTracksScreenVisible = value
        }

    private var _isAudioTracksScreenVisible by mutableStateOf(false)
    var isAudioTracksScreenVisible
        get() = _isAudioTracksScreenVisible
        set(value) {
            _isAudioTracksScreenVisible = value
        }

    private var _isSubtitleTracksScreenVisible by mutableStateOf(false)
    var isSubtitleTracksScreenVisible
        get() = _isSubtitleTracksScreenVisible
        set(value) {
            _isSubtitleTracksScreenVisible = value
        }

    init {
        val channelGroupList = channelGroupListProvider()

        changeCurrentChannel(settingsViewModel.iptvChannelLastPlay.isEmptyOrElse {
            channelGroupList.channelFirstOrNull() ?: Channel.EMPTY
        })

        videoPlayerState.onReady {
            settingsViewModel.iptvChannelLinePlayableUrlList += currentChannelLine.url
            settingsViewModel.iptvChannelLinePlayableHostList += currentChannelLine.url.urlHost()
        }

        videoPlayerState.onError {
            if (_currentPlaybackEpgProgramme != null) return@onError

            settingsViewModel.iptvChannelLinePlayableUrlList -= currentChannelLine.url
            settingsViewModel.iptvChannelLinePlayableHostList -= currentChannelLine.url.urlHost()

            if (_currentChannelLineIdx < _currentChannel.lineList.size - 1) {
                changeCurrentChannel(_currentChannel, _currentChannelLineIdx + 1)
            }
        }

        videoPlayerState.onInterrupt {
            changeCurrentChannel(
                _currentChannel,
                _currentChannelLineIdx,
                _currentPlaybackEpgProgramme
            )
        }

        videoPlayerState.onIsBuffering { isBuffering ->
            if (isBuffering) {
                _isTempChannelScreenVisible = true
            } else {
                _tempChannelScreenHideJob?.cancel()
                _tempChannelScreenHideJob = coroutineScope.launch {
                    val name = _currentChannel.name
                    val lineIdx = _currentChannelLineIdx
                    delay(Constants.UI_TEMP_CHANNEL_SCREEN_SHOW_DURATION)
                    if (name == _currentChannel.name && lineIdx == _currentChannelLineIdx) {
                        _isTempChannelScreenVisible = false
                    }
                }
            }
        }
    }

    private fun getPrevFavoriteChannel(): Channel? {
        if (!settingsViewModel.iptvChannelFavoriteListVisible) return null

        val channelGroupList = channelGroupListProvider()
        val favoriteChannelList = favoriteChannelListProvider()

        if (_currentChannel !in favoriteChannelList) return null

        val currentIdx = favoriteChannelList.indexOf(_currentChannel)

        return favoriteChannelList.getOrElse(currentIdx - 1) {
            if (settingsViewModel.iptvChannelChangeListLoop) favoriteChannelList.lastOrNull()
            else channelGroupList.channelLastOrNull()
        }
    }

    private fun getNextFavoriteChannel(): Channel? {
        if (!settingsViewModel.iptvChannelFavoriteListVisible) return null

        val channelGroupList = channelGroupListProvider()
        val favoriteChannelList = favoriteChannelListProvider()

        if (_currentChannel !in favoriteChannelList) return null

        val currentIdx = favoriteChannelList.indexOf(_currentChannel)

        return favoriteChannelList.getOrElse(currentIdx + 1) {
            if (settingsViewModel.iptvChannelChangeListLoop) favoriteChannelList.firstOrNull()
            else channelGroupList.channelFirstOrNull()
        }
    }

    private fun getPrevChannel(): Channel {
        return getPrevFavoriteChannel() ?: run {
            val channelGroupList = channelGroupListProvider()
            return if (settingsViewModel.iptvChannelChangeListLoop) {
                val group =
                    channelGroupList.getOrElse(channelGroupList.channelGroupIdx(_currentChannel)) { channelGroupList.first() }
                val currentIdx = group.channelList.indexOf(_currentChannel)
                group.channelList.getOrElse(currentIdx - 1) { group.channelList.last() }
            } else {
                val currentIdx = channelGroupList.channelIdx(_currentChannel)
                channelGroupList.channelList.getOrElse(currentIdx - 1) {
                    channelGroupList.channelLastOrNull() ?: Channel()
                }
            }
        }
    }

    private fun getNextChannel(): Channel {
        return getNextFavoriteChannel() ?: run {
            val channelGroupList = channelGroupListProvider()
            return if (settingsViewModel.iptvChannelChangeListLoop) {
                val group =
                    channelGroupList.getOrElse(channelGroupList.channelGroupIdx(_currentChannel)) { channelGroupList.first() }
                val currentIdx = group.channelList.indexOf(_currentChannel)
                group.channelList.getOrElse(currentIdx + 1) { group.channelList.first() }
            } else {
                val currentIdx = channelGroupList.channelIdx(_currentChannel)
                channelGroupList.channelList.getOrElse(currentIdx + 1) {
                    channelGroupList.channelFirstOrNull() ?: Channel()
                }
            }
        }
    }

    private fun getLineIdx(lineList: ChannelLineList, lineIdx: Int? = null): Int {
        val idx = if (lineIdx == null) {
            val idx = lineList.indexOfFirst { line ->
                settingsViewModel.iptvChannelLinePlayableUrlList.contains(line.url)
            }

            if (idx < 0) {
                lineList.indexOfFirst { line ->
                    settingsViewModel.iptvChannelLinePlayableHostList.contains(line.url.urlHost())
                }
            } else idx
        } else (lineIdx + lineList.size) % lineList.size

        return max(0, min(idx, lineList.size - 1))
    }

    fun changeCurrentChannel(
        channel: Channel,
        lineIdx: Int? = null,
        playbackEpgProgramme: EpgProgramme? = null,
    ) {
        settingsViewModel.iptvChannelLastPlay = channel

        if (channel != _currentChannel) _autoSwitchLineCount = 0

        if (channel == _currentChannel && lineIdx == _currentChannelLineIdx && playbackEpgProgramme == _currentPlaybackEpgProgramme) return

        if (channel == _currentChannel && lineIdx != _currentChannelLineIdx) {
            settingsViewModel.iptvChannelLinePlayableUrlList -= currentChannelLine.url
            settingsViewModel.iptvChannelLinePlayableHostList -= currentChannelLine.url.urlHost()
        }

        _isTempChannelScreenVisible = true

        _currentChannel = channel
        _currentChannelLineIdx = getLineIdx(_currentChannel.lineList, lineIdx)

        _currentPlaybackEpgProgramme = playbackEpgProgramme

        var url = currentChannelLine.playableUrl
        if (_currentPlaybackEpgProgramme != null) {
            val timeFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            val query = listOf(
                "playseek=",
                timeFormat.format(_currentPlaybackEpgProgramme!!.startAt),
                "-",
                timeFormat.format(_currentPlaybackEpgProgramme!!.endAt),
            ).joinToString("")
            url = if (URI(url).query.isNullOrBlank()) "$url?$query" else "$url&$query"
            url = ChannelUtil.urlToCanPlayback(url)
        }
        val line = currentChannelLine.copy(url = url)

        log.d("播放${_currentChannel.name}（${_currentChannelLineIdx + 1}/${_currentChannel.lineList.size}）: $line")

        if (line.url.startsWith("webview://")) {
            videoPlayerState.metadata = VideoPlayer.Metadata()
            videoPlayerState.stop()
        } else {
            videoPlayerState.prepare(line)
        }
    }

    /** 播放成功，允许下次重新自动换线 */
    fun resetAutoSwitchLine() {
        _autoSwitchLineCount = 0
    }

    /**
     * 播放失败时自动切到下一条线路。
     *
     * 规则：同一频道内按顺序轮换线路，轮完一遍还没成功就停在错误提示，
     * 交给用户手动换源或换台，避免无限重试把设备拖死。
     */
    fun handlePlayerError() {
        val size = _currentChannel.lineList.size
        if (size <= 1) return

        val now = System.currentTimeMillis()
        if (now - _lastAutoSwitchLineAt < 1000) return

        if (_autoSwitchLineCount >= size - 1) {
            log.d("线路全部不可用：${_currentChannel.name}")
            _autoSwitchLineCount = 0
            return
        }

        _lastAutoSwitchLineAt = now
        _autoSwitchLineCount += 1

        val nextIdx = (_currentChannelLineIdx + 1) % size
        log.d("播放失败，自动换线到 ${_currentChannel.name} 第 ${nextIdx + 1}/$size 条")
        Snackbar.show("线路不可用，自动切换到线路 ${nextIdx + 1}/$size")

        changeCurrentChannel(_currentChannel, nextIdx, _currentPlaybackEpgProgramme)
    }

    fun changeCurrentChannelToPrev() {
        changeCurrentChannel(getPrevChannel())
    }

    fun changeCurrentChannelToNext() {
        changeCurrentChannel(getNextChannel())
    }

    fun reverseEpgProgrammeOrNot(channel: Channel, programme: EpgProgramme) {
        val reverse = settingsViewModel.epgChannelReserveList.firstOrNull {
            it.test(channel, programme)
        }

        if (reverse != null) {
            settingsViewModel.epgChannelReserveList =
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList - reverse)
            Snackbar.show("取消预约：${reverse.channel} - ${reverse.programme}")
        } else {
            val newReserve = EpgProgrammeReserve(
                channel = channel.name,
                programme = programme.title,
                startAt = programme.startAt,
                endAt = programme.endAt,
            )

            settingsViewModel.epgChannelReserveList =
                EpgProgrammeReserveList(settingsViewModel.epgChannelReserveList + newReserve)
            Snackbar.show("已预约：${channel.name} - ${programme.title}")
        }
    }

    fun supportPlayback(
        channel: Channel = _currentChannel,
        lineIdx: Int? = _currentChannelLineIdx,
    ): Boolean {
        val currentLineIdx = getLineIdx(channel.lineList, lineIdx)
        return ChannelUtil.urlSupportPlayback(channel.lineList[currentLineIdx].url)
    }
}

@Composable
fun rememberMainContentState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    videoPlayerState: VideoPlayerState = rememberVideoPlayerState(),
    channelGroupListProvider: () -> ChannelGroupList = { ChannelGroupList() },
    favoriteChannelListProvider: () -> ChannelList = { ChannelList() },
    settingsViewModel: SettingsViewModel = settingsVM,
): MainContentState {
    val favoriteChannelListProviderUpdated by rememberUpdatedState(favoriteChannelListProvider)

    return remember(settingsVM.videoPlayerCore) {
        MainContentState(
            coroutineScope = coroutineScope,
            videoPlayerState = videoPlayerState,
            channelGroupListProvider = channelGroupListProvider,
            favoriteChannelListProvider = favoriteChannelListProviderUpdated,
            settingsViewModel = settingsViewModel,
        )
    }
}