package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Device Alias Dialog Component
 * 
 * Shown when a new watch device connects successfully.
 * Allows user to assign an optional friendly name (alias) to identify the device.
 * 
 * @param deviceName The name of the connected device
 * @param onSave Callback when user saves the alias (receives alias string)
 * @param onSkip Callback when user skips alias assignment
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun DeviceAliasDialog(
    deviceName: String,
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var aliasText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Device Connected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device name display
                Text(
                    text = "Connected: $deviceName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Explanation text
                Text(
                    text = "You can assign an optional alias to identify who this device belongs to.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Alias input field
                SandboxInput(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = "Alias (optional)",
                    placeholder = "e.g., Dad, Mom, Patient 1, John",
                    singleLine = true
                )

                // Examples hint
                Text(
                    text = "Examples: Dad, Mom, Patient 1, John",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(aliasText.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    )
}
