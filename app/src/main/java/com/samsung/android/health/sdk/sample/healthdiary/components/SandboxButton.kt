package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxButtonShape
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Button Component
 * 
 * A reusable button component following Sandbox design system.
 * Supports primary, secondary, and text variants with loading and disabled states.
 * 
 * @param text The button label text
 * @param onClick Action to perform when clicked
 * @param modifier Modifier for styling
 * @param variant Button style variant (Primary, Secondary, Text)
 * @param enabled Whether the button is enabled
 * @param isLoading Whether the button is in loading state
 * @param icon Optional leading icon
 * @param fullWidth Whether button should fill available width
 */
@Composable
fun SandboxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }
    
    when (variant) {
        ButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier.height(56.dp),
                shape = SandboxButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(text, icon, isLoading)
            }
        }
        ButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier.height(56.dp),
                shape = SandboxButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(text, icon, isLoading)
            }
        }
        ButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier.height(56.dp),
                shape = SandboxButtonShape
            ) {
                ButtonContent(text, icon, isLoading)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class ButtonVariant {
    Primary,
    Secondary,
    Text
}

@Preview(showBackground = true)
@Composable
private fun SandboxButtonPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxButton(
                text = "Primary Button",
                onClick = {},
                fullWidth = true
            )
            SandboxButton(
                text = "Secondary Button",
                onClick = {},
                variant = ButtonVariant.Secondary,
                fullWidth = true
            )
            SandboxButton(
                text = "Text Button",
                onClick = {},
                variant = ButtonVariant.Text,
                fullWidth = true
            )
            SandboxButton(
                text = "Loading",
                onClick = {},
                isLoading = true,
                fullWidth = true
            )
            SandboxButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
                fullWidth = true
            )
        }
    }
}
