package com.samsung.android.health.sdk.sample.healthdiary.views.challenges

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxButton
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.components.ButtonVariant
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Drawing Challenge Screen
 * 
 * Displays a random image/pattern that the user must memorize.
 * Features:
 * - Random drawing selection
 * - 15-second countdown timer
 * - Auto-hide when timer ends
 * - Hide/Reveal/Next controls
 */

// Available challenge drawings
private val challengeDrawings = listOf(
    R.drawable.challenge_drawing_house,
    R.drawable.challenge_drawing_tree,
    R.drawable.challenge_drawing_pattern,
    R.drawable.challenge_drawing_arrows,
    R.drawable.challenge_drawing_night
)

private val drawingNames = mapOf(
    R.drawable.challenge_drawing_house to "Casa con sol",
    R.drawable.challenge_drawing_tree to "Árbol con manzanas",
    R.drawable.challenge_drawing_pattern to "Patrón geométrico",
    R.drawable.challenge_drawing_arrows to "Secuencia de flechas",
    R.drawable.challenge_drawing_night to "Cielo nocturno"
)

@Composable
fun DrawingChallengeScreen(
    onNavigateBack: () -> Unit
) {
    // State
    var currentDrawingIndex by remember { mutableStateOf(Random.nextInt(challengeDrawings.size)) }
    var isHidden by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var hasTimerStarted by remember { mutableStateOf(false) }
    
    val currentDrawing = challengeDrawings[currentDrawingIndex]
    val currentDrawingName = drawingNames[currentDrawing] ?: "Dibujo"
    
    // Timer effect
    LaunchedEffect(isTimerRunning, timeRemaining) {
        if (isTimerRunning && timeRemaining > 0) {
            delay(1000L)
            timeRemaining--
        } else if (isTimerRunning && timeRemaining == 0) {
            isTimerRunning = false
            isHidden = true
        }
    }
    
    // Generate new challenge
    fun generateNewChallenge() {
        currentDrawingIndex = Random.nextInt(challengeDrawings.size)
        isHidden = false
        timeRemaining = 15
        isTimerRunning = false
        hasTimerStarted = false
    }
    
    // Start timer
    fun startTimer() {
        if (!hasTimerStarted) {
            hasTimerStarted = true
            isTimerRunning = true
        }
    }
    
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Reto de Dibujos",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Instructions and timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Memoriza el dibujo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Drawing name
                Text(
                    text = currentDrawingName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                // Timer
                if (hasTimerStarted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                timeRemaining > 10 -> Color(0xFF10B981)
                                timeRemaining > 5 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        ),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$timeRemaining",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Drawing display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFFF59E0B),
                                        Color(0xFFEF4444)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = isHidden,
                            label = "drawing_visibility"
                        ) { hidden ->
                            if (!hidden) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = currentDrawing),
                                            contentDescription = currentDrawingName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🎨",
                                        fontSize = 64.sp
                                    )
                                    Text(
                                        text = "¿Recuerdas el dibujo?",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Controls
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start timer button (only shown if timer hasn't started)
                if (!hasTimerStarted && !isHidden) {
                    SandboxButton(
                        text = "Iniciar (15 seg)",
                        onClick = { startTimer() },
                        fullWidth = true
                    )
                }
                
                // Hide/Reveal row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SandboxButton(
                        text = if (isHidden) "Revelar" else "Ocultar",
                        onClick = { 
                            isHidden = !isHidden
                            if (!isHidden && hasTimerStarted) {
                                // Stop timer when revealing
                                isTimerRunning = false
                            }
                        },
                        icon = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SandboxButton(
                        text = "Siguiente",
                        onClick = { generateNewChallenge() },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawingChallengeScreenPreview() {
    SandboxTheme {
        DrawingChallengeScreen(onNavigateBack = {})
    }
}
