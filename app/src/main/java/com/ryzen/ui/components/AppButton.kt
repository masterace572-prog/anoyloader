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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.pressScale

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    DESTRUCTIVE
}

enum class ButtonSize {
    DEFAULT, // 52dp
    COMPACT  // 44dp
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

    val containerColor = customContainerColor ?: when (variant) {
        ButtonVariant.PRIMARY -> AppTheme.colors.accent
        ButtonVariant.SECONDARY -> AppTheme.colors.surfaceElevated
        ButtonVariant.TERTIARY -> Color.Transparent
        ButtonVariant.DESTRUCTIVE -> AppTheme.colors.error
    }

    val contentColor = customContentColor ?: when (variant) {
        ButtonVariant.PRIMARY -> if (AppTheme.colors.isDark) AppTheme.colors.background else Color.White
        ButtonVariant.SECONDARY -> AppTheme.colors.textPrimary
        ButtonVariant.TERTIARY -> AppTheme.colors.textSecondary
        ButtonVariant.DESTRUCTIVE -> Color.White
    }

    val disabledContainerColor = when {
        customContainerColor != null -> customContainerColor.copy(alpha = 0.4f)
        variant == ButtonVariant.PRIMARY -> AppTheme.colors.accentDisabled
        variant == ButtonVariant.SECONDARY -> AppTheme.colors.surfaceVariant.copy(alpha = 0.5f)
        variant == ButtonVariant.DESTRUCTIVE -> AppTheme.colors.error.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    val disabledContentColor = AppTheme.colors.disabled

    val border = when {
        customContainerColor != null -> null
        variant == ButtonVariant.SECONDARY -> BorderStroke(1.dp, AppTheme.colors.border)
        variant == ButtonVariant.DESTRUCTIVE -> null
        else -> null
    }

    val buttonHeight = if (size == ButtonSize.COMPACT) 40.dp else 48.dp
    val widthModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    Button(
        onClick = onClick,
        modifier = widthModifier
            .height(buttonHeight)
            .pressScale(targetScale = 0.97f, interactionSource = interactionSource, enabled = enabled && !loading),
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AppRadii.sm),
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (leadingPainter != null) {
                        Image(
                            painter = leadingPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) contentColor else disabledContentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = text,
                        style = AppTheme.typography.labelLarge,
                        color = if (enabled) contentColor else disabledContentColor
                    )

                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled) contentColor else disabledContentColor
                        )
                    }
                }
            }
        }
    }
}
