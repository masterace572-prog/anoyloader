package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.ui.theme.AppTheme

/** Shows actual installation state, never invented engine/network health. */
@Composable
fun SetupChecklist(hostInstalled: Boolean, containerInstalled: Boolean, assetsReady: Boolean) {
    val complete = hostInstalled && containerInstalled && assetsReady
    AppCard(modifier = Modifier.fillMaxWidth(), containerColor = AppTheme.colors.surface, contentPadding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(if (complete) R.string.setup_ready_title else R.string.setup_title),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary
            )
            listOf(
                R.string.setup_official_game to hostInstalled,
                R.string.setup_virtual_copy to (hostInstalled && containerInstalled),
                R.string.setup_game_assets to complete
            ).forEachIndexed { index, (label, done) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(26.dp).background(
                            if (done) AppTheme.colors.successTint else AppTheme.colors.surfaceSubtle,
                            CircleShape
                        ), contentAlignment = Alignment.Center
                    ) {
                        if (done) Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.setup_complete), tint = AppTheme.colors.success, modifier = Modifier.size(15.dp))
                        else Text("${index + 1}", style = AppTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
                    }
                    Text(stringResource(label), style = AppTheme.typography.bodySmall, color = if (done) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary)
                }
            }
            if (!complete) Text(stringResource(R.string.setup_hint), style = AppTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
        }
    }
}
