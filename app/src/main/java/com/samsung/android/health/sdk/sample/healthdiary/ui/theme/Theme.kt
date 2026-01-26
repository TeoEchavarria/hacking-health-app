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
import androidx.core.view.WindowCompat

/**
 * Sandbox Design System - Theme
 * 
 * Light theme only for now (minimal, health-focused)
 * Can be extended with dark theme later
 */

private val SandboxLightColorScheme = lightColorScheme(
    primary = SandboxPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SandboxPrimaryLight,
    onPrimaryContainer = SandboxPrimaryDark,
    
    secondary = SandboxSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SandboxSecondaryLight,
    onSecondaryContainer = SandboxSecondaryDark,
    
    tertiary = SandboxHealthAccent,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = SandboxHealthAccentLight,
    onTertiaryContainer = SandboxHealthAccentDark,
    
    error = SandboxError,
    onError = Color(0xFFFFFFFF),
    errorContainer = SandboxErrorLight,
    onErrorContainer = SandboxError,
    
    background = SandboxBackground,
    onBackground = SandboxOnBackground,
    
    surface = SandboxSurface,
    onSurface = SandboxOnSurface,
    surfaceVariant = SandboxSurfaceVariant,
    onSurfaceVariant = SandboxOnSurfaceVariant,
    
    outline = SandboxBorder,
    outlineVariant = SandboxDivider
)

@Composable
fun SandboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SandboxTypography,
        shapes = SandboxShapes,
        content = content
    )
}
