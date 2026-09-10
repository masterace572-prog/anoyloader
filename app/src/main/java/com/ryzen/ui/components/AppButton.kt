package com.ryzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    DESTRUCTIVE
}

enum class ButtonSize {
    DEFAULT,
    COMPACT
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.DEFAULT,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    leadingPainter: Painter? = null,
    trailingIcon: ImageVector? = null,
    customContainerColor: Color? = null,
    customContentColor: Color? = null,
    fullWidth: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonHeight = if (size == ButtonSize.COMPACT) 40.dp else 48.dp
    val widthModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    val shape = RoundedCornerShape(AppRadii.sm)
    val isEnabled = enabled && !loading

    // Destructive = text button with error color (never a filled capsule)
    if (variant == ButtonVariant.TERTIARY || variant == ButtonVariant.DESTRUCTIVE) {
        val contentColor = customContentColor ?: when (variant) {
            ButtonVariant.DESTRUCTIVE -> AppTheme.colors.error
            else -> AppTheme.colors.textPrimary
        }
        TextButton(
            onClick = onClick,
            modifier = widthModifier.height(buttonHeight),
            enabled = isEnabled,
            interactionSource = interactionSource,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = contentColor,
                disabledContentColor = contentColor.copy(alpha = 0.38f)
            )
        ) {
            ButtonContent(
                text = text,
                loading = loading,
                enabled = isEnabled,
                contentColor = if (isEnabled) contentColor else contentColor.copy(alpha = 0.38f),
                leadingIcon = leadingIcon,
                leadingPainter = leadingPainter,
                trailingIcon = trailingIcon
            )
        }
        return
    }

    val containerColor = customContainerColor ?: when (variant) {
        ButtonVariant.PRIMARY -> AppTheme.colors.accent
        ButtonVariant.SECONDARY -> Color.Transparent
        else -> Color.Transparent
    }
    val contentColor = customContentColor ?: when (variant) {
        ButtonVariant.PRIMARY -> AppTheme.colors.onAccent
        ButtonVariant.SECONDARY -> AppTheme.colors.textPrimary
        else -> AppTheme.colors.textPrimary
    }
    val border = when (variant) {
        ButtonVariant.SECONDARY -> BorderStroke(1.dp, AppTheme.colors.outline)
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = widthModifier.height(buttonHeight),
        enabled = isEnabled,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = when (variant) {
                ButtonVariant.PRIMARY -> AppTheme.colors.accent.copy(alpha = 0.38f)
                else -> Color.Transparent
            },
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        ButtonContent(
            text = text,
            loading = loading,
            enabled = isEnabled,
            contentColor = if (isEnabled) contentColor else contentColor.copy(alpha = 0.38f),
            leadingIcon = leadingIcon,
            leadingPainter = leadingPainter,
            trailingIcon = trailingIcon
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    contentColor: Color,
    leadingIcon: ImageVector?,
    leadingPainter: Painter?,
    trailingIcon: ImageVector?
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Keep width stable while loading: hide label row, show spinner centered
        Row(
            modifier = Modifier.alpha(if (loading) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingPainter != null) {
                Image(
                    painter = leadingPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = AppTheme.typography.labelLarge,
                color = contentColor
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        }
    }
}
