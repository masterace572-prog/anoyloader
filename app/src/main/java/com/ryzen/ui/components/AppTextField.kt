package com.ryzen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AppRadii.xs)

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.outline.copy(alpha = 0.5f)
            isError -> AppTheme.colors.error
            isFocused -> AppTheme.colors.accent
            else -> AppTheme.colors.outline
        },
        animationSpec = tween(AppMotion.DurationState),
        label = "fieldBorder"
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.textTertiary
            isError -> AppTheme.colors.error
            isFocused -> AppTheme.colors.accent
            else -> AppTheme.colors.textSecondary
        },
        animationSpec = tween(AppMotion.DurationState),
        label = "fieldLabel"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> AppTheme.colors.textTertiary
            isFocused -> AppTheme.colors.textPrimary
            else -> AppTheme.colors.textSecondary
        },
        animationSpec = tween(AppMotion.DurationState),
        label = "fieldIcon"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = AppTheme.typography.labelMedium,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(AppTheme.colors.surfaceSubtle)
                .border(1.dp, borderColor, shape)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textTertiary
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        textStyle = AppTheme.typography.bodyLarge.copy(
                            color = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.textTertiary
                        ),
                        cursorBrush = SolidColor(AppTheme.colors.accent),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        singleLine = singleLine
                    )
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        }

        val displayFeedback = if (isError) errorMessage else helperText
        if (!displayFeedback.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayFeedback,
                style = AppTheme.typography.labelMedium,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.textTertiary
            )
        }
    }
}
