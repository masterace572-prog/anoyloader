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
import androidx.compose.material3.CircularProgressIndicator
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
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppProgressBar
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
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationScreen,
            easing = AppMotion.EasingEntrance
        ),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) { startAnim = true }

    AppBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppTheme.colors.surfaceElevated)
                        .border(1.dp, AppTheme.colors.outline, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = stringResource(id = R.string.app_name),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(id = R.string.brand_name),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.textPrimary
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(contentAlpha)
                    .padding(bottom = AppTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AppTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "v$versionName",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textTertiary
                )
            }
        }

        if (updateDialogState != null && updateDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {
                    if (!updateDialogState.isMandatory && !updateDialogState.isDownloading) {
                        updateDialogState.onLaterClick()
                    }
                },
                title = "Update ${updateDialogState.serverVersionName}"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (updateDialogState.isDownloading) {
                        AppProgressBar(
                            progress = (updateDialogState.downloadProgress.coerceIn(0, 100)) / 100f,
                            statusText = updateDialogState.downloadStatusText.ifBlank { "Downloading…" },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        if (updateDialogState.changelog.isNotBlank()) {
                            Text(
                                text = updateDialogState.changelog,
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                        }
                        AppButton(
                            text = "Update",
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
                                variant = ButtonVariant.TERTIARY,
                                fullWidth = true
                            )
                        }
                    }
                }
            }
        }

        if (maintenanceDialogState != null && maintenanceDialogState.isVisible) {
            AppDialog(
                onDismissRequest = {},
                title = "Under maintenance"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = maintenanceDialogState.message.ifBlank { "Please try again shortly." },
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))
                    AppButton(
                        text = "Retry",
                        onClick = maintenanceDialogState.onRefresh,
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Refresh,
                        fullWidth = true
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                    AppButton(
                        text = "Exit",
                        onClick = maintenanceDialogState.onExit,
                        variant = ButtonVariant.TERTIARY,
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
        SplashScreen(versionName = "1.4.1", statusText = "Starting…")
    }
}

@Preview(name = "Splash — Light", showBackground = true)
@Composable
fun PreviewSplashScreenLight() {
    AppTheme(darkTheme = false) {
        SplashScreen(versionName = "1.4.1", statusText = "Checking updates…")
    }
}
