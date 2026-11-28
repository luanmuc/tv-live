package com.doubao.tv

import com.doubao.tv.settings.SettingsRepository

/**
 * 轻量服务定位器，先收拢设置仓库；后续可替换为 Hilt/手动构造注入，
 * 避免 ViewModel 和 UI 继续直接依赖 Configs / 全局单例。
 */
object AppContainer {
    val settings: SettingsRepository by lazy { SettingsRepository() }
}
