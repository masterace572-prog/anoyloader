package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

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
    leadingIcon: ImageVector? = null
) {
    val containerColor = AppTheme.colors.surfaceElevated
    val borderColor = AppTheme.colors.borderSubtle

    val dotColor = when (tone) {
        BadgeTone.ACCENT -> AppTheme.colors.accent
        BadgeTone.SUCCESS -> AppTheme.colors.success
        BadgeTone.WARNING -> AppTheme.colors.warning
        BadgeTone.ERROR -> AppTheme.colors.error
        BadgeTone.NEUTRAL -> AppTheme.colors.textTertiary
    }

    val textColor = AppTheme.colors.textSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadii.xs))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(AppRadii.xs))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = dotColor
            )
            Spacer(modifier = Modifier.width(5.dp))
        } else if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = text,
            style = AppTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = textColor
        )
    }
}
