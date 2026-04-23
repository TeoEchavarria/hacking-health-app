package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxCard
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.components.CardElevation
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxPrimary
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Daily Challenge Screen
 * 
 * Main screen for cognitive exercises featuring three challenge types:
 * - Numbers: Memorize a 3-digit number
 * - Words: Memorize 3 random words
 * - Drawings: Memorize a pattern/image
 */
@Composable
fun DailyChallengeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNumbers: () -> Unit,
    onNavigateToWords: () -> Unit,
    onNavigateToDrawing: () -> Unit
) {
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Reto Diario",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header text
            Text(
                text = "Elige un reto para ejercitar tu mente",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Numbers Challenge Card
            ChallengeCard(
                icon = Icons.Default.Pin,
                title = "Números",
                description = "Memoriza una secuencia de dígitos",
                gradientColors = listOf(
                    Color(0xFF6366F1),
                    Color(0xFF8B5CF6)
                ),
                onClick = onNavigateToNumbers
            )
            
            // Words Challenge Card
            ChallengeCard(
                icon = Icons.Default.TextFields,
                title = "Palabras",
                description = "Memoriza una lista de palabras",
                gradientColors = listOf(
                    Color(0xFF10B981),
                    Color(0xFF14B8A6)
                ),
                onClick = onNavigateToWords
            )
            
            // Drawing Challenge Card
            ChallengeCard(
                icon = Icons.Default.Brush,
                title = "Dibujos",
                description = "Memoriza patrones e imágenes",
                gradientColors = listOf(
                    Color(0xFFF59E0B),
                    Color(0xFFEF4444)
                ),
                onClick = onNavigateToDrawing
            )
        }
    }
}

@Composable
private fun ChallengeCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(gradientColors)
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Text content
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyChallengeScreenPreview() {
    SandboxTheme {
        DailyChallengeScreen(
            onNavigateBack = {},
            onNavigateToNumbers = {},
            onNavigateToWords = {},
            onNavigateToDrawing = {}
        )
    }
}
