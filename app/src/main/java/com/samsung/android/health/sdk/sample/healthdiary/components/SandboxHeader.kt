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
 * Sandbox Header Component
 * 
 * A reusable header/title component following Sandbox design system.
 * Used for screen titles and section headers.
 * 
 * @param title The header text
 * @param modifier Modifier for styling
 * @param subtitle Optional subtitle text
 * @param variant Header size variant
 */
@Composable
fun SandboxHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    variant: HeaderVariant = HeaderVariant.Large
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = when (variant) {
                HeaderVariant.Large -> MaterialTheme.typography.headlineLarge
                HeaderVariant.Medium -> MaterialTheme.typography.headlineMedium
                HeaderVariant.Small -> MaterialTheme.typography.headlineSmall
            },
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class HeaderVariant {
    Large,
    Medium,
    Small
}

@Preview(showBackground = true)
@Composable
private fun SandboxHeaderPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SandboxHeader(
                title = "Large Header",
                variant = HeaderVariant.Large
            )
            SandboxHeader(
                title = "Medium Header",
                variant = HeaderVariant.Medium
            )
            SandboxHeader(
                title = "Small Header",
                variant = HeaderVariant.Small
            )
            SandboxHeader(
                title = "Header with Subtitle",
                subtitle = "This is a subtitle that provides additional context"
            )
        }
    }
}
