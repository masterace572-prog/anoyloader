package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================
// COLOR TOKENS — slate surfaces, accessible blue accent
// No gradients. Every UI color must come from AppColors.
// =====================================================

// Light
val LightBackground = Color(0xFFF3F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFEDF2FA)
val LightOutline = Color(0xFFD7E1EF)
val LightTextPrimary = Color(0xFF15243C)
val LightTextSecondary = Color(0xFF53657F)
val LightTextTertiary = Color(0xFF60738E)
val LightAccent = Color(0xFF275DDB)
val LightAccentPressed = Color(0xFF204EB8)
val LightOnAccent = Color(0xFFFFFFFF)
val LightAccentSubtle = Color(0xFFE9F0FF)
val LightError = Color(0xFFB72C45)
val LightSuccess = Color(0xFF16724F)
val LightWarning = Color(0xFF91600E)

// Dark
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF111B2E)
val DarkSurfaceElevated = Color(0xFF172338)
val DarkSurfaceSubtle = Color(0xFF1C2A40)
val DarkOutline = Color(0xFF2A3B54)
val DarkTextPrimary = Color(0xFFEDF3FF)
val DarkTextSecondary = Color(0xFFB0BFD5)
val DarkTextTertiary = Color(0xFF90A2BC)
val DarkAccent = Color(0xFF91B7FF)
val DarkAccentPressed = Color(0xFFAAC8FF)
val DarkOnAccent = Color(0xFF0A2044)
val DarkAccentSubtle = Color(0xFF203557)
val DarkError = Color(0xFFFF9CA8)
val DarkSuccess = Color(0xFF79D9B3)
val DarkWarning = Color(0xFFF1C67C)

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    val accentSubtle: Color,
    val error: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean
) {
    val ripple: Color get() = textPrimary.copy(alpha = 0.08f)

    val surfaceVariant: Color get() = surfaceSubtle
    val surfaceElevatedCompat: Color get() = surfaceElevated
    val border: Color get() = outline
    val borderSubtle: Color get() = outline
    val divider: Color get() = outline
    val disabled: Color get() = textTertiary.copy(alpha = 0.38f)
    val accentTint: Color get() = accentSubtle
    val accentDisabled: Color get() = accent.copy(alpha = 0.38f)
    val accentGlow: Color get() = Color.Transparent
    val successTint: Color get() = success.copy(alpha = if (isDark) 0.12f else 0.08f)
    val warningTint: Color get() = warning.copy(alpha = if (isDark) 0.12f else 0.08f)
    val errorTint: Color get() = error.copy(alpha = if (isDark) 0.12f else 0.08f)
    val info: Color get() = textSecondary
    val infoTint: Color get() = surfaceSubtle
    val violet: Color get() = accent
    val violetTint: Color get() = accentSubtle
    val accentContainer: Color get() = accentSubtle
    val successContainer: Color get() = successTint
    val warningContainer: Color get() = warningTint
    val errorContainer: Color get() = errorTint
    val infoContainer: Color get() = surfaceSubtle
}

val LightAppColors = AppColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceSubtle = LightSurfaceSubtle,
    outline = LightOutline,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accent = LightAccent,
    accentPressed = LightAccentPressed,
    onAccent = LightOnAccent,
    accentSubtle = LightAccentSubtle,
    error = LightError,
    success = LightSuccess,
    warning = LightWarning,
    isDark = false
)

val DarkAppColors = AppColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceSubtle = DarkSurfaceSubtle,
    outline = DarkOutline,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accent = DarkAccent,
    accentPressed = DarkAccentPressed,
    onAccent = DarkOnAccent,
    accentSubtle = DarkAccentSubtle,
    error = DarkError,
    success = DarkSuccess,
    warning = DarkWarning,
    isDark = true
)

typealias ExtendedColors = AppColors
val LocalExtendedColors = staticCompositionLocalOf { LightAppColors }
val LocalAppColors = LocalExtendedColors
