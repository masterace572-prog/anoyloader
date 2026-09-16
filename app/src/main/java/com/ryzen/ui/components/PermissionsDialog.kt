package com.ryzen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppTheme

data class PermissionsDialogState(
    val isVisible: Boolean = false,
    val hasAllFiles: Boolean = false,
    val hasRuntimeStorage: Boolean = false,
    val hasNotification: Boolean = false,
    val hasInstall: Boolean = false
) {
    val isAllGranted: Boolean
        get() = hasAllFiles && hasRuntimeStorage && hasNotification && hasInstall

    val grantedCount: Int
        get() = (if (hasAllFiles) 1 else 0) +
            (if (hasRuntimeStorage) 1 else 0) +
            (if (hasNotification) 1 else 0) +
            (if (hasInstall) 1 else 0)

    val totalCount: Int = 4
}

@Composable
fun PermissionsDialog(
    state: PermissionsDialogState,
    onGrantAllFiles: () -> Unit,
    onGrantRuntimeStorage: () -> Unit,
    onGrantNotification: () -> Unit,
    onGrantInstall: () -> Unit,
    onRestartApp: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    if (!state.isVisible) return

    AppDialog(
        onDismissRequest = { if (state.isAllGranted) onDismissRequest() },
        title = "Allow access",
        subtitle = "${state.grantedCount} of ${state.totalCount}"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PermissionRow("Files", Icons.Outlined.FolderOpen, state.hasAllFiles, onGrantAllFiles)
            PermissionRow("Storage", Icons.Outlined.SdStorage, state.hasRuntimeStorage, onGrantRuntimeStorage)
            PermissionRow("Notifications", Icons.Outlined.Notifications, state.hasNotification, onGrantNotification)
            PermissionRow("Install apps", Icons.Outlined.SystemUpdate, state.hasInstall, onGrantInstall)

            if (state.isAllGranted) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                AppButton(
                    text = "Continue",
                    onClick = onRestartApp,
                    variant = ButtonVariant.PRIMARY,
                    fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAllowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Outlined.Check else icon,
                contentDescription = null,
                tint = if (isGranted) AppTheme.colors.success else AppTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.textPrimary
            )
        }
        if (isGranted) {
            Text(
                text = "On",
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.success
            )
        } else {
            AppButton(
                text = "Allow",
                onClick = onAllowClick,
                variant = ButtonVariant.PRIMARY,
                size = ButtonSize.COMPACT,
                fullWidth = false
            )
        }
    }
}
