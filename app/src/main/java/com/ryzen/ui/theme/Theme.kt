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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =====================================================
// THEME — Material 3 mapped from warm tokens
// Dynamic color disabled. Flat surfaces. Edge-to-edge.
// =====================================================

private val DarkMaterialColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentSubtle,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceSubtle,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkTextTertiary,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSubtle,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
    onError = DarkOnAccent,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = LightAccent,
    scrim = Color.Black.copy(alpha = 0.4f)
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentSubtle,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightTextSecondary,
    onSecondary = LightBackground,
    secondaryContainer = LightSurfaceSubtle,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightTextTertiary,
    onTertiary = LightBackground,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSubtle,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError,
    onError = LightOnAccent,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = DarkAccent,
    scrim = Color.Black.copy(alpha = 0.32f)
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

/** Flat canvas root — solid background token only. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
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
