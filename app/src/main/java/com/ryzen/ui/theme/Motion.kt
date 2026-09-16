package com.ryzen.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// =====================================================
// MOTION — calm, no spring / bounce / overshoot
// 150ms state, 250ms screen, 300ms sheets
// =====================================================

object AppMotion {
    const val DurationState = 150
    const val DurationScreen = 250
    const val DurationSheet = 300

    // Back-compat names used by existing call sites
    const val DurationMicro = DurationState
    const val DurationFast = DurationState
    const val DurationStandard = DurationScreen
    const val DurationEmphasis = DurationSheet
    const val DurationSlow = DurationSheet

    val EasingStandard = FastOutSlowInEasing
    val EasingEntrance = FastOutSlowInEasing
    val EasingExit = FastOutSlowInEasing

    fun <T> stateTween() = tween<T>(durationMillis = DurationState, easing = EasingStandard)
    fun <T> screenTween() = tween<T>(durationMillis = DurationScreen, easing = EasingStandard)
    fun <T> sheetTween() = tween<T>(durationMillis = DurationSheet, easing = EasingStandard)
}

/**
 * Press feedback is handled by Material ripple only — no scale bounce.
 * Kept as a no-op modifier so existing `.pressScale(...)` call sites compile.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.pressScale(
    targetScale: Float = 1f,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier = composed {
    // Ensure interaction source is remembered if caller passes null (API parity)
    interactionSource ?: remember { MutableInteractionSource() }
    this
}
