package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================
// COLOR TOKENS — warm neutral + single terracotta accent
// Every UI color must come from AppColors / these vals.
// =====================================================

// Light
val LightBackground = Color(0xFFF5F4EF)
val LightSurface = Color(0xFFFAF9F5)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFECEAE3)
val LightOutline = Color(0xFFDDD9CF)
val LightTextPrimary = Color(0xFF1F1E1D)
val LightTextSecondary = Color(0xFF6B6860)
val LightTextTertiary = Color(0xFF9C988E)
val LightAccent = Color(0xFFC96442)
val LightAccentPressed = Color(0xFFB5573A)
val LightOnAccent = Color(0xFFFFFFFF)
val LightAccentSubtle = Color(0xFFF3E4DC)
val LightError = Color(0xFFB3261E)
val LightSuccess = Color(0xFF4F7A5B)
val LightWarning = Color(0xFF9A6B1B)

// Dark
val DarkBackground = Color(0xFF1F1E1D)
val DarkSurface = Color(0xFF262523)
val DarkSurfaceElevated = Color(0xFF2B2A27)
val DarkSurfaceSubtle = Color(0xFF33312E)
val DarkOutline = Color(0xFF3D3B37)
val DarkTextPrimary = Color(0xFFF5F4EF)
val DarkTextSecondary = Color(0xFFA8A49B)
val DarkTextTertiary = Color(0xFF77736B)
val DarkAccent = Color(0xFFD97757)
val DarkAccentPressed = Color(0xFFE08A6E)
val DarkOnAccent = Color(0xFF1F1E1D)
val DarkAccentSubtle = Color(0xFF3A2C26)
val DarkError = Color(0xFFE5776A)
val DarkSuccess = Color(0xFF86B096)
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
    /** Ripple: textPrimary @ 8% */
    val ripple: Color get() = textPrimary.copy(alpha = 0.08f)

    // Back-compat aliases used during migration (map old names → new tokens)
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
