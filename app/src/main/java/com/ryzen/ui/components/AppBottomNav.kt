package com.ryzen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzen.ui.theme.AppTheme

enum class MainNavTab(
    val title: String,
    val icon: ImageVector
) {
    GAMES("Games", Icons.Rounded.SportsEsports),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun AppBottomNav(
    selectedTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dock wrapper centered horizontally, floating above navigation bar
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rounded Pill Dock Container with Claude tone-based elevation
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surfaceElevated)
                .border(1.dp, AppTheme.colors.border, CircleShape)
                .padding(3.dp)
                .pointerInput(selectedTab) {
                    var dragAccumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            if (dragAccumulator > 25f && selectedTab != MainNavTab.SETTINGS) {
                                onTabSelected(MainNavTab.SETTINGS)
                                dragAccumulator = 0f
                            } else if (dragAccumulator < -25f && selectedTab != MainNavTab.GAMES) {
                                onTabSelected(MainNavTab.GAMES)
                                dragAccumulator = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val tabCount = MainNavTab.values().size
            val tabWidth = (maxWidth) / tabCount
            val targetOffset = if (selectedTab == MainNavTab.GAMES) 0.dp else tabWidth

            // Smooth animated sliding pill indicator
            val pillOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "sliding_pill_offset"
            )

            // Sliding active pill background (tone surface with subtle border)
            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(tabWidth)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surface)
                    .border(1.dp, AppTheme.colors.borderSubtle, CircleShape)
            )

            // Pill Tabs with tap selection and text/icon color transitions
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val interactionSource = remember { MutableInteractionSource() }

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_content_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onTabSelected(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tab.title,
                                style = AppTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
