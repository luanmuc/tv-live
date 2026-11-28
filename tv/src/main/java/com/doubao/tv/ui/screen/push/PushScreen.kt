package com.doubao.tv.ui.screen.push

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import com.doubao.tv.core.util.utils.actionView
import com.doubao.tv.ui.screen.components.AppScreen
import com.doubao.tv.ui.screen.components.Qrcode
import com.doubao.tv.ui.screen.settings.SettingsViewModel
import com.doubao.tv.ui.screen.settings.settingsVM
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.utils.clickableNoIndication
import com.doubao.tv.utlis.HttpServer

@Composable
fun PushScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = settingsVM,
    onBackPressed: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        while (true) {
            settingsViewModel.refresh()
            delay(1000)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(settingsViewModel.debugDeveloperMode) {
        if (settingsViewModel.debugDeveloperMode) {
            HttpServer.startService(context)
        } else {
            HttpServer.stopService(context)
        }
    }

    AppScreen(
        modifier = modifier,
        header = { Text("数据推送") },
        canBack = true,
        onBackPressed = onBackPressed,
    ) {
        PushContent(enabled = settingsViewModel.debugDeveloperMode)
    }
}

@Composable
fun PushContent(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
) {
    val context = LocalContext.current
    val serverUrl = HttpServer.serverUrl(context)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!enabled) {
                Text("远程配置服务默认关闭")
                Spacer(Modifier.height(12.dp))
                Text("请在「设置 → 调试」中开启开发者模式后重新进入本页")
            } else {
                Qrcode(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                        .clickableNoIndication { context.actionView(serverUrl) },
                    text = serverUrl,
                )

                Spacer(Modifier.height(20.dp))
                Text("服务已启动：${serverUrl}")
                Text("请扫描二维码或输入IP地址进行连接")
            }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun PushScreenPreview() {
    MyTvTheme {
        PushScreen()
    }
}