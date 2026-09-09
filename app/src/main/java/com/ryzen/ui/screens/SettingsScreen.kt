package com.ryzen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ryzen.R
import com.ryzen.model.ManagedGame
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppTopBar
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    keyText: String,
    isLifetime: Boolean = false,
    expiryDateStr: String = "",
    days: String = "00",
    hours: String = "00",
    mins: String = "00",
    games: List<ManagedGame> = emptyList(),
    selectedGame: ManagedGame = ManagedGame.DEFAULT_BGMI,
    onSelectGame: (ManagedGame) -> Unit = {},
    onClearLoginClick: (ManagedGame) -> Unit = {},
    onClearGameDataClick: (ManagedGame) -> Unit = {},
    onResetGuestClick: () -> Unit = {},
    onContactAdminClick: () -> Unit = {},
    appVersionName: String = "1.0",
    appVersionCode: Long = 1L
) {
    val context = LocalContext.current
    var isKeyRevealed by remember { mutableStateOf(false) }
    var showConfirmClearDataDialog by remember { mutableStateOf(false) }
    var showConfirmResetGuestDialog by remember { mutableStateOf(false) }
    val isBgmiSelected = selectedGame.packageName == "com.pubg.imobile"

    val displayKey = if (isKeyRevealed) {
        keyText.ifBlank { "NONE" }
    } else {
        if (keyText.length > 8) {
            "${keyText.take(4)}••••••••${keyText.takeLast(4)}"
        } else if (keyText.isNotEmpty()) {
            "••••••••••••"
        } else {
            "NONE"
        }
    }

    // Transparent root — ambient canvas comes from MAct AppBackground
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TOP BAR
            AppTopBar(
                title = "Settings",
                subtitle = "License & Sandbox Maintenance"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // 2. LICENSE CARD
                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.large,
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VpnKey,
                                    contentDescription = "Key",
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Active License",
                                    style = AppTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppTheme.colors.textPrimary
                                )
                            }

                            AppBadge(
                                text = if (isLifetime) "Lifetime" else "Active",
                                tone = if (isLifetime) BadgeTone.ACCENT else BadgeTone.SUCCESS
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Key display box with Copy and Reveal actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppTheme.shapes.small)
                                .background(AppTheme.colors.surfaceElevated)
                                .border(1.dp, AppTheme.colors.borderSubtle, AppTheme.shapes.small)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayKey,
                                style = AppTheme.typography.bodyMedium.copy(
                                    fontFamily = com.ryzen.ui.theme.MonoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AppTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isKeyRevealed = !isKeyRevealed },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isKeyRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle Key",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (keyText.isNotBlank()) {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("License Key", keyText))
                                            Toast.makeText(context, "Key copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy Key",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Expiry Meta
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Expiry",
                                tint = AppTheme.colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLifetime) {
                                    "Unlimited Lifetime Subscription"
                                } else if (expiryDateStr.isNotBlank()) {
                                    "Expires: $expiryDateStr ($days days, $hours hrs remaining)"
                                } else {
                                    "Remaining: $days days, $hours hrs, $mins mins"
                                },
                                style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // 3. GAME DATA & CLEANING TOOLS
                Text(
                    text = "Sandbox Maintenance & Reset",
                    style = AppTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.large,
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select Target Game",
                            style = AppTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = AppTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Game selection tabs
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
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(AppTheme.shapes.extraSmall)
                                            .background(
                                                if (isSelected) AppTheme.colors.accentTint
                                                else Color.Transparent
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    1.dp,
                                                    AppTheme.colors.accent.copy(alpha = 0.35f),
                                                    AppTheme.shapes.extraSmall
                                                )
                                                else Modifier
                                            )
                                            .clickable { onSelectGame(game) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabLabel,
                                            style = AppTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        val gameShortName = if (selectedGame.getDisplayTitle().contains("PUBG", ignoreCase = true)) "PUBG" else "BGMI"

                        // Clear Login Button (Operation 1)
                        AppButton(
                            text = "Clear $gameShortName Login data",
                            onClick = { onClearLoginClick(selectedGame) },
                            leadingIcon = Icons.Rounded.CleaningServices,
                            variant = ButtonVariant.SECONDARY
                        )

                        // BGMI-only: full guest identity reset (device_id + cache wipe)
                        if (isBgmiSelected) {
                            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                            AppButton(
                                text = "Reset Guest",
                                onClick = { showConfirmResetGuestDialog = true },
                                leadingIcon = Icons.Rounded.RestartAlt,
                                variant = ButtonVariant.PRIMARY
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        // Clear Resources Data Button (Operation 2)
                        AppButton(
                            text = "Clear $gameShortName Resources data",
                            onClick = { showConfirmClearDataDialog = true },
                            leadingIcon = Icons.Rounded.DeleteSweep,
                            variant = ButtonVariant.DESTRUCTIVE
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // 4. COMMUNITY & CONTACT ADMIN
                Text(
                    text = "Support & Inquiries",
                    style = AppTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.large,
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Need Assistance or Key Extension?",
                            style = AppTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reach out to the official administrator on Telegram for customer support, bug reports, and subscriptions.",
                            style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = AppTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                        AppButton(
                            text = "Contact Admin on Telegram",
                            onClick = onContactAdminClick,
                            leadingPainter = painterResource(id = R.drawable.ic_telegram_app),
                            variant = ButtonVariant.PRIMARY
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                // 5. APP VERSION FOOTER
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.brand_name) + " v$appVersionName (Build $appVersionCode)",
                        style = AppTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "BlackBoxCore Virtual Sandboxing Engine • Anti-Tamper Security",
                        style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AppTheme.colors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(110.dp)) // Padding for floating dock navigation bar
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // Confirmation Dialog for Full Game Data Clear
        if (showConfirmClearDataDialog) {
            val gameShortName = if (selectedGame.getDisplayTitle().contains("PUBG", ignoreCase = true)) "PUBG" else "BGMI"
            AppDialog(
                onDismissRequest = { showConfirmClearDataDialog = false },
                title = "Clear $gameShortName Resources Data?",
                subtitle = "Reset resources for $gameShortName"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    Text(
                        text = "Are you sure you want to clear all downloaded resources and cache for $gameShortName?\n\nThe OBB file will be preserved, but in-game maps and resources must be redownloaded.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )

                    AppButton(
                        text = "Clear $gameShortName Resources data",
                        onClick = {
                            showConfirmClearDataDialog = false
                            onClearGameDataClick(selectedGame)
                        },
                        leadingIcon = Icons.Rounded.DeleteSweep,
                        variant = ButtonVariant.DESTRUCTIVE
                    )

                    AppButton(
                        text = "Cancel",
                        onClick = { showConfirmClearDataDialog = false },
                        variant = ButtonVariant.TERTIARY
                    )
                }
            }
        }

        // BGMI-only Reset Guest confirmation
        if (showConfirmResetGuestDialog && isBgmiSelected) {
            AppDialog(
                onDismissRequest = { showConfirmResetGuestDialog = false },
                title = "Reset BGMI Guest?",
                subtitle = "Generate a fresh guest identity"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    Text(
                        text = "This will wipe BGMI guest login, device ID, caches, and related sandbox files, then write a new random guest UUID.\n\nOBB is kept. Controls/settings under SaveGames may be cleared.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )

                    AppButton(
                        text = "Reset Guest",
                        onClick = {
                            showConfirmResetGuestDialog = false
                            onResetGuestClick()
                        },
                        leadingIcon = Icons.Rounded.RestartAlt,
                        variant = ButtonVariant.PRIMARY
                    )

                    AppButton(
                        text = "Cancel",
                        onClick = { showConfirmResetGuestDialog = false },
                        variant = ButtonVariant.TERTIARY
                    )
                }
            }
        }
    }
}
