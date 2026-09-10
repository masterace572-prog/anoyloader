package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ryzen.ui.components.ButtonSize
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.theme.AppRadii
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

@OptIn(ExperimentalMaterial3Api::class)
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

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(AppTheme.spacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadii.lg))
                .background(AppTheme.colors.surface)
                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppRadii.lg))
                .padding(AppTheme.spacing.xl)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle / Accent notch
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.border)
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                Text(
                    text = "Permissions Required",
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                Text(
                    text = "Grant the following permissions to enable the game sandbox",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // List of Permissions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    PermissionRow(
                        title = "All Files Access",
                        description = "Mount game OBBs and sandbox files",
                        icon = Icons.Outlined.FolderOpen,
                        isGranted = state.hasAllFiles,
                        onAllowClick = onGrantAllFiles
                    )

                    PermissionRow(
                        title = "Storage Read and Write",
                        description = "Access game data without permission popups",
                        icon = Icons.Outlined.SdStorage,
                        isGranted = state.hasRuntimeStorage,
                        onAllowClick = onGrantRuntimeStorage
                    )

                    PermissionRow(
                        title = "Notifications",
                        description = "Keep virtual game services active",
                        icon = Icons.Outlined.Notifications,
                        isGranted = state.hasNotification,
                        onAllowClick = onGrantNotification
                    )

                    PermissionRow(
                        title = "Install Packages",
                        description = "Register game packages in sandbox",
                        icon = Icons.Outlined.SystemUpdate,
                        isGranted = state.hasInstall,
                        onAllowClick = onGrantInstall
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                // Bottom Action: Restart App if all granted, or progress tracker
                if (state.isAllGranted) {
                    AppButton(
                        text = "Restart App",
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.RestartAlt,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRestartApp
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                    Text(
                        text = "All permissions granted. Restart to continue setup.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.success
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.grantedCount} of ${state.totalCount} granted",
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )

                        AppBadge(
                            text = "Setup incomplete",
                            tone = BadgeTone.WARNING
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAllowClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadii.md))
            .background(AppTheme.colors.background)
            .border(
                1.dp,
                if (isGranted) AppTheme.colors.success.copy(alpha = 0.3f) else AppTheme.colors.border,
                RoundedCornerShape(AppRadii.md)
            )
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(AppRadii.sm))
                        .background(
                            if (isGranted) AppTheme.colors.success.copy(alpha = 0.15f)
                            else AppTheme.colors.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) AppTheme.colors.success else AppTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppTheme.spacing.sm))

                Column {
                    Text(
                        text = title,
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = description,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Granted",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                AppButton(
                    text = "Allow",
                    variant = ButtonVariant.PRIMARY,
                    size = ButtonSize.COMPACT,
                    fullWidth = false,
                    onClick = onAllowClick
                )
            }
        }
    }
}
