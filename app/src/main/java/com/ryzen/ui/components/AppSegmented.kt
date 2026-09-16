package com.ryzen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

@Composable
fun AppSegmented(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val shape = RoundedCornerShape(AppRadii.sm)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(AppTheme.colors.surfaceSubtle)
            .border(1.dp, AppTheme.colors.outline, shape)
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) AppTheme.colors.surfaceElevated else AppTheme.colors.surfaceSubtle,
                animationSpec = tween(AppMotion.DurationState),
                label = "segBg_$index"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                animationSpec = tween(AppMotion.DurationState),
                label = "segFg_$index"
            )
            val interaction = remember(index) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(AppRadii.xs))
                    .background(bg)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTheme.typography.labelMedium,
                    color = fg,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
