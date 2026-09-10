package com.ryzen.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppProgressBar
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.theme.AppBackground
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

data class AppUpdateDialogState(
    val isVisible: Boolean = false,
    val serverVersionName: String = "",
    val changelog: String = "",
    val isMandatory: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadStatusText: String = "",
    val onUpdateClick: () -> Unit = {},
    val onLaterClick: () -> Unit = {}
)

@Composable
fun SplashScreen(
    versionName: String,
    statusText: String,
    updateDialogState: AppUpdateDialogState? = null,
    maintenanceDialogState: MaintenanceDialogState? = null,
    modifier: Modifier = Modifier
) {
    var startAnim by remember { mutableStateOf(false) }

    // Fade only — no scale bounce
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationScreen,
            easing = AppMotion.EasingEntrance
        ),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    AppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Version chip (top-end)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.spacing.lg,
                        end = AppTheme.spacing.screenHorizontal
                    ),
                contentAlignment = Alignment.TopEnd
            ) {
                AppBadge(
                    text = "v$versionName",
                    tone = BadgeTone.NEUTRAL,
                    showDot = false
                )
            }

            // Center brand
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(contentAlpha)
                    .padding(horizontal = AppTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
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

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                Text(
                    text = stringResource(id = R.string.brand_name),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                Text(
                    text = stringResource(id = R.string.brand_subtitle_engine),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
            }

            // Bottom status
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .alpha(contentAlpha)
                    .padding(
                        horizontal = AppTheme.spacing.screenHorizontal,
                        vertical = AppTheme.spacing.xxl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AppTheme.colors.accent
                    )
                    Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                    Text(
                        text = statusText,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                Text(
                    text = stringResource(id = R.string.userspace_badge),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textTertiary
                )
            }
        }

        // Update dialog
        if (updateDialogState != null && updateDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {
                    if (!updateDialogState.isMandatory && !updateDialogState.isDownloading) {
                        updateDialogState.onLaterClick()
                    }
                },
                title = "Update available",
                subtitle = "Version ${updateDialogState.serverVersionName} is ready to install"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (updateDialogState.isDownloading) {
                        AppProgressBar(
                            progress = (updateDialogState.downloadProgress.coerceIn(0, 100)) / 100f,
                            statusText = updateDialogState.downloadStatusText.ifBlank {
                                "Downloading update…"
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(AppRadii.md))
                                .background(AppTheme.colors.surfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (updateDialogState.changelog.isNotBlank()) {
                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = AppTheme.spacing.md
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "What's new",
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                                    Text(
                                        text = updateDialogState.changelog,
                                        style = AppTheme.typography.bodySmall,
                                        color = AppTheme.colors.textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                        AppButton(
                            text = "Update now",
                            onClick = updateDialogState.onUpdateClick,
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.CloudDownload,
                            fullWidth = true
                        )

                        if (!updateDialogState.isMandatory) {
                            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                            AppButton(
                                text = "Later",
                                onClick = updateDialogState.onLaterClick,
                                variant = ButtonVariant.SECONDARY,
                                fullWidth = true
                            )
                        }
                    }
                }
            }
        }

        // Maintenance dialog
        if (maintenanceDialogState != null && maintenanceDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {},
                title = "Server maintenance",
                subtitle = "The application is temporarily unavailable"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            tint = AppTheme.colors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Text(
                        text = maintenanceDialogState.message.ifBlank {
                            "Server is undergoing scheduled maintenance."
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
                        text = "Check status",
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
    }
}

@Preview(name = "Splash — Dark", showBackground = true)
@Composable
fun PreviewSplashScreenDark() {
    AppTheme(darkTheme = true) {
        SplashScreen(
            versionName = "1.3.1",
            statusText = "Initializing security core…"
        )
    }
}

@Preview(name = "Splash — Light", showBackground = true)
@Composable
fun PreviewSplashScreenLight() {
    AppTheme(darkTheme = false) {
        SplashScreen(
            versionName = "1.3.1",
            statusText = "Checking for updates…"
        )
    }
}
