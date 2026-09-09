package com.ryzen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ryzen.ui.theme.AppMotion
import com.ryzen.ui.theme.AppTheme

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
    games: List<ManagedGame> = ManagedGame.DEFAULT_GAMES,
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

    // Transparent root — ambient canvas comes from MAct AppBackground
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TOP BAR (Clean compact title + small expiry indicator)
            AppTopBar(
                title = stringResource(id = R.string.brand_name),
                subtitle = "Virtualization Engine",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(AppTheme.shapes.extraSmall)
                            .background(AppTheme.colors.successTint)
                            .border(1.dp, AppTheme.colors.success.copy(alpha = 0.35f), AppTheme.shapes.extraSmall)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(AppTheme.colors.success)
                        )
                        Text(
                            text = if (isLifetime) "Lifetime" else if (days != "00") "${days}d ${hours}h" else "${hours}h ${mins}m",
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AppTheme.colors.success
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                // 2. TARGET APPS SECTION HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target application",
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = AppTheme.colors.textSecondary
                    )

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                // 3b. DYNAMIC GAME SELECTOR TABS (Seamless switching between BGMI & PUBG GL)
                if (games.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppTheme.shapes.small)
                            .background(AppTheme.colors.surfaceElevated)
                            .border(1.dp, AppTheme.colors.borderSubtle, AppTheme.shapes.small)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        games.forEach { game ->
                            val isSelected = selectedGame.id == game.id
                            val tabLabel = game.getDisplayTitle()
                            val tabBg by animateColorAsState(
                                targetValue = if (isSelected) AppTheme.colors.accentTint else Color.Transparent,
                                animationSpec = tween(AppMotion.DurationFast),
                                label = "gameTabBg"
                            )
                            val tabFg by animateColorAsState(
                                targetValue = if (isSelected) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                animationSpec = tween(AppMotion.DurationFast),
                                label = "gameTabFg"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(AppTheme.shapes.extraSmall)
                                    .background(tabBg)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.dp,
                                                AppTheme.colors.accent.copy(alpha = 0.35f),
                                                AppTheme.shapes.extraSmall
                                            )
                                        } else Modifier
                                    )
                                    .clickable { onSelectGame(game) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    style = AppTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = tabFg
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                // 4. CLEAN & MODERN GAME CARDS (Multi-version support, e.g. 4.5.0 and 4.6.0)
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
                        (hostInstalledVersionName.isNotBlank() && hostInstalledVersionName == version.versionName)
                    )

                    // Tag badge fetched from server
                    val versionTag = version.tag.ifBlank { "Latest" }
                    val (badgeText, badgeTone) = when {
                        isVerComingSoon -> Pair("Coming soon", BadgeTone.ACCENT)
                        !isGameEnabled -> Pair("Disabled", BadgeTone.ERROR)
                        versionTag.equals("LATEST", ignoreCase = true) -> Pair("Latest", BadgeTone.SUCCESS)
                        versionTag.equals("STABLE", ignoreCase = true) -> Pair("Stable", BadgeTone.SUCCESS)
                        versionTag.equals("BETA", ignoreCase = true) -> Pair("Beta", BadgeTone.WARNING)
                        versionTag.equals("TEST", ignoreCase = true) -> Pair("Test", BadgeTone.ACCENT)
                        versionTag.equals("COMING SOON", ignoreCase = true) -> Pair("Coming soon", BadgeTone.ACCENT)
                        else -> Pair(versionTag, BadgeTone.NEUTRAL)
                    }

                    val cardButtonState = when {
                        isVerComingSoon -> {
                            ActionButtonState("Coming soon", false, ButtonVariant.SECONDARY, Icons.Rounded.Schedule)
                        }
                        !isGameEnabled -> {
                            ActionButtonState("Unavailable", false, ButtonVariant.SECONDARY, null)
                        }
                        !isHostGameInstalled -> {
                            ActionButtonState("$gameTitle Not Installed", true, ButtonVariant.PRIMARY, Icons.Rounded.Download)
                        }
                        !isMatchingHostVersion -> {
                            ActionButtonState("Version Not Installed", false, ButtonVariant.SECONDARY, null)
                        }
                        isInstalling -> {
                            ActionButtonState("Installing to Sandbox...", false, ButtonVariant.PRIMARY, null)
                        }
                        isCopyingObb -> {
                            ActionButtonState("Synchronizing OBB...", false, ButtonVariant.PRIMARY, null)
                        }
                        isLaunching -> {
                            ActionButtonState("Starting engine...", false, ButtonVariant.PRIMARY, null)
                        }
                        !isClonedInContainer -> {
                            ActionButtonState("Install", true, ButtonVariant.PRIMARY, Icons.Rounded.Download)
                        }
                        !hasObb -> {
                            ActionButtonState("Synchronize OBB", true, ButtonVariant.PRIMARY, Icons.Rounded.Sync)
                        }
                        else -> {
                            ActionButtonState("Launch", true, ButtonVariant.PRIMARY, Icons.Rounded.PlayArrow)
                        }
                    }

                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.large,
                        elevated = true,
                        contentPadding = AppTheme.spacing.md
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Game Icon (Dedicated icon per game)
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(AppTheme.shapes.medium)
                                        .background(AppTheme.colors.surfaceElevated)
                                        .border(1.dp, AppTheme.colors.borderSubtle, AppTheme.shapes.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedGame.iconType.contains("bgmi", ignoreCase = true) ||
                                        selectedGame.packageName == "com.pubg.imobile") {
                                        Image(
                                            painter = painterResource(id = R.drawable.india),
                                            contentDescription = "$gameTitle Icon",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_pubg_gl),
                                            contentDescription = "$gameTitle Icon",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(AppTheme.spacing.md))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gameTitle,
                                        style = AppTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Version ${version.versionName}",
                                        style = AppTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = AppTheme.colors.textSecondary
                                    )
                                }

                                // Server-driven Status Badge (e.g. LATEST, BETA, TEST, STABLE, COMING SOON)
                                AppBadge(
                                    text = badgeText,
                                    tone = badgeTone
                                )
                            }

                            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                            // Action Button
                            val isRunningThis = isMatchingHostVersion && (isLaunching || isCopyingObb || isInstalling)
                            AppButton(
                                text = cardButtonState.text,
                                onClick = {
                                    when {
                                        isVerComingSoon -> {}
                                        !isGameEnabled -> {}
                                        !isHostGameInstalled -> {
                                            if (onLaunchVersionClick != null) onLaunchVersionClick(version) else onLaunchClick()
                                        }
                                        !isMatchingHostVersion -> {}
                                        !isClonedInContainer -> {
                                            if (onInstallVersionClick != null) onInstallVersionClick(version) else onInstallClick()
                                        }
                                        !hasObb -> onSyncObbClick()
                                        else -> {
                                            if (onLaunchVersionClick != null) onLaunchVersionClick(version) else onLaunchClick()
                                        }
                                    }
                                },
                                enabled = cardButtonState.enabled && !isLaunching && !isCopyingObb && !isInstalling,
                                loading = isRunningThis,
                                leadingIcon = if (!isRunningThis) cardButtonState.icon else null,
                                variant = cardButtonState.variant,
                                fullWidth = true
                            )
                        }
                    }
                }

                // 5. PROGRESS CARD (Visible only during active operations)
                AnimatedVisibility(
                    visible = isCopyingObb || isInstalling,
                    enter = fadeIn(tween(AppMotion.DurationStandard)) +
                        slideInVertically(tween(AppMotion.DurationStandard)) { it / 8 },
                    exit = fadeOut(tween(AppMotion.DurationFast)) +
                        slideOutVertically(tween(AppMotion.DurationFast)) { it / 10 }
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

                Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // 7. START OPTIONS MODAL DIALOG
        if (showOptionsDialog) {
            AppDialog(
                onDismissRequest = onOptionsDismiss,
                title = "Launch Options",
                subtitle = "Manage $gameTitle sandbox environment"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    AppButton(
                        text = if (isClonedInContainer) "Launch $gameTitle" else "Install $gameTitle to Sandbox",
                        onClick = {
                            onOptionsDismiss()
                            if (isClonedInContainer) onLaunchClick() else onInstallClick()
                        },
                        leadingIcon = Icons.Rounded.PlayArrow,
                        variant = ButtonVariant.PRIMARY,
                        fullWidth = true
                    )

                    AppButton(
                        text = "Sync OBB assets",
                        onClick = {
                            onOptionsDismiss()
                            onSyncObbClick()
                        },
                        leadingIcon = Icons.Rounded.Sync,
                        variant = ButtonVariant.SECONDARY,
                        fullWidth = true
                    )

                    AppButton(
                        text = "Clear login credentials",
                        onClick = {
                            onOptionsDismiss()
                            onClearLoginClick()
                        },
                        leadingIcon = Icons.Rounded.DeleteOutline,
                        variant = ButtonVariant.DESTRUCTIVE,
                        fullWidth = true
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                    AppButton(
                        text = "Cancel",
                        onClick = onOptionsDismiss,
                        variant = ButtonVariant.TERTIARY,
                        fullWidth = true
                    )
                }
            }
        }

        // Game Not Installed Dialog
        GameNotInstalledDialog(
            state = gameNotInstalledDialogState,
            onInstallFromPlayStore = onInstallFromPlayStore,
            onDismissRequest = onDismissGameNotInstalledDialog
        )
    }
}

