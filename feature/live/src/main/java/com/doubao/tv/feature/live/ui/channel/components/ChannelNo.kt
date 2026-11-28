package com.doubao.tv.feature.live.ui.channel.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.doubao.tv.ui.theme.MyTvTheme

@Composable
fun ChannelNumber(
    modifier: Modifier = Modifier,
    channelNumberProvider: () -> String = { "" },
) {
    Text(
        modifier = modifier,
        text = channelNumberProvider(),
        style = MaterialTheme.typography.displayMedium,
    )
}

@Preview
@Composable
private fun ChannelNumberPreview() {
    MyTvTheme {
        ChannelNumber(channelNumberProvider = { "01" })
    }
}