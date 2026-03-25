package com.samsung.android.health.sdk.sample.healthdiary.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat

/**
 * Sandbox Design System - Theme
 * 
 * Tu Salud - Light theme with blue primary and red emergency accents
 * Based on Material Design 3 semantic tokens
 */

private val SandboxLightColorScheme = lightColorScheme(
    primary = SandboxPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SandboxPrimaryContainer,
    onPrimaryContainer = SandboxOnPrimaryContainer,
    
    secondary = SandboxSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SandboxSecondaryContainer,
    onSecondaryContainer = SandboxOnSecondaryContainer,
    
    tertiary = SandboxTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = SandboxTertiaryContainer,
    onTertiaryContainer = SandboxOnTertiaryContainer,
    
    error = SandboxError,
    onError = Color(0xFFFFFFFF),
    errorContainer = SandboxErrorContainer,
    onErrorContainer = SandboxOnErrorContainer,
    
    background = SandboxBackground,
    onBackground = SandboxOnBackground,
    
    surface = SandboxSurface,
    onSurface = SandboxOnSurface,
    surfaceVariant = SandboxSurfaceVariant,
    onSurfaceVariant = SandboxOnSurfaceVariant,
    
    outline = SandboxOutline,
    outlineVariant = SandboxOutlineVariant,
    
    inverseSurface = SandboxInverseSurface,
    inverseOnSurface = SandboxInverseOnSurface,
    inversePrimary = SandboxInversePrimary,
    
    surfaceDim = SandboxSurfaceDim,
    surfaceBright = SandboxSurfaceBright,
    surfaceContainerLowest = SandboxSurfaceContainerLowest,
    surfaceContainerLow = SandboxSurfaceContainerLow,
    surfaceContainer = SandboxSurfaceContainer,
    surfaceContainerHigh = SandboxSurfaceContainerHigh,
    surfaceContainerHighest = SandboxSurfaceContainerHighest
)

@Composable
fun SandboxTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            // Dark theme can be added later
            SandboxLightColorScheme
        }
        else -> SandboxLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = colorScheme.background
            window.statusBarColor = statusBarColor.toArgb()

            val useDarkIcons = statusBarColor.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SandboxTypography,
        shapes = SandboxShapes,
        content = content
    )
}
