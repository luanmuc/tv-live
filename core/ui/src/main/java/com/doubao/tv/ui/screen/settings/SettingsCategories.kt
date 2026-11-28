package com.doubao.tv.ui.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class SettingsCategories(
    val icon: ImageVector,
    val title: String,
) {
    APP(Icons.Outlined.Devices, "应用"),
    IPTV(Icons.Outlined.LiveTv, "直播源"),
    EPG(Icons.AutoMirrored.Outlined.LibraryBooks, "节目单"),
    UI(Icons.Outlined.DisplaySettings, "界面"),
    CONTROL(Icons.Outlined.ControlCamera, "控制"),
    VIDEO_PLAYER(Icons.Outlined.SmartDisplay, "播放器"),
    UPDATE(Icons.Outlined.Update, "更新"),

    // FAVORITE(Icons.Outlined.FavoriteBorder, "收藏"),
    // EPG_RESERVE(Icons.Default.BookmarkBorder, "预约"),
    NETWORK(Icons.Outlined.Wifi, "网络"),
    THEME(Icons.Outlined.ColorLens, "主题"),
    LOG(Icons.AutoMirrored.Outlined.FormatListBulleted, "日志"),
    PERMISSIONS(Icons.Outlined.Shield, "权限"),
}
