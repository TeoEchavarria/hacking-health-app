package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Toggle Component
 * 
 * A reusable toggle/switch component following Sandbox design system.
 * 
 * @param checked Whether toggle is checked
 * @param onCheckedChange Callback when toggle state changes
 * @param modifier Modifier for styling
 * @param enabled Whether toggle is enabled
 * @param label Optional label text
 */
@Composable
fun SandboxToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    if (label != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SandboxTogglePreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxToggle(
                checked = true,
                onCheckedChange = {},
                label = "Toggle with Label"
            )
            SandboxToggle(
                checked = false,
                onCheckedChange = {},
                label = "Unchecked Toggle"
            )
            SandboxToggle(
                checked = true,
                onCheckedChange = {},
                label = "Disabled Toggle",
                enabled = false
            )
            SandboxToggle(
                checked = false,
                onCheckedChange = {}
            )
        }
    }
}
