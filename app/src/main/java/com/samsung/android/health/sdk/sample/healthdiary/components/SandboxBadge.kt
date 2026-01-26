package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxBadgeShape
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Badge Component
 * 
 * A reusable badge component following Sandbox design system.
 * Used for status indicators, counts, and labels.
 * 
 * @param text The badge text
 * @param modifier Modifier for styling
 * @param variant Badge style variant
 * @param size Badge size
 */
@Composable
fun SandboxBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Primary,
    size: BadgeSize = BadgeSize.Medium
) {
    val backgroundColor = when (variant) {
        BadgeVariant.Primary -> MaterialTheme.colorScheme.primaryContainer
        BadgeVariant.Secondary -> MaterialTheme.colorScheme.secondaryContainer
        BadgeVariant.Error -> MaterialTheme.colorScheme.errorContainer
        BadgeVariant.Success -> MaterialTheme.colorScheme.tertiaryContainer
        BadgeVariant.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (variant) {
        BadgeVariant.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
        BadgeVariant.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
        BadgeVariant.Error -> MaterialTheme.colorScheme.onErrorContainer
        BadgeVariant.Success -> MaterialTheme.colorScheme.onTertiaryContainer
        BadgeVariant.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val textStyle = when (size) {
        BadgeSize.Small -> MaterialTheme.typography.labelSmall
        BadgeSize.Medium -> MaterialTheme.typography.labelMedium
        BadgeSize.Large -> MaterialTheme.typography.labelLarge
    }
    
    val padding = when (size) {
        BadgeSize.Small -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        BadgeSize.Medium -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        BadgeSize.Large -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    }
    
    Surface(
        color = backgroundColor,
        shape = SandboxBadgeShape,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = textStyle,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(padding)
        )
    }
}

enum class BadgeVariant {
    Primary,
    Secondary,
    Error,
    Success,
    Neutral
}

enum class BadgeSize {
    Small,
    Medium,
    Large
}

@Preview(showBackground = true)
@Composable
private fun SandboxBadgePreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SandboxBadge(text = "Primary", variant = BadgeVariant.Primary)
                SandboxBadge(text = "Secondary", variant = BadgeVariant.Secondary)
                SandboxBadge(text = "Error", variant = BadgeVariant.Error)
                SandboxBadge(text = "Success", variant = BadgeVariant.Success)
                SandboxBadge(text = "Neutral", variant = BadgeVariant.Neutral)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SandboxBadge(text = "Small", size = BadgeSize.Small)
                SandboxBadge(text = "Medium", size = BadgeSize.Medium)
                SandboxBadge(text = "Large", size = BadgeSize.Large)
            }
        }
    }
}
