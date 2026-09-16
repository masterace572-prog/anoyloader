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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

/**
 * Permissions setup — one request card per permission.
 * Each pending item shows its own full-width Allow button (never a mid-row chip).
 * When everything is granted, a single Restart primary action is shown.
 */
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
        onDismissRequest = {
            // Required flow — ignore outside dismiss unless fully granted
            if (state.isAllGranted) onDismissRequest()
        },
        title = "Permissions required",
        subtitle = "Allow each item below so the sandbox can run the game safely"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.grantedCount} of ${state.totalCount} granted",
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                AppBadge(
                    text = if (state.isAllGranted) "Ready" else "Action needed",
                    tone = if (state.isAllGranted) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            // Thin progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.surfaceSubtle)
            ) {
                val fraction = state.grantedCount / state.totalCount.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (state.isAllGranted) AppTheme.colors.success
                            else AppTheme.colors.accent
                        )
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
            ) {
                PermissionRequestCard(
                    title = "All files access",
                    description = "Needed to mount OBB packages and sandbox game files.",
                    icon = Icons.Outlined.FolderOpen,
                    isGranted = state.hasAllFiles,
                    onAllowClick = onGrantAllFiles
                )
                PermissionRequestCard(
                    title = "Storage access",
                    description = "Read and write game data used by the virtual environment.",
                    icon = Icons.Outlined.SdStorage,
                    isGranted = state.hasRuntimeStorage,
                    onAllowClick = onGrantRuntimeStorage
                )
                PermissionRequestCard(
                    title = "Notifications",
                    description = "Keeps virtual game services running in the background.",
                    icon = Icons.Outlined.Notifications,
                    isGranted = state.hasNotification,
                    onAllowClick = onGrantNotification
                )
                PermissionRequestCard(
                    title = "Install unknown apps",
                    description = "Lets the loader register game packages inside the sandbox.",
                    icon = Icons.Outlined.SystemUpdate,
                    isGranted = state.hasInstall,
                    onAllowClick = onGrantInstall
                )
            }

            if (state.isAllGranted) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                Text(
                    text = "All permissions are granted. Restart the app to finish setup.",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                AppButton(
                    text = "Restart app",
                    onClick = onRestartApp,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Outlined.RestartAlt,
                    fullWidth = true
                )
            } else {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                Text(
                    text = "Tap Allow on each card. System settings may open for some items.",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textTertiary
                )
            }
        }
    }
}

/**
 * One permission = one card.
 * Layout (pending): icon + title/desc stacked, then full-width Allow.
 * Layout (granted): same header with Granted status — no mid-row chip button.
 */
@Composable
private fun PermissionRequestCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAllowClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppRadii.md)
    val borderColor = if (isGranted) {
        AppTheme.colors.success.copy(alpha = 0.35f)
    } else {
        AppTheme.colors.outline
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surfaceElevated)
            .border(1.dp, borderColor, shape)
            .padding(AppTheme.spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(AppRadii.sm))
                    .background(AppTheme.colors.surfaceSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Outlined.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) {
                        AppTheme.colors.success
                    } else {
                        AppTheme.colors.textSecondary
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

        if (isGranted) {
            // Full-width status strip — not a floating mid-row control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadii.xs))
                    .background(AppTheme.colors.surfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AppTheme.colors.success,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Granted",
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.success
                )
            }
        } else {
            // Full-width Allow — primary request action for this permission only
            AppButton(
                text = "Allow",
                onClick = onAllowClick,
                variant = ButtonVariant.PRIMARY,
                size = ButtonSize.COMPACT,
                fullWidth = true
            )
        }
    }
}
