package com.ryzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =====================================================
// SHAPE SYSTEM (Restrained, Consistent Geometry)
// =====================================================

object AppRadii {
    val xs = 6.dp      // Chips, badges, tags
    val sm = 10.dp     // Inputs, buttons, compact items
    val md = 14.dp     // Standard cards, segmented panels
    val lg = 20.dp     // Sheets, dialogs, hero cards
    val pill = 999.dp  // Full pill
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadii.xs),
    small = RoundedCornerShape(AppRadii.sm),
    medium = RoundedCornerShape(AppRadii.md),
    large = RoundedCornerShape(AppRadii.lg),
    extraLarge = RoundedCornerShape(24.dp)
)
