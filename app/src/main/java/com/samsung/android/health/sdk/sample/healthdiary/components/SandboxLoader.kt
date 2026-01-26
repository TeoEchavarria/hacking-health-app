package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Loader Component
 * 
 * A reusable loading indicator component following Sandbox design system.
 * Supports full-screen, inline, and button-sized variants.
 * 
 * @param modifier Modifier for styling
 * @param variant Loader size variant
 * @param message Optional loading message
 */
@Composable
fun SandboxLoader(
    modifier: Modifier = Modifier,
    variant: LoaderVariant = LoaderVariant.Medium,
    message: String? = null
) {
    val size = when (variant) {
        LoaderVariant.Small -> 20.dp
        LoaderVariant.Medium -> 40.dp
        LoaderVariant.Large -> 60.dp
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = when (variant) {
                LoaderVariant.Small -> 2.dp
                LoaderVariant.Medium -> 3.dp
                LoaderVariant.Large -> 4.dp
            },
            color = MaterialTheme.colorScheme.primary
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class LoaderVariant {
    Small,
    Medium,
    Large
}

@Preview(showBackground = true)
@Composable
private fun SandboxLoaderPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SandboxLoader(variant = LoaderVariant.Small)
            SandboxLoader(variant = LoaderVariant.Medium)
            SandboxLoader(variant = LoaderVariant.Large)
            SandboxLoader(
                variant = LoaderVariant.Medium,
                message = "Loading..."
            )
        }
    }
}
