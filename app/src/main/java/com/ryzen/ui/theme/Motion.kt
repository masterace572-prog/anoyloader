package com.ryzen.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// =====================================================
// MOTION — snappy springs + soft entrances
// =====================================================

object AppMotion {
    const val DurationMicro = 90
    const val DurationFast = 160
    const val DurationStandard = 240
    const val DurationEmphasis = 360
    const val DurationSlow = 480

    val EasingStandard = FastOutSlowInEasing
    val EasingEntrance = LinearOutSlowInEasing
    val EasingExit = FastOutLinearInEasing

    val SpringSnappy = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium
    )

    val SpringSoft = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun fadeSlideIn(delayMs: Int = 0) = fadeIn(
        animationSpec = tween(DurationStandard, delayMillis = delayMs, easing = EasingEntrance)
    ) + slideInVertically(
        animationSpec = tween(DurationStandard, delayMillis = delayMs, easing = EasingEntrance),
        initialOffsetY = { it / 12 }
    )

    fun fadeSlideOut() = fadeOut(
        animationSpec = tween(DurationFast, easing = EasingExit)
    ) + slideOutVertically(
        animationSpec = tween(DurationFast, easing = EasingExit),
        targetOffsetY = { it / 16 }
    )
}

/**
 * Soft press scale (0.97) with spring recovery — feels smoother than fixed tween.
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
        animationSpec = if (isPressed) {
            tween(durationMillis = AppMotion.DurationMicro, easing = AppMotion.EasingExit)
        } else {
            AppMotion.SpringSnappy
        },
        label = "pressScaleAnimation"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Entrance fade + slight rise. Call with start=true after composition.
 */
@Composable
fun Modifier.entrance(
    visible: Boolean,
    delayMs: Int = 0
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationEmphasis,
            delayMillis = delayMs,
            easing = AppMotion.EasingEntrance
        ),
        label = "entranceAlpha"
    )
    val translate by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(
            durationMillis = AppMotion.DurationEmphasis,
            delayMillis = delayMs,
            easing = AppMotion.EasingEntrance
        ),
        label = "entranceY"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = translate
    }
}
