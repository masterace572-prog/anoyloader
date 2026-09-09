package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =====================================================
// SPACING — 8dp grid: 4, 8, 12, 16, 20, 24, 32, 40, 48
// =====================================================

@Immutable
data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val section: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val massive: Dp = 48.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenHorizontalCompact: Dp = 16.dp,
    val listItemVertical: Dp = 16.dp,
    val minTouch: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
