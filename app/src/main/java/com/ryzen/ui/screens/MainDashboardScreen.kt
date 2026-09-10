package com.ryzen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.model.GameVersion
import com.ryzen.model.ManagedGame
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppProgressBar
import com.ryzen.ui.components.AppTopBar
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.components.GameNotInstalledDialog
import com.ryzen.ui.components.GameNotInstalledDialogState
import com.ryzen.ui.components.SectionHeader
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import androidx.compose.foundation.shape.RoundedCornerShape

private data class ActionButtonState(
    val text: String,
    val enabled: Boolean,
    val variant: ButtonVariant,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?
)

@Composable
fun MainDashboardScreen(
    days: String = "00",
    hours: String = "00",
    mins: String = "00",
    secs: String = "00",
    isLifetime: Boolean = false,
    isLaunching: Boolean = false,
    isInstalling: Boolean = false,
    isCopyingObb: Boolean = false,
    obbProgress: Float = 0f,
    progressMessage: String = "",
    hasObb: Boolean = false,
    isClonedInContainer: Boolean = false,
    games: List<ManagedGame> = listOf(ManagedGame.DEFAULT_BGMI),
    selectedGame: ManagedGame = ManagedGame.DEFAULT_BGMI,
    selectedVersion: GameVersion? = selectedGame.versions.firstOrNull(),
    isHostGameInstalled: Boolean = true,
    hostInstalledVersionName: String = "",
    hostInstalledVersionCode: Long = 0L,
    onSelectGame: (ManagedGame) -> Unit = {},
    isGameEnabled: Boolean = true,
    gameStatusText: String = "OBB Ready",
    onLaunchClick: () -> Unit = {},
    onInstallClick: () -> Unit = {},
    onLaunchVersionClick: ((GameVersion) -> Unit)? = null,
    onInstallVersionClick: ((GameVersion) -> Unit)? = null,
    onSyncObbClick: () -> Unit = {},
    onClearLoginClick: () -> Unit = {},
    showOptionsDialog: Boolean = false,
    onOptionsDismiss: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    gameNotInstalledDialogState: GameNotInstalledDialogState = GameNotInstalledDialogState(),
    onInstallFromPlayStore: () -> Unit = {},
    onDismissGameNotInstalledDialog: () -> Unit = {}
) {
    val gameTitle = selectedGame.getDisplayTitle()
    // Bottom nav is 56dp + system insets; keep content clear of it
    val bottomContentPad = 72.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppTopBar(
                title = stringResource(id = R.string.brand_name),
                subtitle = "Virtualization engine",
                trailingContent = {
                    Text(
                        text = when {
                            isLifetime -> stringResource(id = R.string.status_lifetime)
                            days != "00" -> "${days}d ${hours}h"
                            else -> "${hours}h ${mins}m"
                        },
                        style = AppTheme.typography.labelMedium,
                        color = if (isLifetime) {
                            AppTheme.colors.accent
                        } else {
                            AppTheme.colors.success
                        }
                    )
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        text = stringResource(id = R.string.header_target_app),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val versionsToDisplay = if (selectedGame.versions.isNotEmpty()) {
                    selectedGame.versions
                } else {
                    listOf(
                        GameVersion(
                            id = "${selectedGame.id}_default",
                            gameId = selectedGame.id,
                            versionName = hostInstalledVersionName.ifBlank { "4.5.0" },
                            versionCode = hostInstalledVersionCode.toInt(),
                            obbName = "",
                            tag = "LATEST",
                            statusText = "Ready",
                            libVersion = "1.0",
                            isDefault = true,
                            isActive = true
                        )
                    )
                }

                versionsToDisplay.forEachIndexed { index, version ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                    }

                    val isVerComingSoon = version.isComingSoon || selectedGame.isComingSoon
                    val isMatchingHostVersion = isHostGameInstalled && (
                        (version.versionCode > 0 && hostInstalledVersionCode == version.versionCode.toLong()) ||
                            (hostInstalledVersionName.isNotBlank() &&
                                hostInstalledVersionName == version.versionName)
                        )

                    val versionTag = version.tag.ifBlank { "Latest" }
                    val (badgeText, badgeTone) = when {
                        isVerComingSoon ->
                            stringResource(id = R.string.status_coming_soon) to BadgeTone.ACCENT
                        !isGameEnabled ->
                            stringResource(id = R.string.status_disabled) to BadgeTone.ERROR
                        versionTag.equals("LATEST", ignoreCase = true) ->
                            stringResource(id = R.string.status_latest) to BadgeTone.SUCCESS
                        versionTag.equals("STABLE", ignoreCase = true) ->
                            stringResource(id = R.string.status_stable) to BadgeTone.SUCCESS
                        versionTag.equals("BETA", ignoreCase = true) ->
                            stringResource(id = R.string.status_beta) to BadgeTone.WARNING
                        versionTag.equals("TEST", ignoreCase = true) ->
                            stringResource(id = R.string.status_test) to BadgeTone.ACCENT
                        versionTag.equals("COMING SOON", ignoreCase = true) ->
                            stringResource(id = R.string.status_coming_soon) to BadgeTone.ACCENT
                        else -> versionTag to BadgeTone.NEUTRAL
                    }

                    val cardButtonState = when {
                        isVerComingSoon -> ActionButtonState(
                            stringResource(id = R.string.status_coming_soon),
                            false,
                            ButtonVariant.SECONDARY,
                            Icons.Outlined.Schedule
                        )
                        !isGameEnabled -> ActionButtonState(
                            "Unavailable",
                            false,
                            ButtonVariant.SECONDARY,
                            null
                        )
                        !isHostGameInstalled -> ActionButtonState(
                            "$gameTitle not installed",
                            true,
                            ButtonVariant.PRIMARY,
                            Icons.Outlined.Download
                        )
                        !isMatchingHostVersion -> ActionButtonState(
                            "Version not installed",
                            false,
                            ButtonVariant.SECONDARY,
                            null
                        )
                        isInstalling -> ActionButtonState(
                            "Installing…",
                            false,
                            ButtonVariant.PRIMARY,
                            null
                        )
                        isCopyingObb -> ActionButtonState(
                            "Synchronizing OBB…",
                            false,
                            ButtonVariant.PRIMARY,
                            null
                        )
                        isLaunching -> ActionButtonState(
                            "Starting…",
                            false,
                            ButtonVariant.PRIMARY,
                            null
                        )
                        !isClonedInContainer -> ActionButtonState(
                            stringResource(id = R.string.action_install),
                            true,
                            ButtonVariant.PRIMARY,
                            Icons.Outlined.Download
                        )
                        !hasObb -> ActionButtonState(
                            stringResource(id = R.string.action_sync_obb),
                            true,
                            ButtonVariant.PRIMARY,
                            Icons.Outlined.Sync
                        )
                        else -> ActionButtonState(
                            stringResource(id = R.string.action_launch),
                            true,
                            ButtonVariant.PRIMARY,
                            Icons.Outlined.PlayArrow
                        )
                    }

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true,
                        contentPadding = AppTheme.spacing.md
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(AppRadii.md))
                                        .background(AppTheme.colors.surfaceSubtle)
                                        .border(
                                            1.dp,
                                            AppTheme.colors.outline,
                                            RoundedCornerShape(AppRadii.md)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.india),
                                        contentDescription = gameTitle,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(AppTheme.spacing.md))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gameTitle,
                                        style = AppTheme.typography.titleMedium,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Version ${version.versionName}",
                                        style = AppTheme.typography.bodySmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }

                                AppBadge(text = badgeText, tone = badgeTone)
                            }

                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                            val isRunningThis =
                                isMatchingHostVersion && (isLaunching || isCopyingObb || isInstalling)
                            AppButton(
                                text = cardButtonState.text,
                                onClick = {
                                    when {
                                        isVerComingSoon -> {}
                                        !isGameEnabled -> {}
                                        !isHostGameInstalled -> {
                                            if (onLaunchVersionClick != null) {
                                                onLaunchVersionClick(version)
                                            } else {
                                                onLaunchClick()
                                            }
                                        }
                                        !isMatchingHostVersion -> {}
                                        !isClonedInContainer -> {
                                            if (onInstallVersionClick != null) {
                                                onInstallVersionClick(version)
                                            } else {
                                                onInstallClick()
                                            }
                                        }
                                        !hasObb -> onSyncObbClick()
                                        else -> {
                                            if (onLaunchVersionClick != null) {
                                                onLaunchVersionClick(version)
                                            } else {
                                                onLaunchClick()
                                            }
                                        }
                                    }
                                },
                                enabled = cardButtonState.enabled &&
                                    !isLaunching && !isCopyingObb && !isInstalling,
                                loading = isRunningThis,
                                leadingIcon = if (!isRunningThis) cardButtonState.icon else null,
                                variant = cardButtonState.variant,
                                fullWidth = true
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isCopyingObb || isInstalling,
                    enter = fadeIn(tween(AppMotion.DurationScreen)),
                    exit = fadeOut(tween(AppMotion.DurationState))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = AppTheme.spacing.md
                        ) {
                            AppProgressBar(
                                progress = obbProgress,
                                statusText = progressMessage
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomContentPad))
            }
        }

        if (showOptionsDialog) {
            AppDialog(
                onDismissRequest = onOptionsDismiss,
                title = "Launch options",
                subtitle = "Manage $gameTitle sandbox"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    AppButton(
                        text = if (isClonedInContainer) {
                            "Launch $gameTitle"
                        } else {
                            "Install $gameTitle"
                        },
                        onClick = {
                            onOptionsDismiss()
                            if (isClonedInContainer) onLaunchClick() else onInstallClick()
                        },
                        leadingIcon = Icons.Outlined.PlayArrow,
                        variant = ButtonVariant.PRIMARY,
                        fullWidth = true
                    )
                    AppButton(
                        text = stringResource(id = R.string.action_sync_obb),
                        onClick = {
                            onOptionsDismiss()
                            onSyncObbClick()
                        },
                        leadingIcon = Icons.Outlined.Sync,
                        variant = ButtonVariant.SECONDARY,
                        fullWidth = true
                    )
                    AppButton(
                        text = stringResource(id = R.string.action_clear_credentials),
                        onClick = {
                            onOptionsDismiss()
                            onClearLoginClick()
                        },
                        leadingIcon = Icons.Outlined.DeleteOutline,
                        variant = ButtonVariant.DESTRUCTIVE,
                        fullWidth = true
                    )
                    AppButton(
                        text = stringResource(id = R.string.action_cancel),
                        onClick = onOptionsDismiss,
                        variant = ButtonVariant.TERTIARY,
                        fullWidth = true
                    )
                }
            }
        }

        GameNotInstalledDialog(
            state = gameNotInstalledDialogState,
            onInstallFromPlayStore = onInstallFromPlayStore,
            onDismissRequest = onDismissGameNotInstalledDialog
        )
    }
}

@Preview(name = "Dashboard — Dark", showBackground = true)
@Composable
private fun MainDashboardScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        MainDashboardScreen(
            days = "59",
            hours = "17",
            mins = "42",
            hasObb = true,
            isClonedInContainer = true,
            games = ManagedGame.DEFAULT_GAMES,
            selectedGame = ManagedGame.DEFAULT_BGMI,
            hostInstalledVersionName = "4.5.0",
            hostInstalledVersionCode = 21325L
        )
    }
}

@Preview(name = "Dashboard — Light", showBackground = true)
@Composable
private fun MainDashboardScreenLightPreview() {
    AppTheme(darkTheme = false) {
        MainDashboardScreen(
            days = "59",
            hours = "17",
            mins = "42",
            hasObb = true,
            isClonedInContainer = false,
            games = ManagedGame.DEFAULT_GAMES,
            selectedGame = ManagedGame.DEFAULT_BGMI,
            hostInstalledVersionName = "4.5.0",
            hostInstalledVersionCode = 21325L
        )
    }
}
