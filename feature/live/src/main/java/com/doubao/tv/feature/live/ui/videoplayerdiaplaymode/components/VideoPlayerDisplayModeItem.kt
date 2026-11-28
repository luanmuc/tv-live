package com.doubao.tv.feature.live.ui.videoplayerdiaplaymode.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.doubao.tv.settings.VideoPlayerDisplayMode
import com.doubao.tv.ui.theme.MyTvTheme
import com.doubao.tv.ui.utils.focusOnLaunchedSaveable
import com.doubao.tv.ui.utils.handleKeyEvents
import com.doubao.tv.ui.utils.ifElse

@Composable
fun VideoPlayerDisplayModeItem(
    modifier: Modifier = Modifier,
    displayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    isSelectedProvider: () -> Boolean = { false },
    onSelected: () -> Unit = {},
) {
    val displayMode = displayModeProvider()
    val isSelected = isSelectedProvider()

    ListItem(
        modifier = modifier
            .ifElse(isSelected, Modifier.focusOnLaunchedSaveable())
            .handleKeyEvents(onSelect = onSelected),
        selected = false,
        onClick = {},
        headlineContent = { Text(displayMode.label) },
        trailingContent = {
            RadioButton(selected = isSelected, onClick = {})
        },
    )
}

@Preview
@Composable
private fun VideoPlayerDisplayModeItemPreview() {
    MyTvTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            VideoPlayerDisplayModeItem(
                displayModeProvider = { VideoPlayerDisplayMode.ORIGINAL },
                isSelectedProvider = { true },
            )

            VideoPlayerDisplayModeItem(
                displayModeProvider = { VideoPlayerDisplayMode.ORIGINAL },
            )
        }
    }
}