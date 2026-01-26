package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Icon Button Component
 * 
 * A reusable icon button component following Sandbox design system.
 * 
 * @param icon The icon to display
 * @param onClick Action when clicked
 * @param modifier Modifier for styling
 * @param contentDescription Content description for accessibility
 * @param enabled Whether button is enabled
 * @param variant Button style variant
 */
@Composable
fun SandboxIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    variant: IconButtonVariant = IconButtonVariant.Standard
) {
    when (variant) {
        IconButtonVariant.Standard -> {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        IconButtonVariant.Filled -> {
            FilledIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        IconButtonVariant.Outlined -> {
            OutlinedIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

enum class IconButtonVariant {
    Standard,
    Filled,
    Outlined
}

@Preview(showBackground = true)
@Composable
private fun SandboxIconButtonPreview() {
    SandboxTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxIconButton(
                icon = androidx.compose.material.icons.Icons.Default.Settings,
                onClick = {},
                variant = IconButtonVariant.Standard
            )
            SandboxIconButton(
                icon = androidx.compose.material.icons.Icons.Default.Settings,
                onClick = {},
                variant = IconButtonVariant.Filled
            )
            SandboxIconButton(
                icon = androidx.compose.material.icons.Icons.Default.Settings,
                onClick = {},
                variant = IconButtonVariant.Outlined
            )
        }
    }
}
