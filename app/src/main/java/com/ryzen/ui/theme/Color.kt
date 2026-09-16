package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================
// COLOR TOKENS — ink + stone, single inverse accent
// No gradients. Every UI color must come from AppColors.
// =====================================================

// Light
val LightBackground = Color(0xFFF7F7F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFEFEFED)
val LightOutline = Color(0xFFE3E2DD)
val LightTextPrimary = Color(0xFF111110)
val LightTextSecondary = Color(0xFF5C5B57)
val LightTextTertiary = Color(0xFF8B8A84)
val LightAccent = Color(0xFF111110)
val LightAccentPressed = Color(0xFF2A2A28)
val LightOnAccent = Color(0xFFF7F7F5)
val LightAccentSubtle = Color(0xFFEFEFED)
val LightError = Color(0xFFC23B32)
val LightSuccess = Color(0xFF3D6B4F)
val LightWarning = Color(0xFF9A6B1B)

// Dark
val DarkBackground = Color(0xFF0E0E0D)
val DarkSurface = Color(0xFF161615)
val DarkSurfaceElevated = Color(0xFF1C1C1A)
val DarkSurfaceSubtle = Color(0xFF232321)
val DarkOutline = Color(0xFF2C2C29)
val DarkTextPrimary = Color(0xFFF4F3EF)
val DarkTextSecondary = Color(0xFFA8A7A1)
val DarkTextTertiary = Color(0xFF787770)
val DarkAccent = Color(0xFFF4F3EF)
val DarkAccentPressed = Color(0xFFE4E3DF)
val DarkOnAccent = Color(0xFF111110)
val DarkAccentSubtle = Color(0xFF232321)
val DarkError = Color(0xFFE07A72)
val DarkSuccess = Color(0xFF8BB59A)
val DarkWarning = Color(0xFFD2A24C)

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
    val successTint: Color get() = surfaceSubtle
    val warningTint: Color get() = surfaceSubtle
    val errorTint: Color get() = surfaceSubtle
    val info: Color get() = textSecondary
    val infoTint: Color get() = surfaceSubtle
    val violet: Color get() = accent
    val violetTint: Color get() = accentSubtle
    val accentContainer: Color get() = accentSubtle
    val successContainer: Color get() = surfaceSubtle
    val warningContainer: Color get() = surfaceSubtle
    val errorContainer: Color get() = surfaceSubtle
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
