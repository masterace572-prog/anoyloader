package com.ryzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =====================================================
// SHAPE SYSTEM — slightly softer radii for modern feel
// =====================================================

object AppRadii {
    val xs = 8.dp      // Chips, badges, tags
    val sm = 12.dp     // Inputs, buttons, compact items
    val md = 16.dp     // Standard cards, segmented panels
    val lg = 22.dp     // Sheets, dialogs, hero cards
    val pill = 999.dp  // Full pill
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadii.xs),
    small = RoundedCornerShape(AppRadii.sm),
    medium = RoundedCornerShape(AppRadii.md),
    large = RoundedCornerShape(AppRadii.lg),
    extraLarge = RoundedCornerShape(28.dp)
)
