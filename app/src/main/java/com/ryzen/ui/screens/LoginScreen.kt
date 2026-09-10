package com.ryzen.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppProgressBar
import com.ryzen.ui.components.AppTextField
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.components.PermissionsDialog
import com.ryzen.ui.components.PermissionsDialogState
import com.ryzen.ui.theme.AppBackground
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

data class LoadingDialogState(
    val isVisible: Boolean = false,
    val title: String = "Authenticating",
    val message: String = "Verifying license key…",
    val progress: Int? = null,
    val isError: Boolean = false,
    val onDismiss: () -> Unit = {},
    val onRetry: (() -> Unit)? = null
)

data class MaintenanceDialogState(
    val isVisible: Boolean = false,
    val message: String = "",
    val estimatedEnd: String = "",
    val onRefresh: () -> Unit = {},
    val onExit: () -> Unit = {}
)

data class AnnouncementDialogState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val link: String? = null,
    val onContinue: () -> Unit = {},
    val onOpenLink: (() -> Unit)? = null
)

@Composable
fun LoginScreen(
    keyText: String,
    onKeyChange: (String) -> Unit,
    isKeyVisible: Boolean,
    onToggleKeyVisibility: () -> Unit,
    onPasteClick: () -> Unit,
    isSaveKeyEnabled: Boolean,
    onSaveKeyToggle: (Boolean) -> Unit,
    isAuthenticating: Boolean,
    onAuthenticateClick: () -> Unit,
    onGetKeyClick: () -> Unit,
    keyError: String? = null,
    isSystemOnline: Boolean = true,
    loadingDialogState: LoadingDialogState? = null,
    maintenanceDialogState: MaintenanceDialogState? = null,
    announcementDialogState: AnnouncementDialogState? = null,
    permissionsDialogState: PermissionsDialogState = PermissionsDialogState(),
    onGrantAllFiles: () -> Unit = {},
    onGrantRuntimeStorage: () -> Unit = {},
    onGrantNotification: () -> Unit = {},
    onGrantInstall: () -> Unit = {},
    onRestartApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    AppBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = AppTheme.spacing.screenHorizontal,
                    vertical = AppTheme.spacing.xl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))

            // Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(AppRadii.lg))
                        .background(AppTheme.colors.surfaceElevated)
                        .border(1.dp, AppTheme.colors.outline, RoundedCornerShape(AppRadii.lg)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(AppRadii.lg)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                Text(
                    text = stringResource(id = R.string.brand_name),
                    style = AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                Text(
                    text = stringResource(id = R.string.brand_subtitle_auth),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // Auth card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                elevated = true,
                contentPadding = AppTheme.spacing.lg
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.label_license_key),
                            style = AppTheme.typography.titleSmall,
                            color = AppTheme.colors.textPrimary
                        )
                        AppBadge(
                            text = if (isSystemOnline) {
                                stringResource(id = R.string.status_online)
                            } else {
                                stringResource(id = R.string.status_maintenance)
                            },
                            tone = if (isSystemOnline) BadgeTone.SUCCESS else BadgeTone.WARNING
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    AppTextField(
                        value = keyText,
                        onValueChange = onKeyChange,
                        placeholder = "XXXX-XXXX-XXXX-XXXX",
                        leadingIcon = Icons.Outlined.Key,
                        visualTransformation = if (isKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onAuthenticateClick() }
                        ),
                        isError = keyError != null,
                        errorMessage = keyError,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onToggleKeyVisibility,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isKeyVisible) {
                                            Icons.Outlined.Visibility
                                        } else {
                                            Icons.Outlined.VisibilityOff
                                        },
                                        contentDescription = if (isKeyVisible) {
                                            "Hide license key"
                                        } else {
                                            "Show license key"
                                        },
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onPasteClick,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentPaste,
                                        contentDescription = "Paste license key",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSaveKeyToggle(!isSaveKeyEnabled) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSaveKeyEnabled,
                            onCheckedChange = onSaveKeyToggle,
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppTheme.colors.accent,
                                uncheckedColor = AppTheme.colors.outline,
                                checkmarkColor = AppTheme.colors.onAccent
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppTheme.spacing.xs))
                        Text(
                            text = stringResource(id = R.string.action_save_key),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    AppButton(
                        text = stringResource(id = R.string.action_authenticate),
                        onClick = onAuthenticateClick,
                        loading = isAuthenticating,
                        enabled = !isAuthenticating,
                        variant = ButtonVariant.PRIMARY,
                        fullWidth = true
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                    AppButton(
                        text = stringResource(id = R.string.action_get_key),
                        onClick = onGetKeyClick,
                        variant = ButtonVariant.SECONDARY,
                        leadingPainter = painterResource(id = R.drawable.ic_telegram_app),
                        fullWidth = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))
            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.brand_enterprise),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                Text(
                    text = stringResource(id = R.string.security_footer),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))
        }

        // Loading / error dialog
        if (loadingDialogState != null && loadingDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {
                    if (loadingDialogState.isError) loadingDialogState.onDismiss()
                },
                title = loadingDialogState.title,
                subtitle = if (loadingDialogState.isError) null else "Please wait"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (loadingDialogState.isError) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(AppRadii.md))
                                .background(AppTheme.colors.surfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                        Text(
                            text = loadingDialogState.message,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.error
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                        if (loadingDialogState.onRetry != null) {
                            AppButton(
                                text = "Retry",
                                onClick = loadingDialogState.onRetry,
                                variant = ButtonVariant.PRIMARY,
                                fullWidth = true
                            )
                            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        }
                        AppButton(
                            text = "Dismiss",
                            onClick = loadingDialogState.onDismiss,
                            variant = ButtonVariant.SECONDARY,
                            fullWidth = true
                        )
                    } else if (loadingDialogState.progress != null) {
                        AppProgressBar(
                            progress = (loadingDialogState.progress.coerceIn(0, 100)) / 100f,
                            statusText = loadingDialogState.message,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AppTheme.colors.accent,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                        Text(
                            text = loadingDialogState.message,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Maintenance
        if (maintenanceDialogState != null && maintenanceDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {},
                title = "Server maintenance",
                subtitle = "Access is temporarily locked"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = maintenanceDialogState.message.ifBlank {
                            "The authentication server is undergoing scheduled maintenance."
                        },
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                    if (maintenanceDialogState.estimatedEnd.isNotBlank()) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        Text(
                            text = "Estimated completion: ${maintenanceDialogState.estimatedEnd}",
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                    AppButton(
                        text = "Refresh status",
                        onClick = maintenanceDialogState.onRefresh,
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Refresh,
                        fullWidth = true
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                    AppButton(
                        text = "Exit app",
                        onClick = maintenanceDialogState.onExit,
                        variant = ButtonVariant.DESTRUCTIVE,
                        fullWidth = true
                    )
                }
            }
        }

        // Announcement
        if (announcementDialogState != null && announcementDialogState.isVisible) {
            AppDialog(
                onDismissRequest = announcementDialogState.onContinue,
                title = announcementDialogState.title.ifBlank { "Announcement" }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = announcementDialogState.message,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                    if (announcementDialogState.onOpenLink != null) {
                        AppButton(
                            text = "Open link",
                            onClick = announcementDialogState.onOpenLink,
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                            fullWidth = true
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                    }
                    AppButton(
                        text = "Continue",
                        onClick = announcementDialogState.onContinue,
                        variant = ButtonVariant.SECONDARY,
                        fullWidth = true
                    )
                }
            }
        }

        if (permissionsDialogState.isVisible) {
            PermissionsDialog(
                state = permissionsDialogState,
                onGrantAllFiles = onGrantAllFiles,
                onGrantRuntimeStorage = onGrantRuntimeStorage,
                onGrantNotification = onGrantNotification,
                onGrantInstall = onGrantInstall,
                onRestartApp = onRestartApp
            )
        }
    }
}

@Preview(name = "Login — Dark", showBackground = true)
@Composable
fun PreviewLoginScreenDark() {
    AppTheme(darkTheme = true) {
        LoginScreen(
            keyText = "ANOY-ABCD-1234-EFGH",
            onKeyChange = {},
            isKeyVisible = true,
            onToggleKeyVisibility = {},
            onPasteClick = {},
            isSaveKeyEnabled = true,
            onSaveKeyToggle = {},
            isAuthenticating = false,
            onAuthenticateClick = {},
            onGetKeyClick = {},
            isSystemOnline = true
        )
    }
}

@Preview(name = "Login — Light", showBackground = true)
@Composable
fun PreviewLoginScreenLight() {
    AppTheme(darkTheme = false) {
        LoginScreen(
            keyText = "",
            onKeyChange = {},
            isKeyVisible = false,
            onToggleKeyVisibility = {},
            onPasteClick = {},
            isSaveKeyEnabled = false,
            onSaveKeyToggle = {},
            isAuthenticating = false,
            onAuthenticateClick = {},
            onGetKeyClick = {},
            isSystemOnline = true
        )
    }
}
