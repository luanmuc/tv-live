package com.doubao.tv.ui.screen.crash

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.doubao.tv.ui.rememberChildPadding
import com.doubao.tv.ui.screen.components.AppScaffoldHeaderBtn
import com.doubao.tv.ui.screen.components.AppScreen
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.utils.focusOnLaunched
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CrashHandlerScreen(
    modifier: Modifier = Modifier,
    errorMessage: String,
    errorStacktrace: String = "",
    onRestart: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val childPaddings = rememberChildPadding()

    AppScreen(
        modifier = modifier.padding(childPaddings.copy(top = 10.dp).paddingValues),
        header = { Text(text = "应用崩溃了") },
        headerExtra = {
            AppScaffoldHeaderBtn(
                modifier = Modifier.focusOnLaunched(),
                title = "重启",
                imageVector = Icons.Default.RestartAlt,
                onSelect = onRestart,
            )
        },
        onBackPressed = onBackPressed,
    ) {
        LazyColumn {
            item { Text(text = "设备ID: ${com.doubao.tv.core.data.utils.Globals.deviceId}") }

            item {
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Text(text = "崩溃时间：${timeFormat.format(System.currentTimeMillis())}")
            }

            item { Text(errorMessage) }
            item { Text(errorStacktrace) }
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun CrashHandlerScreenPreview() {
    MyTvTheme {
        CrashHandlerScreen(
            errorMessage = "ChannelsChannelItem should not be used directly",
            errorStacktrace = """
                java.lang.IllegalStateException: ChannelsChannelItem should not be used directly
            """.trimIndent().repeat(100),
        )
    }
}