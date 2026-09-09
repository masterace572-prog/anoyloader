package com.ryzen.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// =====================================================
// MOTION SYSTEM (Restrained Micro-interactions & Polish)
// =====================================================

object AppMotion {
    const val DurationMicro = 100
    const val DurationStandard = 200
    const val DurationEmphasis = 300

    val EasingStandard = FastOutSlowInEasing
    val EasingEntrance = LinearOutSlowInEasing
    val EasingExit = FastOutLinearInEasing
}

/**
 * Standardized press scale feedback (scales to 0.97f on press).
 * Never bounces or overshoots.
 */
fun Modifier.pressScale(
    targetScale: Float = 0.97f,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationMicro,
            easing = AppMotion.EasingStandard
        ),
        label = "pressScaleAnimation"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