// =====================================================
// PREVIEWS (Both Light & Dark Themes Verified)
// =====================================================

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun MainDashboardScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        MainDashboardScreen(
            days = "59",
            hours = "17",
            mins = "42",
            secs = "10",
            isLifetime = false,
            isLaunching = false,
            isInstalling = false,
            isCopyingObb = false,
            obbProgress = 0.45f,
            progressMessage = "Copying OBB: 45%",
            hasObb = true,
            isClonedInContainer = true,
            games = ManagedGame.DEFAULT_GAMES,
            selectedGame = ManagedGame.DEFAULT_BGMI,
            selectedVersion = ManagedGame.DEFAULT_BGMI.versions[0],
            isHostGameInstalled = true,
            hostInstalledVersionName = "4.5.0",
            hostInstalledVersionCode = 21325L,
            onLaunchClick = {},
            onInstallClick = {},
            onSyncObbClick = {},
            onClearLoginClick = {},
            showOptionsDialog = false,
            onOptionsDismiss = {},
            onOptionsClick = {}
        )
    }
}

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun MainDashboardScreenLightPreview() {
    AppTheme(darkTheme = false) {
        MainDashboardScreen(
            days = "59",
            hours = "17",
            mins = "42",
            secs = "10",
            isLifetime = false,
            isLaunching = false,
            isInstalling = false,
            isCopyingObb = false,
            obbProgress = 0.45f,
            progressMessage = "Copying OBB: 45%",
            hasObb = true,
            isClonedInContainer = false,
            games = ManagedGame.DEFAULT_GAMES,
            selectedGame = ManagedGame.DEFAULT_BGMI,
            selectedVersion = ManagedGame.DEFAULT_BGMI.versions[0],
            isHostGameInstalled = true,
            hostInstalledVersionName = "4.5.0",
            hostInstalledVersionCode = 21325L,
            onLaunchClick = {},
            onInstallClick = {},
            onSyncObbClick = {},
            onClearLoginClick = {},
            showOptionsDialog = false,
            onOptionsDismiss = {},
            onOptionsClick = {}
        )
    }
}
