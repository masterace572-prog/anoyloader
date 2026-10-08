package com.ryzen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppTheme

enum class MainNavTab(val title: String, val icon: ImageVector) {
    GAMES("Games", Icons.Outlined.SportsEsports),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

@Composable
fun AppBottomNav(selectedTab: MainNavTab, onTabSelected: (MainNavTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)
        NavigationBar(containerColor = AppTheme.colors.surface, tonalElevation = 0.dp) {
            MainNavTab.values().forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.title, style = AppTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.colors.accent,
                        selectedTextColor = AppTheme.colors.accent,
                        indicatorColor = AppTheme.colors.accentSubtle,
                        unselectedIconColor = AppTheme.colors.textSecondary,
                        unselectedTextColor = AppTheme.colors.textSecondary
                    )
                )
            }
        }
    }
}
