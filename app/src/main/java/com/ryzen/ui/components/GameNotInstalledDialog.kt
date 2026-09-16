package com.ryzen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ryzen.model.TargetGame
import com.ryzen.ui.theme.AppTheme

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

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = "$title not installed"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppButton(
                text = "Get on Play Store",
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                fullWidth = true,
                onClick = onInstallFromPlayStore
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
            AppButton(
                text = "Cancel",
                variant = ButtonVariant.TERTIARY,
                fullWidth = true,
                onClick = onDismissRequest
            )
        }
    }
}
