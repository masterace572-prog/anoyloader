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
import com.ryzen.ui.theme.pressScale

/**
 * Soft elevated card — subtle border + light tonal lift for depth without heavy shadows.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadii.md),
    containerColor: Color = AppTheme.colors.surface,
    borderColor: Color = AppTheme.colors.borderSubtle,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = if (elevated) AppTheme.colors.surfaceElevated else containerColor
    val elevation = if (elevated) 2.dp else 0.dp

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(
                targetScale = 0.985f,
                interactionSource = interactionSource
            ),
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(borderWidth, borderColor),
            interactionSource = interactionSource,
            shadowElevation = elevation,
            tonalElevation = elevation
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(borderWidth, borderColor),
            shadowElevation = elevation,
            tonalElevation = elevation
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}
