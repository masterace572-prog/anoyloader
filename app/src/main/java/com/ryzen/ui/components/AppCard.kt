package com.ryzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

/**
 * Card: surfaceElevated, 12dp radius, 1dp outline, 16dp padding, no shadow.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadii.md),
    containerColor: Color = AppTheme.colors.surfaceElevated,
    borderColor: Color = AppTheme.colors.outline,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = if (elevated) AppTheme.colors.surfaceElevated else containerColor

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(borderWidth, borderColor),
            interactionSource = interactionSource,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(borderWidth, borderColor),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
