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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzen.ui.components.AppBadge
import com.ryzen.ui.components.AppButton
import com.ryzen.ui.components.AppCard
import com.ryzen.ui.components.BadgeTone
import com.ryzen.ui.components.ButtonVariant
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme

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

    com.ryzen.ui.theme.AppBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Header Area
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
                                text = "Crash Report",
                                style = AppTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Normal
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "An unhandled exception was captured",
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    AppBadge(
                        text = "Fatal",
                        tone = BadgeTone.ERROR
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                // Error Summary Card
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppTheme.colors.surfaceElevated,
                    contentPadding = AppTheme.spacing.md
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Error summary",
                            style = AppTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.sp
                            ),
                            color = AppTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                        Text(
                            text = errorMessage ?: "Unknown runtime exception",
                            style = AppTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = AppTheme.colors.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                // Stack Trace Monospace Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.medium)
                        .background(AppTheme.colors.surface)
                        .border(1.dp, AppTheme.colors.border, AppTheme.shapes.medium)
                        .padding(AppTheme.spacing.md)
                ) {
                    Text(
                        text = stackTrace ?: "No detailed stack trace available",
                        style = AppTheme.typography.bodySmall.copy(
                            fontFamily = com.ryzen.ui.theme.MonoFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
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

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
            ) {
                AppButton(
                    text = "Copy log",
                    onClick = onCopyClick,
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Outlined.ContentCopy,
                    modifier = Modifier.weight(1f),
                    fullWidth = false
                )

                AppButton(
                    text = "Restart app",
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

@Preview(name = "Crash Screen - Dark", showBackground = true)
@Composable
fun PreviewCrashScreenDark() {
    AppTheme(darkTheme = true) {
        CrashScreen(
            errorMessage = "java.lang.NullPointerException: Attempt to invoke virtual method",
            stackTrace = "at com.ryzen.MAct.onCreate(MAct.kt:75)\nat android.app.Activity.performCreate(Activity.java:8290)\nat android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1329)",
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}

@Preview(name = "Crash Screen - Light", showBackground = true)
@Composable
fun PreviewCrashScreenLight() {
    AppTheme(darkTheme = false) {
        CrashScreen(
            errorMessage = "java.lang.NullPointerException: Attempt to invoke virtual method",
            stackTrace = "at com.ryzen.MAct.onCreate(MAct.kt:75)\nat android.app.Activity.performCreate(Activity.java:8290)",
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}
