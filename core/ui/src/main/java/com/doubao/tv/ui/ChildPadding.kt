package com.doubao.tv.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.doubao.tv.ui.material.Padding
import com.doubao.tv.ui.theme.SAFE_AREA_HORIZONTAL_PADDING
import com.doubao.tv.ui.theme.SAFE_AREA_VERTICAL_PADDING

val ParentPadding = PaddingValues(
    vertical = SAFE_AREA_VERTICAL_PADDING.dp,
    horizontal = SAFE_AREA_HORIZONTAL_PADDING.dp,
)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current) =
    remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction),
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction),
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
