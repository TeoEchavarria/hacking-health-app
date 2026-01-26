package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Section Component
 * 
 * A reusable section container component following Sandbox design system.
 * Used to group related content with optional title.
 * 
 * @param title Optional section title
 * @param content The section content
 * @param modifier Modifier for styling
 */
@Composable
fun SandboxSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun SandboxSectionPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SandboxSection(
                title = "Section Title",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Section content goes here")
            }
            SandboxSection(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Section without title")
            }
        }
    }
}
