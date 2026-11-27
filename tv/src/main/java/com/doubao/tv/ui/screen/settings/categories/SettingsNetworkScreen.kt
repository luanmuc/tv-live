package com.doubao.tv.ui.screen.settings.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Text
import com.doubao.tv.core.data.utils.Constants
import com.doubao.tv.core.util.utils.humanizeMs
import com.doubao.tv.ui.screen.settings.components.SettingsCategoryScreen
import com.doubao.tv.ui.screen.settings.components.SettingsListItem
import com.doubao.tv.ui.theme.MyTvTheme

@Composable
fun SettingsNetworkScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
) {
    SettingsCategoryScreen(
        modifier = modifier,
        header = { Text("设置 / 网络") },
        onBackPressed = onBackPressed,
    ) { firstItemFocusRequester ->
        item {
            SettingsListItem(
                modifier = Modifier.focusRequester(firstItemFocusRequester),
                headlineContent = "HTTP请求重试次数",
                supportingContent = "影响直播源、节目单数据获取",
                trailingContent = Constants.NETWORK_RETRY_COUNT.toString(),
                locK = true,
            )
        }

        item {
            SettingsListItem(
                headlineContent = "HTTP请求重试间隔时间",
                supportingContent = "影响直播源、节目单数据获取",
                trailingContent = Constants.NETWORK_RETRY_INTERVAL.humanizeMs(),
                locK = true,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun SettingsNetworkScreenPreview() {
    MyTvTheme {
        SettingsNetworkScreen()
    }
}