package com.ryzen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ryzen.ui.theme.AppRadii
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.TabularNumberStyle

@Composable
fun AppCountdownTimer(
    days: String,
    hours: String,
    mins: String,
    secs: String,
    modifier: Modifier = Modifier,
    isLifetime: Boolean = false
) {
    val label = if (isLifetime) {
        "Lifetime"
    } else {
        "$days:$hours:$mins:$secs"
    }

    Box(
        modifier = modifier
            .background(AppTheme.colors.surfaceSubtle, RoundedCornerShape(AppRadii.xs))
            .border(1.dp, AppTheme.colors.outline, RoundedCornerShape(AppRadii.xs))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TabularNumberStyle,
            color = AppTheme.colors.textPrimary
        )
    }
}
