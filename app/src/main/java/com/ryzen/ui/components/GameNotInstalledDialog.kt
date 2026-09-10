package com.ryzen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ryzen.R
import com.ryzen.model.TargetGame
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

data class GameNotInstalledDialogState(
    val isVisible: Boolean = false,
    val gameTitle: String = "BGMI (BATTLEGROUNDS)",
    val packageName: String = "com.pubg.imobile",
    val iconType: String = "bgmi",
    val isBgmi: Boolean = true,
    val game: TargetGame = TargetGame.BGMI
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameNotInstalledDialog(
    state: GameNotInstalledDialogState,
    onInstallFromPlayStore: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!state.isVisible) return

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(AppTheme.spacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadii.lg))
                .background(AppTheme.colors.surface)
                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppRadii.lg))
                .padding(AppTheme.spacing.xl)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle / Accent notch
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.border)
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // Game Icon with Warning Badge
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(AppRadii.md))
                            .background(AppTheme.colors.surfaceElevated)
                            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppRadii.md)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                id = R.drawable.india
                            ),
                            contentDescription = state.gameTitle.ifBlank { state.game.title },
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Warning chip badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceSubtle)
                            .border(2.dp, AppTheme.colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                Text(
                    text = "${state.gameTitle.ifBlank { state.game.title }} Not Found",
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))

                Text(
                    text = state.packageName.ifBlank { state.game.packageName },
                    style = AppTheme.typography.bodySmall.copy(
                        fontFamily = com.ryzen.ui.theme.MonoFontFamily,
                        fontSize = 12.sp
                    ),
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                // Explanatory Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadii.md))
                        .background(AppTheme.colors.background)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(AppRadii.md))
                        .padding(AppTheme.spacing.md)
                ) {
                    Text(
                        text = "To run ${state.gameTitle.ifBlank { state.game.title }} inside the sandbox container, the official game must be installed from Google Play Store on your device.",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    AppButton(
                        text = "Install from Play Store",
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onInstallFromPlayStore
                    )

                    AppButton(
                        text = "Dismiss",
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismissRequest
                    )
                }
            }
        }
    }
}
