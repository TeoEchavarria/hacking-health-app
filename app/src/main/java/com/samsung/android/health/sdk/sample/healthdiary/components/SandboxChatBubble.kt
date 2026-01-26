package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Chat Bubble Component
 * 
 * A reusable chat bubble component following Sandbox design system.
 * Used for chat interfaces and message displays.
 * 
 * @param text The message text
 * @param isUser Whether this is a user message (affects styling)
 * @param modifier Modifier for styling
 * @param timestamp Optional timestamp text
 */
@Composable
fun SandboxChatBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    timestamp: String? = null
) {
    Column(
        modifier = modifier.widthIn(max = 300.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
        if (timestamp != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SandboxChatBubblePreview() {
    SandboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SandboxChatBubble(
                text = "This is a user message",
                isUser = true,
                timestamp = "10:30 AM"
            )
            SandboxChatBubble(
                text = "This is an assistant message with longer text that wraps to multiple lines",
                isUser = false,
                timestamp = "10:31 AM"
            )
        }
    }
}
