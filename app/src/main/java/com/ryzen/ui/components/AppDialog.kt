package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(AppRadii.lg)

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(AppTheme.spacing.lg)
                .fillMaxWidth()
                .background(AppTheme.colors.surfaceElevated, shape)
                .border(1.dp, AppTheme.colors.outline, shape)
                .padding(AppTheme.spacing.xl),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = AppTheme.typography.headlineSmall,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Start
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = subtitle,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

            content()
        }
    }
}
