package com.ryzen.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ryzen.ui.theme.AppTheme

/**
 * Small section label above a group of cards/rows.
 * Uses labelMedium + textSecondary — no uppercase tracking hacks.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = AppTheme.typography.labelMedium,
        color = AppTheme.colors.textSecondary,
        modifier = modifier.padding(bottom = AppTheme.spacing.xs)
    )
}
