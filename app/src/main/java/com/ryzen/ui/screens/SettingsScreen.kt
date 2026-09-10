package com.ryzen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.model.ManagedGame
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppTopBar
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.components.SectionHeader
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.MonoFontFamily

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
    val bottomContentPad = 72.dp

    val displayKey = if (isKeyRevealed) {
        keyText.ifBlank { "—" }
    } else if (keyText.length > 8) {
        "${keyText.take(4)}········${keyText.takeLast(4)}"
    } else if (keyText.isNotEmpty()) {
        "············"
    } else {
        "—"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppTopBar(
                title = stringResource(id = R.string.settings_title),
                subtitle = stringResource(id = R.string.settings_subtitle)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // License
                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Outlined.VpnKey,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.active_license),
                                    style = AppTheme.typography.titleSmall,
                                    color = AppTheme.colors.textPrimary
                                )
                            }
                            AppBadge(
                                text = if (isLifetime) {
                                    stringResource(id = R.string.status_lifetime)
                                } else {
                                    stringResource(id = R.string.status_active)
                                },
                                tone = if (isLifetime) BadgeTone.ACCENT else BadgeTone.SUCCESS
                            )
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppRadii.xs))
                                .background(AppTheme.colors.surfaceSubtle)
                                .border(
                                    1.dp,
                                    AppTheme.colors.outline,
                                    RoundedCornerShape(AppRadii.xs)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayKey,
                                style = AppTheme.typography.bodyMedium.copy(
                                    fontFamily = MonoFontFamily
                                ),
                                color = AppTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isKeyRevealed = !isKeyRevealed },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isKeyRevealed) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = "Toggle key visibility",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (keyText.isNotBlank()) {
                                            val cm = context.getSystemService(
                                                Context.CLIPBOARD_SERVICE
                                            ) as ClipboardManager
                                            cm.setPrimaryClip(
                                                ClipData.newPlainText("License Key", keyText)
                                            )
                                            Toast.makeText(
                                                context,
                                                "Key copied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy key",
                                        tint = AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AppTheme.colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = when {
                                    isLifetime -> "Unlimited lifetime subscription"
                                    expiryDateStr.isNotBlank() ->
                                        "Expires $expiryDateStr · ${days}d ${hours}h left"
                                    else -> "Remaining ${days}d ${hours}h ${mins}m"
                                },
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                SectionHeader(text = stringResource(id = R.string.sandbox_maintenance))

                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppButton(
                            text = "Clear BGMI login data",
                            onClick = { onClearLoginClick(selectedGame) },
                            leadingIcon = Icons.Outlined.CleaningServices,
                            variant = ButtonVariant.SECONDARY
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                        AppButton(
                            text = "Reset guest",
                            onClick = { showConfirmResetGuestDialog = true },
                            leadingIcon = Icons.Outlined.RestartAlt,
                            variant = ButtonVariant.SECONDARY
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                        AppButton(
                            text = "Clear BGMI resources",
                            onClick = { showConfirmClearDataDialog = true },
                            leadingIcon = Icons.Outlined.DeleteSweep,
                            variant = ButtonVariant.DESTRUCTIVE
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                SectionHeader(text = stringResource(id = R.string.support_inquiries))

                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Need help or a key extension?",
                            style = AppTheme.typography.titleSmall,
                            color = AppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                        Text(
                            text = "Message the administrator on Telegram for support, reports, and subscriptions.",
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.md))
                        AppButton(
                            text = stringResource(id = R.string.contact_admin_telegram),
                            onClick = onContactAdminClick,
                            leadingPainter = painterResource(id = R.drawable.ic_telegram_app),
                            variant = ButtonVariant.PRIMARY
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${stringResource(id = R.string.brand_name)} v$appVersionName · $appVersionCode",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                    Text(
                        text = stringResource(id = R.string.security_footer),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(bottomContentPad))
            }
        }

        if (showConfirmClearDataDialog) {
            AppDialog(
                onDismissRequest = { showConfirmClearDataDialog = false },
                title = "Clear BGMI resources?",
                subtitle = "Downloaded maps and cache will be removed"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    Text(
                        text = "OBB files are kept. In-game resources must be redownloaded after clearing.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    AppButton(
                        text = "Clear resources",
                        onClick = {
                            showConfirmClearDataDialog = false
                            onClearGameDataClick(selectedGame)
                        },
                        leadingIcon = Icons.Outlined.DeleteSweep,
                        variant = ButtonVariant.DESTRUCTIVE
                    )
                    AppButton(
                        text = stringResource(id = R.string.action_cancel),
                        onClick = { showConfirmClearDataDialog = false },
                        variant = ButtonVariant.TERTIARY
                    )
                }
            }
        }

        if (showConfirmResetGuestDialog) {
            AppDialog(
                onDismissRequest = { showConfirmResetGuestDialog = false },
                title = "Reset BGMI guest?",
                subtitle = "Generate a fresh guest identity"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
                ) {
                    Text(
                        text = "Wipes guest login, device ID, and related caches, then writes a new random guest UUID. OBB is kept.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    AppButton(
                        text = "Reset guest",
                        onClick = {
                            showConfirmResetGuestDialog = false
                            onResetGuestClick()
                        },
                        leadingIcon = Icons.Outlined.RestartAlt,
                        variant = ButtonVariant.PRIMARY
                    )
                    AppButton(
                        text = stringResource(id = R.string.action_cancel),
                        onClick = { showConfirmResetGuestDialog = false },
                        variant = ButtonVariant.TERTIARY
                    )
                }
            }
        }
    }
}
