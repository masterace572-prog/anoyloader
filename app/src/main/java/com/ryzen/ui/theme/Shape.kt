package com.ryzen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =====================================================
// SHAPE — buttons 14, inputs 12, cards 20, dialogs 24,
// sheets 24 top, avatars circle
// =====================================================

object AppRadii {
    val xs = 12.dp       // inputs
    val sm = 14.dp      // buttons
    val md = 20.dp      // cards, images
    val lg = 24.dp      // dialogs
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
