package com.ryzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =====================================================
// SHAPE — buttons 10, inputs 8, cards 12, dialogs 16,
// sheets 24 top, avatars circle
// =====================================================

object AppRadii {
    val xs = 8.dp       // inputs
    val sm = 10.dp      // buttons
    val md = 12.dp      // cards, images
    val lg = 16.dp      // dialogs
    val xl = 24.dp      // bottom sheets (top)
    val pill = 999.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadii.xs),
    small = RoundedCornerShape(AppRadii.sm),
    medium = RoundedCornerShape(AppRadii.md),
    large = RoundedCornerShape(AppRadii.lg),
    extraLarge = RoundedCornerShape(AppRadii.xl)
)
