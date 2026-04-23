package com.samsung.android.health.sdk.sample.healthdiary.views.challenges

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxButton
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.components.ButtonVariant
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import com.samsung.android.health.sdk.sample.healthdiary.utils.TextToSpeechHelper
import kotlin.random.Random

/**
 * Words Challenge Screen
 * 
 * Displays 3 random words that the user must memorize.
 * Features:
 * - Random word selection from predefined list
 * - Direction indicator (forward/reverse)
 * - TTS reads each word, repeats twice
 * - Hide/Reveal/Next controls
 */
@Composable
fun WordsChallengeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Load words from assets
    val allWords = remember { loadChallengeWords(context) }
    
    // State
    var currentWords by remember { mutableStateOf(selectRandomWords(allWords, 3)) }
    var isReverse by remember { mutableStateOf(Random.nextBoolean()) }
    var isHidden by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    
    // TTS Helper
    val ttsHelper = remember {
        TextToSpeechHelper(
            context = context,
            onInitialized = { ttsReady = true },
            onError = { /* Handle error */ }
        )
    }
    
    // Cleanup TTS on dispose
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.release()
        }
    }
    
    // Generate new challenge
    fun generateNewChallenge() {
        ttsHelper.stop()
        isSpeaking = false
        currentWords = selectRandomWords(allWords, 3)
        isReverse = Random.nextBoolean()
        isHidden = false
    }
    
    // Speak the words
    fun speakWords() {
        if (!ttsReady || isSpeaking) return
        
        isSpeaking = true
        val wordsToSpeak = if (isReverse) {
            currentWords.reversed()
        } else {
            currentWords
        }
        
        ttsHelper.speakWords(
            words = wordsToSpeak,
            delayBetweenWords = 1000L,
            repetitions = 2,
            delayBetweenRepetitions = 2000L,
            onComplete = { isSpeaking = false }
        )
    }
    
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Reto de Palabras",
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
            // Instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Memoriza las palabras",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Direction indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isReverse) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isReverse) "Al revés" else "Al derecho",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Words display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF14B8A6)
                                    )
                                )
                            )
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = isHidden,
                            label = "words_visibility"
                        ) { hidden ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (!hidden) {
                                    currentWords.forEachIndexed { index, word ->
                                        Text(
                                            text = "${index + 1}. $word",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    repeat(3) { index ->
                                        Text(
                                            text = "${index + 1}. ???",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Controls - Large buttons for better visibility
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play audio button - Extra large
                Button(
                    onClick = { speakWords() },
                    enabled = ttsReady && !isSpeaking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    if (isSpeaking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Reproduciendo...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escuchar",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Hide/Reveal and Next buttons row - Large
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hide/Reveal button
                    Button(
                        onClick = { isHidden = !isHidden },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHidden) Color(0xFF3B82F6) else Color(0xFF6B7280),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHidden) "Revelar" else "Ocultar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Next button
                    Button(
                        onClick = { generateNewChallenge() },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Siguiente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loads challenge words from assets/challenge_words.yaml
 * Falls back to hardcoded list if loading fails
 */
private fun loadChallengeWords(context: Context): List<String> {
    return try {
        context.assets.open("challenge_words.yaml").bufferedReader().use { reader ->
            val lines = reader.readLines()
            lines.filter { line ->
                val trimmed = line.trim()
                trimmed.startsWith("- ") && !trimmed.contains(":")
            }.map { line ->
                line.trim().removePrefix("- ").trim()
            }
        }
    } catch (e: Exception) {
        // Fallback to hardcoded words
        listOf(
            "gato", "perro", "casa", "árbol", "sol",
            "luna", "agua", "pan", "mesa", "silla",
            "libro", "mano", "ojo", "flor", "rojo",
            "azul", "verde", "carro", "avión", "tren"
        )
    }
}

private fun selectRandomWords(words: List<String>, count: Int): List<String> {
    return words.shuffled().take(count)
}

@Preview(showBackground = true)
@Composable
private fun WordsChallengeScreenPreview() {
    SandboxTheme {
        WordsChallengeScreen(onNavigateBack = {})
    }
}
