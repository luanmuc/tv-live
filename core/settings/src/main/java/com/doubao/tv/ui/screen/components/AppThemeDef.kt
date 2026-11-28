package com.doubao.tv.ui.screen.components

import kotlinx.serialization.Serializable

@Serializable
data class AppThemeDef(
    val name: String,
    val background: String,
    val texture: String? = null,
    val textureAlpha: Float? = null,
)

