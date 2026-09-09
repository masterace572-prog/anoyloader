package com.ryzen.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================
// THEME — Material 3 + extended tokens + ambient canvas
// =====================================================

private val DarkMaterialColorScheme = darkColorScheme(
    primary = AccentEmberDark,
    onPrimary = Color(0xFF1A0F0C),
    primaryContainer = AccentTintDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = VioletDark,
    onSecondary = Color(0xFF1A1528),
    secondaryContainer = VioletTintDark,
    onSecondaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = ErrorDark,
    onError = Color(0xFF1A0F0C)
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = AccentEmberLight,
    onPrimary = Color.White,
    primaryContainer = AccentTintLight,
    onPrimaryContainer = LightTextPrimary,
    secondary = VioletLight,
    onSecondary = Color.White,
    secondaryContainer = VioletTintLight,
    onSecondaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    error = ErrorLight,
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val materialColorScheme = if (darkTheme) DarkMaterialColorScheme else LightMaterialColorScheme
    val extendedColors = if (darkTheme) DarkAppColors else LightAppColors
    val spacing = Spacing()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}

/**
 * Full-screen ambient gradient canvas — use as the root of each screen
 * for smoother depth than a flat background color.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.ambientBrush())
    ) {
        content()
    }
}

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: androidx.compose.material3.Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val motion: AppMotion
        get() = AppMotion
}
