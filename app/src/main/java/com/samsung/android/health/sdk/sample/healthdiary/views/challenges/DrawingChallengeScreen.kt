package com.samsung.android.health.sdk.sample.healthdiary.views.challenges

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Drawing Challenge Screen
 * 
 * Displays a random Quick Draw image that the user must memorize.
 * Features:
 * - Fetches random drawings from Quick Draw API
 * - Falls back to local drawings if API fails
 * - 15-second countdown timer
 * - Auto-hide when timer ends
 * - Hide/Reveal/Next controls
 */

// Fallback challenge drawings (used when API is unavailable)
private val fallbackDrawings = listOf(
    R.drawable.challenge_drawing_house,
    R.drawable.challenge_drawing_tree,
    R.drawable.challenge_drawing_pattern,
    R.drawable.challenge_drawing_arrows,
    R.drawable.challenge_drawing_night
)

private val fallbackDrawingNames = mapOf(
    R.drawable.challenge_drawing_house to "Casa con sol",
    R.drawable.challenge_drawing_tree to "Árbol con manzanas",
    R.drawable.challenge_drawing_pattern to "Patrón geométrico",
    R.drawable.challenge_drawing_arrows to "Secuencia de flechas",
    R.drawable.challenge_drawing_night to "Cielo nocturno"
)

/**
 * Result of fetching a drawing from the API
 */
private sealed class DrawingState {
    object Loading : DrawingState()
    data class FromApi(val bitmap: Bitmap, val categoryName: String) : DrawingState()
    data class FromFallback(val drawableRes: Int, val name: String) : DrawingState()
    data class Error(val message: String) : DrawingState()
}

/**
 * Select a random fallback drawing when API is unavailable
 */
private fun useFallbackDrawing(): DrawingState {
    val index = Random.nextInt(fallbackDrawings.size)
    val drawableRes = fallbackDrawings[index]
    val name = fallbackDrawingNames[drawableRes] ?: "Dibujo"
    return DrawingState.FromFallback(drawableRes, name)
}

@Composable
fun DrawingChallengeScreen(
    onNavigateBack: () -> Unit
) {
    // Coroutine scope for API calls
    val scope = rememberCoroutineScope()
    
    // Drawing state
    var drawingState by remember { mutableStateOf<DrawingState>(DrawingState.Loading) }
    
    // Timer state
    var isHidden by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var hasTimerStarted by remember { mutableStateOf(false) }
    
    // Get current drawing name for display
    val currentDrawingName = when (val state = drawingState) {
        is DrawingState.Loading -> "Cargando..."
        is DrawingState.FromApi -> state.categoryName.replaceFirstChar { it.uppercase() }
        is DrawingState.FromFallback -> state.name
        is DrawingState.Error -> "Error"
    }
    
    // Function to fetch drawing from API
    suspend fun fetchDrawingFromApi(): DrawingState {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.drawingApiService.getRandomDrawing()
                if (response.isSuccessful && response.body() != null) {
                    val bytes = response.body()!!.bytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val category = response.headers()["X-Drawing-Category"] ?: "dibujo"
                    Log.d("DrawingChallenge", "Loaded drawing from API: $category")
                    DrawingState.FromApi(bitmap, category)
                } else {
                    Log.w("DrawingChallenge", "API error: ${response.code()}, falling back to local")
                    useFallbackDrawing()
                }
            } catch (e: Exception) {
                Log.e("DrawingChallenge", "Failed to fetch from API: ${e.message}", e)
                useFallbackDrawing()
            }
        }
    }
    
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
    
    // Initial load
    LaunchedEffect(Unit) {
        drawingState = fetchDrawingFromApi()
    }
    
    // Generate new challenge
    fun generateNewChallenge() {
        isHidden = false
        timeRemaining = 15
        isTimerRunning = false
        hasTimerStarted = false
        drawingState = DrawingState.Loading
        scope.launch {
            drawingState = fetchDrawingFromApi()
        }
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
                                        when (val state = drawingState) {
                                            is DrawingState.Loading -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(48.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            is DrawingState.FromApi -> {
                                                Image(
                                                    bitmap = state.bitmap.asImageBitmap(),
                                                    contentDescription = state.categoryName,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp)
                                                )
                                            }
                                            is DrawingState.FromFallback -> {
                                                Image(
                                                    painter = painterResource(id = state.drawableRes),
                                                    contentDescription = state.name,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp)
                                                )
                                            }
                                            is DrawingState.Error -> {
                                                Text(
                                                    text = "Error loading drawing",
                                                    color = Color.Red
                                                )
                                            }
                                        }
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
            
            // Controls - Large buttons for better visibility
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start timer button (only shown if timer hasn't started) - Extra large
                if (!hasTimerStarted && !isHidden) {
                    Button(
                        onClick = { startTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Iniciar (15 seg)",
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
                        onClick = { 
                            isHidden = !isHidden
                            if (!isHidden && hasTimerStarted) {
                                // Stop timer when revealing
                                isTimerRunning = false
                            }
                        },
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

@Preview(showBackground = true)
@Composable
private fun DrawingChallengeScreenPreview() {
    SandboxTheme {
        DrawingChallengeScreen(onNavigateBack = {})
    }
}
