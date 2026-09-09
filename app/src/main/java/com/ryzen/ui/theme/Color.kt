package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================
// COLOR SYSTEM (Token-Based, Light + Dark)
// =====================================================

// Dark Theme Foundations (Claude Warm Charcoal - Never pure black)
val DarkBackground = Color(0xFF1F1E1D)
val DarkSurface = Color(0xFF262524)
val DarkSurfaceElevated = Color(0xFF2E2C29)
val DarkSurfaceVariant = Color(0xFF383632)
val DarkBorder = Color(0xFF3D3B36)
val DarkBorderSubtle = Color(0xFF302E2B)
val DarkDivider = Color(0xFF302E2B)
val DarkTextPrimary = Color(0xFFF5F4EF)
val DarkTextSecondary = Color(0xFFB5B0A6)
val DarkTextTertiary = Color(0xFF858178)
val DarkDisabled = Color(0xFF54514B)

// Light Theme Foundations (Claude Warm Parchment - Never pure white)
val LightBackground = Color(0xFFF5F4EF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFECE8DF)
val LightSurfaceVariant = Color(0xFFE5E1D7)
val LightBorder = Color(0xFFDFD9CD)
val LightBorderSubtle = Color(0xFFEBE7DD)
val LightDivider = Color(0xFFE8E4DA)
val LightTextPrimary = Color(0xFF1F1E1D)
val LightTextSecondary = Color(0xFF6E6A62)
val LightTextTertiary = Color(0xFF969187)
val LightDisabled = Color(0xFFC8C4BA)

// Primary Accent (Claude Signature Warm Terracotta)
val AccentTerracottaLight = Color(0xFFC96442)
val AccentTerracottaLightPressed = Color(0xFFB05333)
val AccentTerracottaDark = Color(0xFFD97757)
val AccentTerracottaDarkPressed = Color(0xFFC96442)

val AccentTerracottaTintLight = Color(0x1AC96442) // ~10% Opacity
val AccentTerracottaTintDark = Color(0x24D97757)  // ~14% Opacity
val AccentTerracottaDisabledLight = Color(0x4DC96442) // 30% Opacity
val AccentTerracottaDisabledDark = Color(0x4DD97757)

// Semantic Status Colors (Muted, WCAG AA compliant)
val SuccessLight = Color(0xFF3B7E54)
val SuccessDark = Color(0xFF4FA870)
val SuccessTintLight = Color(0x1A3B7E54)
val SuccessTintDark = Color(0x224FA870)

val WarningLight = Color(0xFFB57726)
val WarningDark = Color(0xFFD99338)
val WarningTintLight = Color(0x1AB57726)
val WarningTintDark = Color(0x22D99338)

val ErrorLight = Color(0xFFC04747)
val ErrorDark = Color(0xFFD95A5A)
val ErrorTintLight = Color(0x1AC04747)
val ErrorTintDark = Color(0x22D95A5A)

val InfoLight = Color(0xFF4675A8)
val InfoDark = Color(0xFF6392C4)
val InfoTintLight = Color(0x1A4675A8)
val InfoTintDark = Color(0x226392C4)

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderSubtle: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val disabled: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentDisabled: Color,
    val accentTint: Color,
    val success: Color,
    val successTint: Color,
    val warning: Color,
    val warningTint: Color,
    val error: Color,
    val errorTint: Color,
    val info: Color,
    val infoTint: Color,
    val isDark: Boolean
) {
    val accentContainer: Color get() = accentTint
    val successContainer: Color get() = successTint
    val warningContainer: Color get() = warningTint
    val errorContainer: Color get() = errorTint
    val infoContainer: Color get() = infoTint
}

val DarkAppColors = AppColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceVariant = DarkSurfaceVariant,
    border = DarkBorder,
    borderSubtle = DarkBorderSubtle,
    divider = DarkDivider,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    disabled = DarkDisabled,
    accent = AccentTerracottaDark,
    accentPressed = AccentTerracottaDarkPressed,
    accentDisabled = AccentTerracottaDisabledDark,
    accentTint = AccentTerracottaTintDark,
    success = SuccessDark,
    successTint = SuccessTintDark,
    warning = WarningDark,
    warningTint = WarningTintDark,
    error = ErrorDark,
    errorTint = ErrorTintDark,
    info = InfoDark,
    infoTint = InfoTintDark,
    isDark = true
)

val LightAppColors = AppColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceVariant = LightSurfaceVariant,
    border = LightBorder,
    borderSubtle = LightBorderSubtle,
    divider = LightDivider,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    disabled = LightDisabled,
    accent = AccentTerracottaLight,
    accentPressed = AccentTerracottaLightPressed,
    accentDisabled = AccentTerracottaDisabledLight,
    accentTint = AccentTerracottaTintLight,
    success = SuccessLight,
    successTint = SuccessTintLight,
    warning = WarningLight,
    warningTint = WarningTintLight,
    error = ErrorLight,
    errorTint = ErrorTintLight,
    info = InfoLight,
    infoTint = InfoTintLight,
    isDark = false
)

typealias ExtendedColors = AppColors
val LocalExtendedColors = staticCompositionLocalOf { DarkAppColors }
val LocalAppColors = LocalExtendedColors
