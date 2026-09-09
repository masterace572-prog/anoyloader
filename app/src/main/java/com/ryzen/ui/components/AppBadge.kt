package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppTheme

/**
 * Status as plain semantic text with a 2dp left rule — never a colored capsule.
 * BadgeTone retained so existing call sites keep compiling.
 */
enum class BadgeTone {
    ACCENT,
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL
}

@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    showDot: Boolean = true,
    @Suppress("UNUSED_PARAMETER") leadingIcon: ImageVector? = null
) {
    val color = when (tone) {
        BadgeTone.ACCENT -> AppTheme.colors.accent
        BadgeTone.SUCCESS -> AppTheme.colors.success
        BadgeTone.WARNING -> AppTheme.colors.warning
        BadgeTone.ERROR -> AppTheme.colors.error
        BadgeTone.NEUTRAL -> AppTheme.colors.textSecondary
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(color)
            )
        }
        Text(
            text = text,
            style = AppTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = if (showDot) 8.dp else 0.dp)
        )
    }
}
