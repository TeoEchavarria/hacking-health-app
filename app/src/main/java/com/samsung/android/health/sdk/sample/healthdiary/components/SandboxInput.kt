package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxInputShape
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Input Component
 * 
 * A reusable text input component following Sandbox design system.
 * Supports various input types and states.
 * 
 * @param value The current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier for styling
 * @param label Optional label text
 * @param placeholder Placeholder text
 * @param enabled Whether input is enabled
 * @param isError Whether input is in error state
 * @param errorMessage Error message to display
 * @param keyboardType Keyboard type (Text, Password, Number, etc.)
 * @param singleLine Whether input is single line
 * @param maxLines Maximum lines (for multi-line)
 */
@Composable
fun SandboxInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 3
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = label?.let { { Text(it) } },
            placeholder = { Text(placeholder) },
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            shape = SandboxInputShape,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (keyboardType == KeyboardType.Password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SandboxInputPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxInput(
                value = "",
                onValueChange = {},
                placeholder = "Enter text",
                label = "Label"
            )
            SandboxInput(
                value = "Some text",
                onValueChange = {},
                placeholder = "Enter text",
                label = "Filled"
            )
            SandboxInput(
                value = "",
                onValueChange = {},
                placeholder = "Password",
                label = "Password",
                keyboardType = KeyboardType.Password
            )
            SandboxInput(
                value = "Error",
                onValueChange = {},
                placeholder = "Enter text",
                label = "Error",
                isError = true,
                errorMessage = "This field is required"
            )
        }
    }
}
