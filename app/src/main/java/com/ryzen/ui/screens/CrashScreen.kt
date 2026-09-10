package com.ryzen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ryzen.R
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.theme.AppBackground
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.MonoFontFamily

@Composable
fun CrashScreen(
    errorMessage: String?,
    stackTrace: String?,
    onCopyClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    AppBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.screenHorizontal)
                .padding(vertical = AppTheme.spacing.lg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(AppRadii.sm))
                                .background(AppTheme.colors.surfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BugReport,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                        Column {
                            Text(
                                text = stringResource(id = R.string.crash_title),
                                style = AppTheme.typography.titleLarge,
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.crash_subtitle),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                    AppBadge(
                        text = stringResource(id = R.string.status_fatal),
                        tone = BadgeTone.ERROR
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.error_summary),
                            style = AppTheme.typography.labelMedium,
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                        Text(
                            text = errorMessage ?: "Unknown runtime exception",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadii.md))
                        .background(AppTheme.colors.surface)
                        .border(1.dp, AppTheme.colors.outline, RoundedCornerShape(AppRadii.md))
                        .padding(AppTheme.spacing.md)
                ) {
                    Text(
                        text = stackTrace ?: "No detailed stack trace available",
                        style = AppTheme.typography.bodySmall.copy(
                            fontFamily = MonoFontFamily
                        ),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
            ) {
                AppButton(
                    text = stringResource(id = R.string.action_copy_log),
                    onClick = onCopyClick,
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Outlined.ContentCopy,
                    modifier = Modifier.weight(1f),
                    fullWidth = false
                )
                AppButton(
                    text = stringResource(id = R.string.action_restart_app),
                    onClick = onRestartClick,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Outlined.Refresh,
                    modifier = Modifier.weight(1f),
                    fullWidth = false
                )
            }
        }
    }
}

@Preview(name = "Crash — Dark", showBackground = true)
@Composable
fun PreviewCrashScreenDark() {
    AppTheme(darkTheme = true) {
        CrashScreen(
            errorMessage = "java.lang.NullPointerException",
            stackTrace = "at com.ryzen.MAct.onCreate(MAct.kt:75)\nat android.app.Activity.performCreate(Activity.java:8290)",
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}

@Preview(name = "Crash — Light", showBackground = true)
@Composable
fun PreviewCrashScreenLight() {
    AppTheme(darkTheme = false) {
        CrashScreen(
            errorMessage = "java.lang.NullPointerException",
            stackTrace = "at com.ryzen.MAct.onCreate(MAct.kt:75)",
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}
