package com.ryzen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================
// COLOR SYSTEM — Obsidian + Ember (gaming-polished)
// Dark: deep ink with warm ember accent
// Light: soft parchment with terracotta accent
// =====================================================

// Dark foundations
val DarkBackground = Color(0xFF12111A)
val DarkSurface = Color(0xFF1A1824)
val DarkSurfaceElevated = Color(0xFF232030)
val DarkSurfaceVariant = Color(0xFF2C293A)
val DarkBorder = Color(0xFF3A3648)
val DarkBorderSubtle = Color(0xFF2A2736)
val DarkDivider = Color(0xFF2A2736)
val DarkTextPrimary = Color(0xFFF4F2FA)
val DarkTextSecondary = Color(0xFFB0AABF)
val DarkTextTertiary = Color(0xFF7E788F)
val DarkDisabled = Color(0xFF4E4A5C)

// Light foundations
val LightBackground = Color(0xFFF6F4F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF0ECF4)
val LightSurfaceVariant = Color(0xFFE8E3EE)
val LightBorder = Color(0xFFD9D3E0)
val LightBorderSubtle = Color(0xFFE6E1EB)
val LightDivider = Color(0xFFE4DFE9)
val LightTextPrimary = Color(0xFF17141F)
val LightTextSecondary = Color(0xFF5C5668)
val LightTextTertiary = Color(0xFF8A8496)
val LightDisabled = Color(0xFFC2BCCB)

// Accent — Ember / Coral (more vivid, still warm)
val AccentEmberLight = Color(0xFFE85D3B)
val AccentEmberLightPressed = Color(0xFFCF4A2B)
val AccentEmberDark = Color(0xFFFF7A55)
val AccentEmberDarkPressed = Color(0xFFE85D3B)
val AccentEmberGlow = Color(0x66FF7A55)

val AccentTintLight = Color(0x1FE85D3B)
val AccentTintDark = Color(0x33FF7A55)
val AccentDisabledLight = Color(0x55E85D3B)
val AccentDisabledDark = Color(0x55FF7A55)

// Secondary accent (cool violet for depth / chips)
val VioletLight = Color(0xFF7B5CFF)
val VioletDark = Color(0xFF9B85FF)
val VioletTintLight = Color(0x1A7B5CFF)
val VioletTintDark = Color(0x289B85FF)

// Semantic
val SuccessLight = Color(0xFF2F9E6B)
val SuccessDark = Color(0xFF3FCB88)
val SuccessTintLight = Color(0x1A2F9E6B)
val SuccessTintDark = Color(0x283FCB88)

val WarningLight = Color(0xFFD4891A)
val WarningDark = Color(0xFFFFB340)
val WarningTintLight = Color(0x1AD4891A)
val WarningTintDark = Color(0x28FFB340)

val ErrorLight = Color(0xFFD64545)
val ErrorDark = Color(0xFFFF6B6B)
val ErrorTintLight = Color(0x1AD64545)
val ErrorTintDark = Color(0x28FF6B6B)

val InfoLight = Color(0xFF4A7FD4)
val InfoDark = Color(0xFF6BA0F0)
val InfoTintLight = Color(0x1A4A7FD4)
val InfoTintDark = Color(0x286BA0F0)

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
    val accentGlow: Color,
    val violet: Color,
    val violetTint: Color,
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

    /** Soft vertical wash used behind screens. */
    fun ambientBrush(): Brush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1528),
                background,
                Color(0xFF0E0D14)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFBF8FF),
                background,
                Color(0xFFEEEAF4)
            )
        )
    }

    /** Primary CTA gradient (top-left ember → deeper ember). */
    fun accentGradient(): Brush = Brush.horizontalGradient(
        colors = listOf(accent, accentPressed)
    )

    /** Subtle radial glow for hero icons / focus. */
    fun heroGlowBrush(): Brush = Brush.radialGradient(
        colors = listOf(accentGlow, Color.Transparent)
    )
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
    accent = AccentEmberDark,
    accentPressed = AccentEmberDarkPressed,
    accentDisabled = AccentDisabledDark,
    accentTint = AccentTintDark,
    accentGlow = AccentEmberGlow,
    violet = VioletDark,
    violetTint = VioletTintDark,
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
    accent = AccentEmberLight,
    accentPressed = AccentEmberLightPressed,
    accentDisabled = AccentDisabledLight,
    accentTint = AccentTintLight,
    accentGlow = Color(0x44E85D3B),
    violet = VioletLight,
    violetTint = VioletTintLight,
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

// Back-compat aliases used by older call sites
val AccentTerracottaLight = AccentEmberLight
val AccentTerracottaLightPressed = AccentEmberLightPressed
val AccentTerracottaDark = AccentEmberDark
val AccentTerracottaDarkPressed = AccentEmberDarkPressed
val AccentTerracottaTintLight = AccentTintLight
val AccentTerracottaTintDark = AccentTintDark
val AccentTerracottaDisabledLight = AccentDisabledLight
val AccentTerracottaDisabledDark = AccentDisabledDark

typealias ExtendedColors = AppColors
val LocalExtendedColors = staticCompositionLocalOf { DarkAppColors }
val LocalAppColors = LocalExtendedColors
