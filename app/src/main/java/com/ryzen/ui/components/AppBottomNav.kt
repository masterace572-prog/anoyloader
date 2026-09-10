package com.ryzen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppTheme

enum class MainNavTab(
    val title: String,
    val icon: ImageVector
) {
    GAMES("Games", Icons.Outlined.SportsEsports),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

/**
 * Flat bottom navigation — surface background, 1dp top outline, no shadow/pill.
 * Selected = accent tint; unselected = textSecondary. Outlined icons only.
 */
@Composable
fun AppBottomNav(
    selectedTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainNavTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember(tab) { MutableInteractionSource() }

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        AppTheme.colors.accent
                    } else {
                        AppTheme.colors.textSecondary
                    },
                    animationSpec = tween(
                        durationMillis = AppMotion.DurationState,
                        easing = AppMotion.EasingStandard
                    ),
                    label = "navColor_${tab.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.title,
                            style = AppTheme.typography.labelMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
