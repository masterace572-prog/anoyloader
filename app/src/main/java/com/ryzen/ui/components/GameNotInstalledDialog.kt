package com.ryzen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.model.TargetGame
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.MonoFontFamily

data class GameNotInstalledDialogState(
    val isVisible: Boolean = false,
    val gameTitle: String = "BGMI",
    val packageName: String = "com.pubg.imobile",
    val iconType: String = "bgmi",
    val isBgmi: Boolean = true,
    val game: TargetGame = TargetGame.BGMI
)

@Composable
fun GameNotInstalledDialog(
    state: GameNotInstalledDialogState,
    onInstallFromPlayStore: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!state.isVisible) return

    val title = state.gameTitle.ifBlank { state.game.title }
    val pkg = state.packageName.ifBlank { state.game.packageName }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = "$title not found",
        subtitle = "Install the official game to continue"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.india),
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppRadii.md))
                    .background(AppTheme.colors.surfaceSubtle)
                    .border(1.dp, AppTheme.colors.outline, RoundedCornerShape(AppRadii.md))
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.md))

            Text(
                text = pkg,
                style = AppTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                color = AppTheme.colors.textTertiary
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.sm))

            Text(
                text = "To run $title inside the sandbox, install the official app from Google Play on this device.",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

            AppButton(
                text = "Install from Play Store",
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                fullWidth = true,
                onClick = onInstallFromPlayStore
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

            AppButton(
                text = "Dismiss",
                variant = ButtonVariant.SECONDARY,
                fullWidth = true,
                onClick = onDismissRequest
            )
        }
    }
}
