package com.doubao.tv.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

/**
 * 品牌背景：深青底色 + 左上薄荷光晕 + 右下珊瑚暖光 + 极淡扫描线。
 *
 * 灵感来自图标里那台复古电视的屏幕质感——不做抢眼的装饰，
 * 只在纯色底上给一点点「显像管」的呼吸感，保证文字与海报仍然最清楚。
 */
@Composable
fun BrandBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val surfaceLow = MaterialTheme.colors.surfaceContainerLow
    val isDark = colorScheme.background.luminance() < 0.5f
    val scanlineAlpha = if (isDark) 0.012f else 0.018f
    val vignetteAlpha = if (isDark) 0.42f else 0.07f

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 底色：上深下浅的轻微垂直渐变
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background,
                            surfaceLow,
                        ),
                    )
                )
                // 左上：图标背景的薄荷青
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.secondary.copy(alpha = if (isDark) 0.16f else 0.20f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.10f),
                        radius = size.maxDimension * 0.62f,
                    )
                )
                // 右下：播放键的珊瑚橙
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = if (isDark) 0.14f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.94f, size.height * 0.98f),
                        radius = size.maxDimension * 0.55f,
                    )
                )
                // 极淡扫描线
                val step = 4.dp.toPx()
                val lineColor = Color.White.copy(alpha = scanlineAlpha)
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                    y += step
                }
                // 四周暗角
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.72f,
                    )
                )
            },
        content = content,
    )
}
