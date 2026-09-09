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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ryzen.ui.theme.AppRadii
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationSlow,
            easing = AppMotion.EasingEntrance
        ),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.88f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationSlow,
            easing = AppMotion.EasingEntrance
        ),
        label = "splashScale"
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
            // Soft ambient glow behind logo
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .alpha(alphaAnim * 0.55f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppTheme.colors.accentGlow,
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Top Version Tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.spacing.lg, end = AppTheme.spacing.lg),
                contentAlignment = Alignment.TopEnd
            ) {
                AppBadge(
                    text = "v$versionName",
                    tone = BadgeTone.ACCENT,
                    showDot = false
                )
            }

            // Center Branding Container
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(alphaAnim)
                    .scale(scaleAnim)
                    .padding(horizontal = AppTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
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

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                Text(
                    text = stringResource(id = R.string.brand_name),
                    style = AppTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                Text(
                    text = "Virtualization Engine & Security Core",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
            }

            // Bottom Live Status & Progress Area
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
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
                    text = "Userspace Sandboxing • Anti-Tamper Protected",
                    style = AppTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AppTheme.colors.textTertiary
                )
            }
        }

        // In-App APK Update Dialog
        if (updateDialogState != null && updateDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {
                    if (!updateDialogState.isMandatory && !updateDialogState.isDownloading) {
                        updateDialogState.onLaterClick()
                    }
                },
                title = "Update Available",
                subtitle = "New version v${updateDialogState.serverVersionName} is available"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (updateDialogState.isDownloading) {
                        AppProgressBar(
                            progress = (updateDialogState.downloadProgress.coerceIn(0, 100)) / 100f,
                            statusText = updateDialogState.downloadStatusText.ifBlank { "Downloading update..." },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.accentTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        if (updateDialogState.changelog.isNotBlank()) {
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = AppTheme.colors.surfaceElevated,
                                contentPadding = AppTheme.spacing.md
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "What's new",
                                        style = AppTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.2.sp
                                        ),
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

                            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                        }

                        AppButton(
                            text = "Update now",
                            onClick = updateDialogState.onUpdateClick,
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Rounded.CloudDownload,
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

        // Maintenance Dialog
        if (maintenanceDialogState != null && maintenanceDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {},
                title = "Server Maintenance Active",
                subtitle = "The application is temporarily locked"
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
                        text = maintenanceDialogState.message.ifBlank { "Server is undergoing scheduled maintenance." },
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
                        text = "Check Status",
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
    }
}

@Preview(name = "Splash Screen - Dark", showBackground = true)
@Composable
fun PreviewSplashScreenDark() {
    AppTheme(darkTheme = true) {
        SplashScreen(
            versionName = "2026.01.01",
            statusText = "Initializing security core..."
        )
    }
}

@Preview(name = "Splash Screen - Light", showBackground = true)
@Composable
fun PreviewSplashScreenLight() {
    AppTheme(darkTheme = false) {
        SplashScreen(
            versionName = "2026.01.01",
            statusText = "Checking for updates..."
        )
    }
}
