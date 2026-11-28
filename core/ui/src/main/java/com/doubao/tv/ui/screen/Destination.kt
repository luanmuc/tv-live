package com.doubao.tv.ui.screen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 类型安全导航目的地。
 *
 * 外层 MainScreen 使用这些 @Serializable 目的地；设置页内部仍保留自己的
 * 字符串子路由，后续再单独迁移。
 */
@Serializable
sealed interface Destination {
    @Serializable
    @SerialName("Agreement")
    data object Agreement : Destination

    @Serializable
    @SerialName("Loading")
    data object Loading : Destination

    @Serializable
    @SerialName("Dashboard")
    data object Dashboard : Destination

    @Serializable
    @SerialName("Live")
    data object Live : Destination

    @Serializable
    @SerialName("Channels")
    data object Channels : Destination

    @Serializable
    @SerialName("Favorites")
    data object Favorites : Destination

    @Serializable
    @SerialName("Search")
    data object Search : Destination

    @Serializable
    @SerialName("Push")
    data object Push : Destination

    @Serializable
    @SerialName("Settings")
    data class Settings(val startDestination: String? = null) : Destination

    @Serializable
    @SerialName("About")
    data object About : Destination

    @Serializable
    @SerialName("Update")
    data object Update : Destination

    @Serializable
    @SerialName("MultiView")
    data object MultiView : Destination

    companion object {
        /** 把 Configs 里历史保存的 Screens 名称转成合法目的地，非法值回退 Loading。 */
        fun fromStoredName(name: String?): Destination =
            when (name?.substringBefore('/')) {
                Screens.Live.name -> Live
                Screens.Dashboard.name -> Dashboard
                Screens.Agreement.name -> Agreement
                Screens.Channels.name -> Channels
                Screens.Favorites.name -> Favorites
                Screens.Search.name -> Search
                Screens.Push.name -> Push
                Screens.About.name -> About
                Screens.Update.name -> Update
                Screens.MultiView.name -> MultiView
                else -> Loading
            }
    }
}
