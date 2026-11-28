package com.doubao.tv.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.IntRange
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import com.doubao.tv.allinone.AllInOne
import com.doubao.tv.core.data.entities.iptvsource.IptvSource.Companion.needExternalStoragePermission
import com.doubao.tv.core.util.back.BackDispatcher
import com.doubao.tv.ui.back.LocalBackDispatcher
import com.doubao.tv.ui.material.Padding
import com.doubao.tv.ui.material.PopupHandleableApplication
import com.doubao.tv.ui.material.Snackbar
import com.doubao.tv.ui.material.SnackbarType
import com.doubao.tv.ui.material.SnackbarUI
import com.doubao.tv.ui.material.Visibility
import com.doubao.tv.ui.screen.main.MainScreen
import com.doubao.tv.ui.screen.monitor.MonitorPopup
import com.doubao.tv.ui.screen.settings.SettingsViewModel
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.theme.DESIGN_WIDTH
import com.doubao.tv.ui.theme.SAFE_AREA_HORIZONTAL_PADDING
import com.doubao.tv.ui.theme.SAFE_AREA_VERTICAL_PADDING
import com.doubao.tv.ui.tooling.PreviewWithLayoutGrids
import com.doubao.tv.ui.utils.rememberReadExternalStoragePermission
import java.io.File

@Composable
fun App(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = settingsVM,
    onBackPressed: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val doubleBackPressedExitState = rememberDoubleBackPressedExitState()
    val backDispatcher = remember { BackDispatcher() }
    val handleBack = {
        if (doubleBackPressedExitState.allowExit) {
            onBackPressed()
        } else {
            doubleBackPressedExitState.backPress()
            Snackbar.show("再按一次退出")
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density * when (settingsViewModel.uiDensityScaleRatio) {
                0f -> configuration.screenWidthDp.toFloat() / DESIGN_WIDTH
                else -> settingsViewModel.uiDensityScaleRatio
            },
            fontScale = LocalDensity.current.fontScale * settingsViewModel.uiFontScaleRatio,
        ),
        LocalBackDispatcher provides backDispatcher,
    ) {
        BackHandler {
            if (!backDispatcher.dispatch()) handleBack()
        }
        PopupHandleableApplication {
            MainScreen(
                modifier = modifier,
                onBackPressed = handleBack,
            )
        }

        SnackbarUI()
        Visibility({ settingsViewModel.debugShowFps }) { MonitorPopup() }
        Visibility({ settingsViewModel.debugShowLayoutGrids }) { PreviewWithLayoutGrids { } }
    }

    if (settingsViewModel.iptvSourceCurrent.needExternalStoragePermission()) {
        val (hasPermission, requestPermission) = rememberReadExternalStoragePermission()
        LaunchedEffect(Unit) { if (!hasPermission) requestPermission() }
    }

    LaunchedEffect(settingsViewModel.iptvSourceCurrent) {
        if (settingsViewModel.feiyangAllInOneFilePath.isNotBlank()) {
            AllInOne.start(
                context,
                settingsViewModel.feiyangAllInOneFilePath,
                onFail = {
                    Snackbar.show("二进制 启动失败", type = SnackbarType.ERROR)
                },
                onUnsupported = {
                    Snackbar.show("二进制 不支持当前平台", type = SnackbarType.ERROR)
                },
            )
        }
    }
}

/**
 * 退出应用二次确认
 */
class DoubleBackPressedExitState internal constructor(
    @IntRange(from = 0)
    private val resetSeconds: Int,
) {
    private var _allowExit by mutableStateOf(false)
    val allowExit get() = _allowExit

    fun backPress() {
        _allowExit = true
        channel.trySend(resetSeconds)
    }

    private val channel = Channel<Int>(Channel.CONFLATED)

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel.consumeAsFlow()
            .debounce { it.toLong() * 1000 }
            .collect { _allowExit = false }
    }
}

/**
 * 退出应用二次确认状态
 */
@Composable
fun rememberDoubleBackPressedExitState(@IntRange(from = 0) resetSeconds: Int = 2) =
    remember { DoubleBackPressedExitState(resetSeconds = resetSeconds) }
        .also { LaunchedEffect(it) { it.observe() } }

val ParentPadding = PaddingValues(
    vertical = SAFE_AREA_VERTICAL_PADDING.dp,
    horizontal = SAFE_AREA_HORIZONTAL_PADDING.dp,
)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current) =
    remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction),
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction),
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
