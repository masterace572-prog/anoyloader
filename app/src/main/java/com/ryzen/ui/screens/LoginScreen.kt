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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ryzen.ui.theme.AppRadii
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.BadgeTone
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzen.R
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppProgressBar
import com.ryzen.ui.components.AppTextField
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.components.PermissionsDialog
import com.ryzen.ui.components.PermissionsDialogState
import com.ryzen.ui.theme.AppTheme

data class LoadingDialogState(
    val isVisible: Boolean = false,
    val title: String = "Authenticating",
    val message: String = "Verifying license key...",
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = AppTheme.colors.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                // Brand Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(AppRadii.lg))
                            .background(AppTheme.colors.surfaceElevated)
                            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppRadii.lg)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = stringResource(id = R.string.app_name) + " App Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(AppRadii.lg)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Text(
                        text = stringResource(id = R.string.brand_name),
                        style = AppTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = AppTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                    Text(
                        text = "Secure Sandbox Authentication",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                // Main Authentication Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = AppTheme.spacing.lg
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Card Title & System Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "License key",
                                style = AppTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AppTheme.colors.textPrimary
                            )

                            AppBadge(
                                text = if (isSystemOnline) "Online" else "Maintenance",
                                tone = if (isSystemOnline) BadgeTone.SUCCESS else BadgeTone.WARNING
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        // Input Box with Show/Hide and Paste Actions
                        AppTextField(
                            value = keyText,
                            onValueChange = onKeyChange,
                            leadingIcon = Icons.Rounded.Key,
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = if (isKeyVisible) "Hide license key" else "Show license key",
                                            tint = AppTheme.colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = onPasteClick,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = "Paste license key from clipboard",
                                            tint = AppTheme.colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Save Key Option Row
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
                                    uncheckedColor = AppTheme.colors.border,
                                    checkmarkColor = AppTheme.colors.textPrimary
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(AppTheme.spacing.xs))

                            Text(
                                text = "Save license key on this device",
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                        // Authenticate Action Button
                        AppButton(
                            text = "Authenticate",
                            onClick = onAuthenticateClick,
                            loading = isAuthenticating,
                            variant = ButtonVariant.PRIMARY,
                            fullWidth = true
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                        // Telegram Get Key Button
                        AppButton(
                            text = "Get license key",
                            onClick = onGetKeyClick,
                            variant = ButtonVariant.SECONDARY,
                            leadingPainter = painterResource(id = R.drawable.ic_telegram_app),
                            fullWidth = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                // Footer Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.brand_enterprise),
                        style = AppTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = AppTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                    Text(
                        text = "Protected with Advanced Sandboxing & Anti-Tamper Core",
                        style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppTheme.colors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
            }
        }

        // Loading & Progress Modal Dialog
        if (loadingDialogState != null && loadingDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {
                    if (loadingDialogState.isError) {
                        loadingDialogState.onDismiss()
                    }
                },
                title = loadingDialogState.title,
                subtitle = if (loadingDialogState.isError) null else "Please wait while operations finish"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (loadingDialogState.isError) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        Text(
                            text = loadingDialogState.message,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.error,
                            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm)
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
                    } else {
                        if (loadingDialogState.progress != null) {
                            AppProgressBar(
                                progress = (loadingDialogState.progress.coerceIn(0, 100)) / 100f,
                                statusText = loadingDialogState.message,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = AppTheme.colors.accent,
                                strokeWidth = 3.dp
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
        }

        // Server Maintenance Modal Dialog
        if (maintenanceDialogState != null && maintenanceDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {},
                title = "Server Maintenance Active",
                subtitle = "Access is temporarily locked by administrators"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.warningContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = AppTheme.colors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Text(
                        text = maintenanceDialogState.message.ifBlank { "The authentication server is undergoing scheduled maintenance." },
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )

                    if (maintenanceDialogState.estimatedEnd.isNotBlank()) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        Text(
                            text = "Estimated Completion: ${maintenanceDialogState.estimatedEnd}",
                            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppTheme.colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    AppButton(
                        text = "Refresh Status",
                        onClick = maintenanceDialogState.onRefresh,
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Rounded.Refresh,
                        fullWidth = true
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                    AppButton(
                        text = "Exit App",
                        onClick = maintenanceDialogState.onExit,
                        variant = ButtonVariant.DESTRUCTIVE,
                        fullWidth = true
                    )
                }
            }
        }

        // Server Announcement Modal Dialog
        if (announcementDialogState != null && announcementDialogState.isVisible) {
            AppDialog(
                onDismissRequest = announcementDialogState.onContinue,
                title = announcementDialogState.title.ifBlank { "Announcement" }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = announcementDialogState.message,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    if (announcementDialogState.onOpenLink != null) {
                        AppButton(
                            text = "Open Link",
                            onClick = announcementDialogState.onOpenLink,
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
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

@Preview(name = "Login Screen - Dark", showBackground = true)
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

@Preview(name = "Login Screen - Light", showBackground = true)
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
