package com.ryzen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.AppDialog
import com.ryzen.ui.components.AppListRow
import com.ryzen.ui.components.AppRowGroup
import com.ryzen.ui.components.AppTopBar
import com.ryzen.ui.components.ButtonVariant
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

    val displayKey = when {
        isKeyRevealed -> keyText.ifBlank { "—" }
        keyText.length > 8 -> "${keyText.take(4)}  ····  ${keyText.takeLast(4)}"
        keyText.isNotEmpty() -> "········"
        else -> "—"
    }

    val expiryLine = when {
        isLifetime -> "Lifetime"
        else -> "$days:$hours:$mins remaining"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AppTopBar(title = "Settings")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                AppCard(
                    elevated = true,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "License",
                            style = AppTheme.typography.labelMedium,
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppRadii.xs))
                                .background(AppTheme.colors.surfaceSubtle)
                                .padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayKey,
                                style = AppTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                color = AppTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isKeyRevealed = !isKeyRevealed },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isKeyRevealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (keyText.isNotBlank()) {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("License Key", keyText))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = expiryLine,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                AppRowGroup {
                    AppListRow(
                        title = "Clear login",
                        leadingIcon = Icons.Outlined.CleaningServices,
                        showDivider = true,
                        onClick = { onClearLoginClick(selectedGame) }
                    )
                    AppListRow(
                        title = "Reset guest",
                        leadingIcon = Icons.Outlined.RestartAlt,
                        showDivider = true,
                        onClick = { showConfirmResetGuestDialog = true }
                    )
                    AppListRow(
                        title = "Clear cache",
                        leadingIcon = Icons.Outlined.DeleteSweep,
                        destructive = true,
                        onClick = { showConfirmClearDataDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AppButton(
                    text = "Telegram",
                    onClick = onContactAdminClick,
                    leadingPainter = painterResource(id = R.drawable.ic_telegram_app),
                    variant = ButtonVariant.SECONDARY,
                    fullWidth = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "v$appVersionName",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.textTertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        if (showConfirmClearDataDialog) {
            AppDialog(
                onDismissRequest = { showConfirmClearDataDialog = false },
                title = "Clear cache?"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "Clear",
                        onClick = {
                            showConfirmClearDataDialog = false
                            onClearGameDataClick(selectedGame)
                        },
                        variant = ButtonVariant.DESTRUCTIVE,
                        fullWidth = true
                    )
                    AppButton(
                        text = "Cancel",
                        onClick = { showConfirmClearDataDialog = false },
                        variant = ButtonVariant.TERTIARY,
                        fullWidth = true
                    )
                }
            }
        }

        if (showConfirmResetGuestDialog) {
            AppDialog(
                onDismissRequest = { showConfirmResetGuestDialog = false },
                title = "Reset guest?"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "Reset",
                        onClick = {
                            showConfirmResetGuestDialog = false
                            onResetGuestClick()
                        },
                        variant = ButtonVariant.PRIMARY,
                        fullWidth = true
                    )
                    AppButton(
                        text = "Cancel",
                        onClick = { showConfirmResetGuestDialog = false },
                        variant = ButtonVariant.TERTIARY,
                        fullWidth = true
                    )
                }
            }
        }
    }
}
