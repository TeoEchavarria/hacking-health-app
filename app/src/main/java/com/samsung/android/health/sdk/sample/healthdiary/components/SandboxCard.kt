package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxCardShape
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Card Component
 * 
 * A reusable card component following Sandbox design system.
 * Provides consistent padding, elevation, and styling.
 * 
 * @param content The content to display inside the card
 * @param modifier Modifier for styling
 * @param onClick Optional click handler (makes card clickable)
 * @param elevation Card elevation (0-3)
 */
@Composable
fun SandboxCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: CardElevation = CardElevation.Low,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardElevation = when (elevation) {
        CardElevation.None -> CardDefaults.cardElevation(defaultElevation = 0.dp)
        CardElevation.Low -> CardDefaults.cardElevation(defaultElevation = 1.dp)
        CardElevation.Medium -> CardDefaults.cardElevation(defaultElevation = 2.dp)
        CardElevation.High -> CardDefaults.cardElevation(defaultElevation = 4.dp)
    }
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = SandboxCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = cardElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = SandboxCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = cardElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

enum class CardElevation {
    None,
    Low,
    Medium,
    High
}

@Preview(showBackground = true)
@Composable
private fun SandboxCardPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Card with Low Elevation")
            }
            SandboxCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardElevation.Medium
            ) {
                Text("Card with Medium Elevation")
            }
            SandboxCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                elevation = CardElevation.High
            ) {
                Text("Clickable Card")
            }
        }
    }
}
