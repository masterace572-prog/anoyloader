package com.ryzen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppTheme

@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val titleColor = when {
        destructive -> AppTheme.colors.error
        else -> AppTheme.colors.textPrimary
    }
    val iconColor = when {
        destructive -> AppTheme.colors.error
        else -> AppTheme.colors.textSecondary
    }
    val interaction = remember { MutableInteractionSource() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null && enabled) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.titleSmall,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)
        }
    }
}

@Composable
fun AppRowGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        contentPadding = AppTheme.spacing.md
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }
    }
}
