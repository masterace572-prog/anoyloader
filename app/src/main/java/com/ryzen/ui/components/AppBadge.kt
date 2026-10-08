package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppTheme

/**
 * Compact status pill with a semantic label and optional status dot.
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
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
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
