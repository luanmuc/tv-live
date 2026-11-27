package com.doubao.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import com.doubao.tv.core.designsystem.theme.Colors
import com.doubao.tv.core.designsystem.theme.LocalColors
import com.doubao.tv.core.designsystem.theme.darkColors
import com.doubao.tv.core.designsystem.theme.lightColors

/**
 * 与「复古奶油小电视」图标同源的圆角体系：
 * 机身的圆润感映射到卡片、弹窗与焦点态，避免电视端生硬的直角。
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun MyTvTheme(
    isInDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isInDarkTheme) colorSchemeForDarkMode else colorSchemeForLightMode
    val colors = if (isInDarkTheme) darkColors else lightColors

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalColors provides colors,
        ) {
            content()
        }
    }
}

val MaterialTheme.colors: Colors
    @Composable
    get() = LocalColors.current

const val SAFE_AREA_HORIZONTAL_PADDING = 58
const val SAFE_AREA_VERTICAL_PADDING = 24
const val LAYOUT_GRID_SPACING = 20
const val LAYOUT_GRID_WIDTH = 52
const val LAYOUT_GRID_COLUMNS = 12
const val DESIGN_WIDTH = 960
const val DESIGN_HEIGHT = 540
