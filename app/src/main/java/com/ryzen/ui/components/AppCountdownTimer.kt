package com.ryzen.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzen.ui.theme.AppTheme
import com.ryzen.ui.theme.TabularNumberStyle

@Composable
fun AppCountdownTimer(
    days: String,
    hours: String,
    mins: String,
    secs: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimerUnitBlock(value = days, label = "Days", modifier = Modifier.weight(1f))
        ColonSeparator()
        TimerUnitBlock(value = hours, label = "Hours", modifier = Modifier.weight(1f))
        ColonSeparator()
        TimerUnitBlock(value = mins, label = "Mins", modifier = Modifier.weight(1f))
        ColonSeparator()
        TimerUnitBlock(value = secs, label = "Secs", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TimerUnitBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(com.ryzen.ui.theme.AppRadii.sm))
            .background(AppTheme.colors.surfaceVariant)
            .border(1.dp, AppTheme.colors.borderSubtle, androidx.compose.foundation.shape.RoundedCornerShape(com.ryzen.ui.theme.AppRadii.sm))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TabularNumberStyle,
                color = AppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.textTertiary
            )
        }
    }
}

@Composable
private fun ColonSeparator() {
    Text(
        text = ":",
        style = AppTheme.typography.titleMedium,
        color = AppTheme.colors.textTertiary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
