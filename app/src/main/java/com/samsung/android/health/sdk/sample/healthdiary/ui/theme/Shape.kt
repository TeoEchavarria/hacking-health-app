package com.samsung.android.health.sdk.sample.healthdiary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sandbox Design System - Shapes
 * 
 * Soft rounded corners throughout
 * Consistent corner radius values
 */

val SandboxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Common shape presets
val SandboxCardShape = RoundedCornerShape(16.dp)
val SandboxButtonShape = RoundedCornerShape(12.dp)
val SandboxInputShape = RoundedCornerShape(12.dp)
val SandboxBadgeShape = RoundedCornerShape(8.dp)
val SandboxChipShape = RoundedCornerShape(20.dp)
